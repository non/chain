package vec

import org.scalacheck.{ Arbitrary, Gen, Prop, Properties }
import Arbitrary.arbitrary
import Prop.forAll

import org.scalacheck._

object VecSpec extends Properties("Vec") {

  def vecGen[A: Arbitrary](d: Int): Gen[Vec[A]] = {
    val e = arbitrary[Iterable[A]].map(Vec(_))
    def c = {
      val g = vecGen[A](d - 1)
      for { lhs <- g; rhs <- g } yield lhs ++ rhs
    }
    if (d <= 0) e else Gen.oneOf(e, c)
  }

  implicit def vecArb[A: Arbitrary]: Arbitrary[Vec[A]] =
    Arbitrary(vecGen[A](5))

  property("Vec(x).toVector = x.toVector") =
    forAll { (x: Iterable[Int]) =>
      Vec(x).toVector == x.toVector
    }

  property("(x ++ y).toVector = x.toVector ++ y.toVector") =
    forAll { (x: Vec[Int], y: Vec[Int]) =>
      (x ++ y).toVector == (x.toVector ++ y.toVector)
    }

  property("(x == y) = (x.toVector == y.toVector)") =
    forAll { (x: Vec[Int], y: Vec[Int]) =>
      (x == y) == (x.toVector == y.toVector)
    }

  def cmp(n: Int): Int = if (n < 0) -1 else if (n > 0) 1 else 0

  property("(x compare y) = -(y compare x)") =
    forAll { (x: Vec[Int], y: Vec[Int]) =>
      cmp(x compare y) == -cmp(y compare x)
    }

  property("(x == y) = ((x compare y) == 0)") =
    forAll { (x: Vec[Int], y: Vec[Int]) =>
      (x == y) == ((x compare y) == 0)
    }

  property("x ++ empty = empty ++ x = x") =
    forAll { (x: Vec[Int]) =>
      val e = Vec.empty[Int]
      ((e ++ x) == x) && ((x ++ e) == x)
    }

  property("(x ++ y) ++ z = x ++ (y ++ z)") =
    forAll { (x: Vec[Int], y: Vec[Int], z: Vec[Int]) =>
      ((x ++ y) ++ z) == (x ++ (y ++ z))
    }

  property("(a +: x) = (Vec(Vector(a)) ++ x)") =
    forAll { (a: Int, x: Vec[Int]) =>
      (a +: x) == (Vec(Vector(a)) ++ x)
    }

  property("(x :+ a) = (x ++ Vec(Vector(a)))") =
    forAll { (x: Vec[Int], a: Int) =>
      (x :+ a) == (x ++ Vec(Vector(a)))
    }

  property("(x == y) -> (x.## == y.##)") =
    forAll { (x: Vec[Int], y: Vec[Int]) =>
      if (x == y) x.## == y.## else true
    }

  property("compression is consistent") =
    forAll { (x: Vec[Int]) =>
      val c = x.compress
      (x == c) && (x.## == c.##) && (x.toVector == c.toVector)
    }

  property("x.map(f).toVector = x.toVector.map(f)") =
    forAll { (x: Vec[Int], f: Int => Int) =>
      x.map(f).toVector == x.toVector.map(f)
    }

  property("x.filter(p).toVector = x.toVector.filter(p)") =
    forAll { (x: Vec[Int], p: Int => Boolean) =>
      x.filter(p).toVector == x.toVector.filter(p)
    }

  property("x.flatMap(f).toVector = x.toVector.flatMap(f)") =
    forAll { (x: Vec[Int], f: Int => Vector[Int]) =>
      x.flatMap(n => Vec(f(n))).toVector == x.toVector.flatMap(f)
    }

  property("x.foldLeft(b)(f) = x.toVector.foldLeft(b)(f)") =
    forAll { (x: Vec[Int], b: Double, f: (Double, Int) => Double) =>
      x.foldLeft(b)(f) == x.toVector.foldLeft(b)(f)
    }
  
  property("x.find(p) = x.toVector.find(p)") =
    forAll { (x: Vec[Int], p: Int => Boolean) =>
      x.find(p) == x.toVector.find(p)
    }
}
