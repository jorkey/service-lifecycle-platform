package com.vyulabs.update.distribution.client

import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

import Utils._

private object Utils {
  def getSubselection(cl: Class[_]): String = ""

  def printFields(cl: Class[_], tabs: Int = 0): Unit = {
    if (!cl.isArray && !cl.isPrimitive && cl != classOf[String]) {
      cl.getDeclaredFields.foreach(f => {
        println(" ".repeat(tabs) + f.getName)
        if (f.getName != "version") {
          printFields(f.getType, tabs + 2)
        }
      })
    }
  }
}

case class GraphqlArgument(name: String, value: JsValue, inputType: String)

object GraphqlArgument {
  def apply[T](arg : (String, T), inputType: String = "")
              (implicit classTag: ClassTag[T], writer: JsonWriter[T]): GraphqlArgument = {
    GraphqlArgument(arg._1, arg._2.toJson,
      if (inputType.isEmpty) {
        if (arg._2.isInstanceOf[Option[_]]) {
          arg._2.asInstanceOf[Option[_]] match {
            case Some(obj) =>
              obj.getClass.getSimpleName
            case None =>
              "String"
          }
        } else {
          arg._2.getClass.getSimpleName
        }
      } else inputType)
  }
}

case class GraphqlRequest[Response](request: String, command: String, arguments: Seq[GraphqlArgument] = Seq.empty)
                                   (implicit responseClassTag: ClassTag[Response], reader: JsonReader[Response]) {
  def encodeRequest(): JsObject = {
    val types = arguments.foldLeft("")((args, arg) => { args + (if (!args.isEmpty) " ," else "") + s"$$${arg.name}: ${arg.inputType}!" })
    val args = arguments.foldLeft("")((args, arg) => { args + (if (!args.isEmpty) ", " else "") + s"${arg.name}: $$${arg.name}" })
    val subSelection = getSubselection(responseClassTag.runtimeClass)
    val query = s"${request} ${command}(${types}) { ${command} (${args}) ${subSelection} }"
    val variables = arguments.foldLeft(Map.empty[String, JsValue])((map, arg) => map + (arg.name -> arg.value))
    JsObject("query" -> JsString(query), "variables" -> variables.toJson)
  }

  def decodeResponse(responseJson: JsObject): Either[Response, String] = {
    val fields = responseJson.fields
    fields.get("data") match {
      case Some(data) if (data != JsNull) =>
        val response = data.asJsObject.fields.get(command).getOrElse(
          return Right(s"No field ${command} in the response data: ${data}"))
        Left(response.convertTo[Response])
      case _ =>
        fields.get("errors") match {
          case Some(errors) =>
            Right(s"Graphql request error: ${errors}")
          case None =>
            Right(s"Graphql invalid response: ${responseJson}")
        }
    }
  }
}

object GraphqlQuery {
  def apply[Response](command: String, arguments: Seq[GraphqlArgument] = Seq.empty)
                     (implicit responseClassTag: ClassTag[Response], reader: JsonReader[Response]) = {
    GraphqlRequest[Response]("query", command, arguments)
  }
}

object GraphqlMutation {
  def apply(command: String, arguments: Seq[GraphqlArgument] = Seq.empty) = {
    GraphqlRequest[Boolean]("mutation", command, arguments)
  }
}