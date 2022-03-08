package com.vyulabs.update.distribution.mongo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{BroadcastHub, Concat, Keep, Source}
import com.mongodb.client.model._
import com.vyulabs.update.common.common.RaceRingBuffer
import com.vyulabs.update.distribution.common.AkkaCallbackSource
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{BsonDateTime, BsonDocument, BsonDocumentReader, BsonDocumentWrapper}
import org.mongodb.scala.bson.BsonInt64
import org.slf4j.Logger

import java.util.Date
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

case class Sequenced[T](sequence: Long, document: T)

case class Timed[T](time: Date, document: T)

case class DocumentAlreadyExists() extends Exception
case class NoSuchDocument() extends Exception

class SequencedCollection[T: ClassTag](val name: String,
                                       collection: Future[MongoDbCollection[BsonDocument]], sequenceCollection: Future[MongoDbCollection[SequenceDocument]],
                                       historyExpireDays: Int = 7, createIndex: Boolean = true)
                                      (implicit system: ActorSystem, executionContext: ExecutionContext,
                                       codecRegistry: CodecRegistry) {
  private implicit val log = Logging(system, this.getClass)

  private val (publisherCallback, publisherSource) = Source.fromGraph(new AkkaCallbackSource[Sequenced[T]]())
    .toMat(BroadcastHub.sink)(Keep.both).run()
  private val publisherBuffer = new RaceRingBuffer[Sequenced[T]](100)

  private var modifyInProcess = Option.empty[Future[Int]]
  private var nextSequenceInProcess = Option.empty[Future[Long]]

  if (createIndex) {
    collection.map(_.createIndex(Indexes.ascending("_sequence")))
    collection.map(_.createIndex(Indexes.descending("_sequence")))
    collection.map(_.createIndex(Indexes.ascending("_archiveTime"),
      new IndexOptions().expireAfter(historyExpireDays, TimeUnit.DAYS)))
  }

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
          doc.append("_sequence", new BsonInt64(seq)); seq += 1
          doc.append("_modifyTime", new BsonDateTime(System.currentTimeMillis()))
          doc
        }
        collection.insert(docs).map(_ => sequence)
      }
    } yield {
      var seq = sequence - documents.size + 1
      synchronized {
        documents.foreach { doc =>
          val line = Sequenced(seq, doc)
          println(s"publish line ${line}")
          publisherBuffer.push(line)
          publisherCallback.invoke(line)
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
        Sequenced[T](doc.getInt64("_sequence").getValue, codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build()))
      })
    }
  }

  def distinctField[T](fieldName: String, filters: Bson = new BsonDocument())
                      (implicit classTag: ClassTag[T]): Future[Seq[T]] = {
    for {
      collection <- collection
      docs <- collection.distinct[T](fieldName, filters)
      /* TODO to slow now
      docs <- collection.distinct[T](fieldName, Filters.and(filters,
        Filters.or(Filters.exists("_archiveTime", false), Filters.exists("_replacedBy", true))))
       */
    } yield docs
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

    def insert(collection: MongoDbCollection[BsonDocument], document: T, sequence: Long): Future[Unit] = {
      val newDoc = BsonDocumentWrapper.asBsonDocument(document, codecRegistry)
      newDoc.append("_sequence", new BsonInt64(sequence))
      newDoc.append("_modifyTime", new BsonDateTime(System.currentTimeMillis()))
      collection.insert(newDoc).map(_ => ())
    }

    def updateAndInsert(doc: Option[BsonDocument]): Future[Int] = {
      for {
        collection <- collection
        sequence <- getNextSequence(collection.getName(), 1)
        result <- {
          doc match {
            case Some(doc) =>
              val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
              val docId = doc.getInt64("_sequence")
              val document = codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build())
              modify(Some(document)) match {
                case Some(document) =>
                  for {
                    _ <- collection.updateOne(Filters.eq("_sequence", docId),
                      Updates.combine(Updates.set("_replacedBy", new BsonInt64(sequence)),
                        Updates.set("_archiveTime", new BsonDateTime(System.currentTimeMillis()))))
                    result <- insert(collection, document, sequence).map(_ => 1)
                    _ <- collection.updateOne(Filters.eq("_sequence", docId),
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

    queueFuture[Int](() => modifyInProcess, f => { modifyInProcess = f }, () => update())
  }

  def add(filters: Bson, doc: T): Future[Int] = {
    update(filters, _ match {
      case Some(_) =>
        throw DocumentAlreadyExists()
      case None =>
        Some(doc)
    })
  }

  def change(filters: Bson, modify: T => T): Future[Int] = {
    update(filters, _ match {
      case Some(oldValue) =>
        Some(modify(oldValue))
      case None =>
        throw NoSuchDocument()
    })
  }

  def delete(filters: Bson = new BsonDocument()): Future[Long] = {
    for {
      collection <- collection
      result <- collection.updateMany(Filters.and(filters,
        Filters.or(Filters.exists("_archiveTime", false))),
        Updates.set("_archiveTime", new BsonDateTime(System.currentTimeMillis()))).map(_.getModifiedCount)
    } yield result
  }

  def drop(): Future[Unit] = {
    for {
      collection <- collection
      result <- collection.drop().map(_ => ())
    } yield result
  }

  def history(filters: Bson = new BsonDocument(), limit: Option[Int] = None): Future[Seq[Timed[T]]] = {
    for {
      docs <- getHistory(filters, limit)
    } yield {
      docs.map(doc => {
        val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
        val modifyTime = new Date(doc.getDateTime("_modifyTime").getValue)
        Timed(modifyTime, codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build()))
      })
    }
  }

  def subscribe(filters: Bson = new BsonDocument(), from: Option[Long] = None, startLimit: Option[Int])
               (implicit log: Logger): Source[Sequenced[T], NotUsed] = {
    val filtersArg = Filters.and(filters,
      from.map(sequence => Filters.gte("_sequence", sequence)).getOrElse(new BsonDocument()))
    val sort = if (!from.isEmpty || startLimit.isEmpty) Sorts.ascending("_sequence") else Sorts.descending("_sequence")
    val source = for {
      storedDocuments <- findSequenced(filtersArg, Some(sort), startLimit)
        .map(_.sortBy(_.sequence))
    } yield {
      val bufferSource = Source.fromIterator(() => publisherBuffer.makeIterator())
      val collectionSource = Source.fromIterator(() => storedDocuments.iterator)
      var sequence = from.getOrElse(0L)
      Source.combine(collectionSource, bufferSource, publisherSource)(Concat(_))
        .buffer(1000, OverflowStrategy.fail)
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

  def getStorageSize(): Future[Long] = {
    collection.map(_.getStorageSize()).flatten
  }

  private def findDocuments(filters: Bson = new BsonDocument(), sort: Option[Bson] = None, limit: Option[Int] = None): Future[Seq[BsonDocument]] = {
    for {
      collection <- collection
      docs <- collection.find(Filters.and(filters,
        Filters.or(Filters.exists("_archiveTime", false), Filters.exists("_replacedBy", true))),
          sort, limit)
    } yield {
      val notReplaced = docs.filter(!_.containsKey("_replacedBy")).map(_.get("_sequence").asInt64())
      docs.filter(doc => { !doc.containsKey("_replacedBy") || !notReplaced.contains(doc.get("_replacedBy").asInt64()) })
    }
  }

  private def getHistory(filters: Bson = new BsonDocument(), limit: Option[Int] = None): Future[Seq[BsonDocument]] = {
    for {
      collection <- collection
      docs <- collection.find(filters,
        Some(Sorts.descending("_sequence")), limit).map(_.sortBy(_.get("_sequence").asInt64()))
    } yield docs
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
