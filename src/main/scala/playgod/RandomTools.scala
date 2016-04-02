package playgod

object RandomTools {
  def rInt = util.Random.nextInt.abs
  def rGaussian = util.Random.nextGaussian
  def rDouble = util.Random.nextDouble
  def inCase(probability:Double) = rDouble < probability

  def rouletteWheelSelection[T](seq:IndexedSeq[T], score:(T) => Double) = {
    val min = score(seq.minBy(score))
    val offset = if( min < 0.0 ) min.abs else 0.0
    def offsetScore(a:T) = offset + score(a)
    val sum = seq.map(offsetScore).sum
    val r = rDouble * sum

    var i = 0
    var currentSum = offsetScore(seq(i))
    while( currentSum < r ) {
      i += 1
      currentSum += offsetScore(seq(i))
    }
    seq(i)
  }

  def rankSelection[T](seq:IndexedSeq[T], score:(T) => Double) = {
    val rankSeq = seq.sortBy(score).zipWithIndex.map{case (a,i) => (a,(i+1).toDouble)}

    val sum = rankSeq.map(_._2).sum
    val r = rDouble * sum

    var i = 0
    var currentSum = rankSeq(i)._2
    while( currentSum < r ) {
      i += 1
      currentSum += rankSeq(i)._2
    }
    rankSeq(i)._1
  }

  def tournamentSelection[T](seq:IndexedSeq[T], score:(T) => Double) = {
    (0 until 5).map(_ => seq(rInt % seq.size)).maxBy(score)
  }
}
