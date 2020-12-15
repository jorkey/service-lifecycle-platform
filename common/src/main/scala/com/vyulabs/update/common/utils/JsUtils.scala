package com.vyulabs.update.common

import spray.json.{JsObject, JsValue}

package object utils {
  implicit class MergedJsObject(obj: JsValue) {
    def merge(obj2: JsValue): JsObject = {

      def mergeLoop(val1: JsValue, val2: JsValue): JsValue = {
        (val1, val2) match {
          case (obj1: JsObject, obj2: JsObject) => {
            val fields1 = obj1.fields
            val fields2 = obj2.fields
            val diff1 = fields1.keySet -- fields2.keySet
            val diff2 = fields2.keySet -- fields1.keySet
            val common = fields1.keySet.intersect(fields2.keySet)
            val merged = common.map { key =>
              val val1 = fields1(key)
              val val2 = fields2(key)
              (val1, val2) match {
                case (val1: JsObject, val2: JsObject) => (key -> mergeLoop(val1, val2))
                case (_, val2) => (key -> val2)
              }
            }.toMap
            val distinct = fields1.filter(t => diff1(t._1)) ++ fields2.filter(t => diff2(t._1))
            JsObject(merged ++ distinct)
          }
          case (_, val2) =>
            val2
        }
      }
      mergeLoop(obj, obj2).asJsObject
    }
  }
}