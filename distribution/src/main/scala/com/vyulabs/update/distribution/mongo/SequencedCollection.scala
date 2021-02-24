package com.vyulabs.update.distribution.mongo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{BroadcastHub, Concat, Keep, Source}
import com.mongodb.client.model._
import com.vyulabs.update.distribution.common.AkkaSource
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{BsonDateTime, BsonDocument, BsonDocumentReader, BsonDocumentWrapper}
import org.mongodb.scala.bson.BsonInt64
import org.slf4j.Logger

import java.util.concurrent.TimeUnit
import scala.collection.immutable.Queue
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

case class Sequenced[T](sequence: Long, document: T)

class SequencedCollection[T: ClassTag](val name: String,
                                       collection: Future[MongoDbCollection[BsonDocument]], sequenceCollection: Future[MongoDbCollection[SequenceDocument]],
                                       historyExpireDays: Int = 7)(implicit system: ActorSystem, executionContext: ExecutionContext, codecRegistry: CodecRegistry) {
  private implicit val log = Logging(system, this.getClass)

  private val (publisherCallback, publisherSource) = Source.fromGraph(new AkkaSource[Sequenced[T]]()).toMat(BroadcastHub.sink)(Keep.both).run()
  private var publisherBuffer = Queue.empty[Sequenced[T]]

  private var modifyInProcess = Option.empty[Future[Int]]
  private var nextSequenceInProcess = Option.empty[Future[Long]]

  for {
    collection <- collection
    _ <- collection.createIndex(Indexes.ascending("_expireTime"),
      new IndexOptions().expireAfter(historyExpireDays, TimeUnit.DAYS))
  } yield {}

  def insert(document: T): Future[Long] = {
    insert(Seq(document))
  }

  def insert(documents: Seq[T]): Future[Long] = {
    for {
      collection <- collection
      sequence <- getNextSequence(collection.getName(), documents.size)
      result <- {
        var seq = sequence - documents.size + 1
        val docs = documents.map { document =>
          val doc = BsonDocumentWrapper.asBsonDocument(document, codecRegistry)
          doc.append("_id", new BsonInt64(seq)); seq += 1
          doc.append("_time", new BsonDateTime(System.currentTimeMillis()))
          doc
        }
        collection.insert(docs).map(_ => sequence)
      }
    } yield {
      var seq = sequence - documents.size + 1
      synchronized {
        documents.foreach { doc =>
          val log = Sequenced(seq, doc)
          if (!publisherBuffer.isEmpty) {
            if (sequence <= publisherBuffer.last.sequence) {
              publisherBuffer = Queue.empty
            }
          }
          publisherBuffer = publisherBuffer.enqueue(log)
          if (publisherBuffer.size > 100) {
            publisherBuffer = publisherBuffer.takeRight(100)
          }
          publisherCallback.invoke(log)
          seq += 1
        }
      }
      result
    }
  }

  def find(filters: Bson = new BsonDocument(), sort: Option[Bson] = None, limit: Option[Int] = None): Future[Seq[T]] = {
    for {
      docs <- findDocuments(filters, sort, limit)
    } yield {
      docs.map(doc => {
        val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
        codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build())
      })
    }
  }

  def findSequenced(filters: Bson = new BsonDocument(), sort: Option[Bson] = None, limit: Option[Int] = None): Future[Seq[Sequenced[T]]] = {
    for {
      docs <- findDocuments(filters, sort, limit)
    } yield {
      docs.map(doc => {
        val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
        Sequenced[T](doc.getInt64("_id").getValue, codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build()))
      })
    }
  }

  def update(filters: Bson, modify: Option[T] => Option[T]): Future[Int] = {
    def update(): Future[Int] = {
      for {
        docs <- findDocuments(filters)
        result <- {
          if (!docs.isEmpty) {
            Future.sequence(docs.map { doc => updateAndInsert(Some(doc)) }).map(_.foldLeft(0)((sum, elem) => sum + elem))
          } else {
            updateAndInsert(None).map(_ => 1)
          }
        }
      } yield result
    }

    def updateAndInsert(doc: Option[BsonDocument]): Future[Int] = {
      for {
        collection <- collection
        sequence <- getNextSequence(collection.getName(), 1)
        result <- {
          doc match {
            case Some(doc) =>
              val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
              val docId = doc.getInt64("_id")
              val document = codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build())
              modify(Some(document)) match {
                case Some(document) =>
                  for {
                    _ <- collection.updateOne(Filters.eq("_id", docId),
                      Updates.combine(Updates.set("_replacedBy", new BsonInt64(sequence)), Updates.set("_expireTime", new BsonDateTime(System.currentTimeMillis()))))
                    result <- insert(collection, document, sequence).map(_ => 1)
                    _ <- collection.updateOne(Filters.eq("_id", docId),
                      Updates.unset("_replacedBy"))
                  } yield result
                case None =>
                  Future(0)
              }
            case None =>
              modify(None) match {
                case Some(document) =>
                  insert(collection, document, sequence).map(_ => 1)
                case None =>
                  Future(0)
              }
          }
        }
      } yield result
    }

    def insert(collection: MongoDbCollection[BsonDocument], document: T, sequence: Long): Future[Unit] = {
      val newDoc = BsonDocumentWrapper.asBsonDocument(document, codecRegistry)
      newDoc.append("_id", new BsonInt64(sequence))
      newDoc.append("_time", new BsonDateTime(System.currentTimeMillis()))
      collection.insert(newDoc).map(_ => ())
    }

    queueFuture[Int](() => modifyInProcess, f => { modifyInProcess = f }, () => update())
  }

  def delete(filters: Bson = new BsonDocument()): Future[Long] = {
    for {
      collection <- collection
      result <- collection.updateMany(Filters.and(filters,
        Filters.or(Filters.exists("_expireTime", false))),
        Updates.set("_expireTime", new BsonDateTime(System.currentTimeMillis()))).map(_.getModifiedCount)
    } yield result
  }

  def drop(): Future[Unit] = {
    for {
      collection <- collection
      result <- collection.drop().map(_ => ())
    } yield result
  }

  def subscribe(filters: Bson = new BsonDocument(), fromSequence: Option[Long] = None)
               (implicit log: Logger): Source[Sequenced[T], NotUsed] = {
    val filtersArg = Filters.and(filters, fromSequence.map(sequence => Filters.gte("_id", sequence)).getOrElse(new BsonDocument()))
    val source = for {
      storedDocuments <- findSequenced(filtersArg, Some(Sorts.ascending("_id")))
    } yield {
      val bufferSource = Source.fromIterator(() => synchronized { publisherBuffer.iterator })
      val collectionSource = Source.fromIterator(() => storedDocuments.iterator)
      var sequence = fromSequence.getOrElse(0L)
      Source.combine(collectionSource, bufferSource, publisherSource)(Concat(_))
        .filter(doc => {
          if (doc.sequence >= sequence) {
            sequence = doc.sequence + 1
            true
          } else {
            false
          }
        })
    }
    Source.futureSource(source).mapMaterializedValue(_ => NotUsed)
  }

  private def findDocuments(filters: Bson = new BsonDocument(), sort: Option[Bson] = None, limit: Option[Int] = None): Future[Seq[BsonDocument]] = {
    for {
      collection <- collection
      docs <- collection.find(Filters.and(filters,
        Filters.or(Filters.exists("_expireTime", false), Filters.exists("_replacedBy", true))), sort)
    } yield {
      val notReplaced = docs.filter(!_.containsKey("_replacedBy")).map(_.get("_id").asInt64())
      docs.filter(doc => { !doc.containsKey("_replacedBy") || !notReplaced.contains(doc.get("_replacedBy").asInt64()) })
      limit match {
        case Some(limit) => docs.take(limit)
        case None => docs
      }
    }
  }

  private def getNextSequence(sequenceName: String, increment: Int = 1): Future[Long] = {
    def getNextSequence(): Future[Long] = {
      (for {
        sequenceCollection <- sequenceCollection
        sequence <- { sequenceCollection.findOneAndUpdate(
          Filters.eq("name", sequenceName), Updates.inc("sequence", increment),
          new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)) }
      } yield sequence).map(_.map(_.sequence).head)
    }

    queueFuture[Long](() => nextSequenceInProcess, nextSequenceInProcess = _, () => getNextSequence())
  }

  private def queueFuture[T](getLast: () => Option[Future[T]], setLast: (Option[Future[T]]) => Unit,
                             process: () => Future[T]): Future[T] = {
    synchronized {
      val future = getLast() match {
        case Some(currentProcess) if !currentProcess.isCompleted =>
          currentProcess.transformWith(_ => process())
        case _ =>
          process()
      }
      setLast(Some(future))
      future.andThen {
        case _ =>
          synchronized {
            getLast() match {
              case Some(`future`) =>
                setLast(None)
              case _ =>
            }
          }
      }
      future
    }
  }
}
