package com.vyulabs.update.distribution.mongo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{BroadcastHub, Concat, Keep, Sink, Source}
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
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

case class Sequenced[T](sequence: Long, modifyTime: Option[Date], document: T)

case class DocumentAlreadyExists() extends Exception
case class NoSuchDocument() extends Exception

class SequencedCollection[T: ClassTag](val name: String,
                                       collection: Future[MongoDbCollection[BsonDocument]],
                                       historyExpire: FiniteDuration = FiniteDuration(7, TimeUnit.DAYS),
                                       modifiable: Boolean = true, createIndices: Boolean = true)
                                      (implicit system: ActorSystem, executionContext: ExecutionContext,
                                       codecRegistry: CodecRegistry) {
  private implicit val log = Logging(system, this.getClass)

  private val (publisherCallback, publisherSource) = Source.fromGraph(new AkkaCallbackSource[Sequenced[T]]())
    .toMat(BroadcastHub.sink)(Keep.both).run()
  private val publisherBuffer = new RaceRingBuffer[Sequenced[T]](1000)

  private var sequence = Await.result(collection.map(_.find(sort = Some(Sorts.descending("_sequence")), limit = Some(1))
    .map(_.headOption.map(_.getInt64("_sequence").getValue).getOrElse(0L))).flatten, Duration.Inf)

  private var modifyInProcess = Option.empty[Future[Int]]

  publisherSource.to(Sink.ignore).run()

  if (createIndices) {
    collection.map(_.createIndex(Indexes.ascending("_sequence")))
    if (modifiable) {
      collection.map(_.createIndex(Indexes.ascending("_archiveTime"),
        new IndexOptions().expireAfter(historyExpire.length, historyExpire.unit)))
    }
  }

  def insert(document: T): Future[Long] = {
    insert(Seq(document))
  }

  def insert(documents: Seq[T]): Future[Long] = {
    var seq = nextSequence(documents.size) - documents.size + 1
    val modifyTime = if (modifiable) Some(new Date()) else None
    for (i <- 0 until documents.size) {
      val line = Sequenced(seq+i, modifyTime, documents(i))
      publisherBuffer.push(line)
      publisherCallback.invoke(line)
    }
    for {
      collection <- collection
      result <- {
        val docs = documents.map { document =>
          val doc = BsonDocumentWrapper.asBsonDocument(document, codecRegistry)
          doc.append("_sequence", new BsonInt64(seq)); seq += 1
          for (modifyTime <- modifyTime) {
            doc.append("_modifyTime", new BsonDateTime(modifyTime.getTime))
          }
          doc
        }
        collection.insert(docs).map(_ => sequence)
      }
    } yield result
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
        val sequence = doc.getInt64("_sequence").getValue
        val modifyTime = if (doc.containsKey("_modifyTime")) Some(new Date(doc.getDateTime("_modifyTime").getValue)) else None
        Sequenced[T](sequence, modifyTime,
          codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build()))
      })
    }
  }

  def findSequencedToStream(filters: Bson = new BsonDocument(), sort: Option[Bson] = None, limit: Option[Int] = None): Source[Sequenced[T], NotUsed] = {
    findDocumentsToStream(filters, sort, limit)
      .map(doc => {
        val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
        val sequence = doc.getInt64("_sequence").getValue
        val modifyTime = if (doc.containsKey("_modifyTime")) Some(new Date(doc.getDateTime("_modifyTime").getValue)) else None
        Sequenced[T](sequence, modifyTime,
          codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build()))
      })
  }

  def findToStream(filters: Bson = new BsonDocument(), sort: Option[Bson] = None, limit: Option[Int] = None): Source[T, NotUsed] = {
    findDocumentsToStream(filters, sort, limit).map(doc => {
      val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
      codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build())
    })
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
    assert(modifiable)
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
      if (modifiable) {
        newDoc.append("_modifyTime", new BsonDateTime(System.currentTimeMillis()))
      }
      collection.insert(newDoc).map(_ => ())
    }

    def updateAndInsert(doc: Option[BsonDocument]): Future[Int] = {
      for {
        collection <- collection
        result <- {
          val sequence = nextSequence(1)
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
                        Updates.set("_archiveTime", new BsonDateTime(
                          System.currentTimeMillis() - historyExpire.toMillis + 60000))))
                    result <- insert(collection, document, sequence).map(_ => 1)
                    _ <- collection.updateOne(Filters.eq("_sequence", docId),
                      Updates.combine(Updates.unset("_replacedBy"),
                        Updates.set("_archiveTime", new BsonDateTime(System.currentTimeMillis()))))
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
    assert(modifiable)
    update(filters, _ match {
      case Some(_) =>
        throw DocumentAlreadyExists()
      case None =>
        Some(doc)
    })
  }

  def change(filters: Bson, modify: T => T): Future[Int] = {
    assert(modifiable)
    update(filters, _ match {
      case Some(oldValue) =>
        Some(modify(oldValue))
      case None =>
        throw NoSuchDocument()
    })
  }

  def delete(filters: Bson = new BsonDocument()): Future[Long] = {
    assert(modifiable)
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

  def history(filters: Bson = new BsonDocument(), limit: Option[Int] = None): Future[Seq[Sequenced[T]]] = {
    assert(modifiable)
    for {
      docs <- getHistory(filters, limit)
    } yield {
      docs.map(doc => {
        val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
        val sequence = doc.getInt64("_sequence").getValue
        val modifyTime = if (doc.containsKey("_modifyTime")) Some(new Date(doc.getDateTime("_modifyTime").getValue)) else None
        Sequenced(sequence, modifyTime, codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build()))
      })
    }
  }

  def subscribe(filters: Bson = new BsonDocument(), from: Option[Long] = None)
               (implicit log: Logger): Source[Sequenced[T], NotUsed] = {
    val filtersArg = Filters.and(filters,
      from.map(sequence => Filters.gte("_sequence", sequence)).getOrElse(new BsonDocument()))
    val sort = Sorts.ascending("_sequence")
    val source = for {
      storedDocuments <- findSequenced(filtersArg, Some(sort))
        .map(_.sortBy(_.sequence))
    } yield {
      val bufferIterator = publisherBuffer.makeIterator()
      val documents = (storedDocuments ++ bufferIterator.toSeq).sortBy(_.sequence) // Stored documents may be with gaps
      val documentsSource = Source.fromIterator(() => documents.iterator)
      val bufferSource = Source.fromIterator(() => bufferIterator) // Makes up for the lack of newly arrived documents
      var sequence = from.getOrElse(0L)
      Source.combine(documentsSource, bufferSource, publisherSource)(Concat(_))
        .buffer(10000, OverflowStrategy.fail)
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
    val findFilters = if (modifiable) {
      Filters.and(filters,
        Filters.or(Filters.exists("_archiveTime", false), Filters.exists("_replacedBy", true)))
    } else {
      filters
    }
    for {
      collection <- collection
      docs <- collection.find(findFilters, sort, limit)
    } yield {
      if (modifiable) {
        val notReplaced = docs.filter(!_.containsKey("_replacedBy")).map(_.get("_sequence").asInt64())
        docs.filter(doc => {
          !doc.containsKey("_replacedBy") || !notReplaced.contains(doc.get("_replacedBy").asInt64())
        })
      } else {
        docs
      }
    }
  }

  private def findDocumentsToStream(filters: Bson = new BsonDocument(), sort: Option[Bson] = None, limit: Option[Int] = None): Source[BsonDocument, NotUsed] = {
    assert(!modifiable)
    Source.futureSource {
      for {
        collection <- collection
      } yield {
        collection.findToStream(filters, sort, limit)
      }
    }.mapMaterializedValue(_ => NotUsed)
  }

  private def getHistory(filters: Bson = new BsonDocument(), limit: Option[Int] = None): Future[Seq[BsonDocument]] = {
    for {
      collection <- collection
      docs <- collection.find(filters,
        Some(Sorts.descending("_sequence")), limit).map(_.sortBy(_.get("_sequence").asInt64()))
    } yield docs
  }

  def setSequence(sequence: Long): Unit = {
    synchronized { this.sequence = sequence }
  }

  private def nextSequence(increment: Int = 1): Long = {
    synchronized {
      sequence += increment
      sequence
    }
  }

  private def queueFuture[T](getLast: () => Option[Future[T]], setLast: (Option[Future[T]]) => Unit,
                             process: () => Future[T]): Future[T] = {
    synchronized {
      val future = getLast() match {
        case Some(currentProcess) =>
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
