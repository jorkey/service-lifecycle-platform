package com.vyulabs.update.common.common

object Misc {
  def seqToCommaSeparatedString[T](seq: Seq[T]): String = {
    seq.foldLeft(new String)((str, v) =>
      str + (if (str.isEmpty) v.toString() else s", ${v.toString}"))
  }
}
