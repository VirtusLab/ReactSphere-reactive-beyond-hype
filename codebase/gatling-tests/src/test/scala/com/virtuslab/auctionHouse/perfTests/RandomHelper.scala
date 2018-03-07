package com.virtuslab.auctionHouse.perfTests

import com.virtuslab.auctions.Categories
import org.apache.commons.lang3.RandomStringUtils

import scala.util.Random

trait RandomHelper {

  def randStr = RandomStringUtils.randomAlphabetic(15)

  def randSeqValue[T](iter: Seq[T]) = {
    iter(Random.nextInt(iter.size))
  }

  def randCategory = randSeqValue(Categories)

  def randPosNum = BigDecimal(Random.nextInt(1000) + 1)
}
