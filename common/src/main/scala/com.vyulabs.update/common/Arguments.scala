package com.vyulabs.update.common

package com.vyulabs.common.utils

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 05.03.19.
  * Copyright FanDate, Inc.
  */
class Arguments(args: Map[String, String]) {
  def get() = args

  def getOptionValue(arg: String): Option[String] = args.get(arg)

  def getOptionIntValue(arg: String): Option[Int] = getOptionValue(arg) match {
    case Some(value) => Some(value.toInt)
    case None => None
  }

  def getOptionBooleanValue(arg: String): Option[Boolean] = getOptionValue(arg) match {
    case Some(value) => Some(value.toBoolean)
    case None => None
  }

  def getValue(arg: String): String = args.get(arg) match {
    case Some(arg) => arg
    case None => throw new RuntimeException(s"Argument ${arg} is not defined")
  }

  def getIntValue(arg: String): Int = getValue(arg).toInt

  def getValue(arg: String, default: String): String = args.getOrElse(arg, default)

  override def toString(): String = {
    args.foldLeft("") {
      case (str, (name, value)) =>
        val entry = name + "=" + value
        if (str.isEmpty) entry else str + ", " + entry
    }
  }
}

object Arguments {
  def empty = new Arguments(Map.empty)

  def parse(args: Array[String]): Arguments = {
    val map = args.foldLeft(Map.empty[String, String])((map, arg) => {
      val index = arg.indexOf('=')
      if (index > 0) {
        val name = arg.substring(0, index)
        val value = arg.substring(index + 1)
        map + (name -> value)
      } else {
        throw new RuntimeException(s"Invalid argument ${arg}, must be in format name=value")
      }
    })
    new Arguments(map)
  }
}

