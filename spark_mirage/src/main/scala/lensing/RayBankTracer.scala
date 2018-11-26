package lensing


import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag


class RayBankTracer() extends Serializable {

  def lensPlane(pixels:RDD[Long], p:Broadcast[MicroParameters]):RDD[(Double,Double)] = {
    pixels.map { long =>
      val x1 = ((long % p.value.width) - p.value.width / 2).toDouble * p.value.dx
      val x2 = (p.value.height / 2 - (long / p.value.width)).toDouble * p.value.dy
      (x1, x2)
    }
  }
  def apply[A <: RayBank: ClassTag](pixels:RDD[A], p:Broadcast[MicroParameters]):RDD[A] = {
    println("In raybanktracer")
    val gminus = 1.0 - p.value.shear
    val gplus = 1.0 + p.value.shear
    pixels.map {bank =>
      for (ind <- bank.indices) {
        val x1 = bank.x(ind)
        val x2 = bank.y(ind)
        var retX = gminus * x1 - p.value.smooth * x1
        var retY = x2 * gplus - p.value.smooth * x2
        var iter = 0
        var dx1 = 0.0
        var dx2 = 0.0
        var r = 0.0
        while (iter < p.value.stars.size) {
          dx1 = x1 - p.value.stars(iter).x
          dx2 = x2 - p.value.stars(iter).y
          r = dx1 * dx1 + dx2 * dx2
          retX -= p.value.stars(iter).mass * dx1 / r
          retY -= p.value.stars(iter).mass * dx2 / r
          iter += 1
        }
        bank.setSourceX(ind,retX)
        bank.setSourceY(ind,retY)
//        if (ind%10000 == 0) {
//          println(bank.x(ind) + "," + bank.y(ind) + " => " + bank.sourceX(ind) + "," + bank.sourceY(ind))
//        }
        // println(ray.sourceX + "," + ray.sourceY)
      }
      bank
    }
    // println(lensing.RayBank.sourcePosition.mkString(","))
  }
}
