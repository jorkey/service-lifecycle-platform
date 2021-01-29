package com.vyulabs.update.common.common

import java.math.BigInteger
import java.security.SecureRandom

class IdGenerator {
  private val random = new SecureRandom()

  def generateId(len: Int): String = {
    new BigInteger(len * 5, random).toString(32)
  }
}
