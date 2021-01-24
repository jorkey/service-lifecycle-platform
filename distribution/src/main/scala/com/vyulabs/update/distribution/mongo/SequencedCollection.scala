package com.vyulabs.update.distribution.mongo

import com.mongodb.client.model._
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{BsonDateTime, BsonDocument, BsonDocumentReader, BsonDocumentWrapper}
import org.mongodb.scala.bson.BsonInt64
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

case class Sequenced[T](sequence: Long, document: T)

class SequencedCollection[T: ClassTag](val name: String,
                                       collection: Future[MongoDbCollection[BsonDocument]], sequenceCollection: Future[MongoDbCollection[SequenceDocument]],
                                       historyExpireDays: Int = 7)
                            (implicit executionContext: ExecutionContext, codecRegistry: CodecRegistry) {
  implicit val log = LoggerFactory.getLogger(getClass)

  private var modifyInProcess = Option.empty[Future[Int]]

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
        Sequenced[T](doc.getInt64("_id").getValue, codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build()))
      })
    }
  }

  def update(filters: Bson, modify: Option[T] => Option[T]): Future[Int] = {
    def process(): Future[Int] = {
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

    def queueFuture(): Future[Int] = {
      synchronized {
        val future = modifyInProcess match {
          case Some(currentProcess) if !currentProcess.isCompleted =>
            currentProcess.transformWith(_ => process())
          case _ =>
            process()
        }
        modifyInProcess = Some(future)
        future.andThen {
          case _ =>
            synchronized {
              modifyInProcess match {
                case Some(`future`) =>
                  modifyInProcess = None
                case _ =>
              }
            }
        }
        future
      }
    }

    queueFuture()
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
      result <- collection.dropItems()
    } yield result
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
    (for {
      sequenceCollection <- sequenceCollection
      sequence <- { sequenceCollection.findOneAndUpdate(
        Filters.eq("name", sequenceName), Updates.inc("sequence", increment),
        new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)) }
    } yield sequence).map(_.map(_.sequence).head)
  }
}