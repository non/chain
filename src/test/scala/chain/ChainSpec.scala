package chain

import org.scalacheck.{ Arbitrary, Gen, Prop, Properties }
import Arbitrary.arbitrary
import Prop.forAll

import org.scalacheck._

object ChainSpec extends Properties("Chain") {

  def vecGen[A: Arbitrary](d: Int): Gen[Chain[A]] = {
    val e = arbitrary[Iterable[A]].map(Chain(_))
    def c = {
      val g = vecGen[A](d - 1)
      for { lhs <- g; rhs <- g } yield lhs ++ rhs
    }
    if (d <= 0) e else Gen.oneOf(e, c)
  }

  implicit def vecArb[A: Arbitrary]: Arbitrary[Chain[A]] =
    Arbitrary(vecGen[A](5))

  property("Chain(x).toVector = x.toVector") =
    forAll { (x: Iterable[Int]) =>
      Chain(x).toVector == x.toVector
    }

  property("(x ++ y).toVector = x.toVector ++ y.toVector") =
    forAll { (x: Chain[Int], y: Chain[Int]) =>
      (x ++ y).toVector == (x.toVector ++ y.toVector)
    }

  property("(x == y) = (x.toVector == y.toVector)") =
    forAll { (x: Chain[Int], y: Chain[Int]) =>
      (x == y) == (x.toVector == y.toVector)
    }

  def cmp(n: Int): Int = if (n < 0) -1 else if (n > 0) 1 else 0

  property("(x compare y) = -(y compare x)") =
    forAll { (x: Chain[Int], y: Chain[Int]) =>
      cmp(x compare y) == -cmp(y compare x)
    }

  property("(x == y) = ((x compare y) == 0)") =
    forAll { (x: Chain[Int], y: Chain[Int]) =>
      (x == y) == ((x compare y) == 0)
    }

  property("x ++ empty = empty ++ x = x") =
    forAll { (x: Chain[Int]) =>
      val e = Chain.empty[Int]
      ((e ++ x) == x) && ((x ++ e) == x)
    }

  property("(x ++ y) ++ z = x ++ (y ++ z)") =
    forAll { (x: Chain[Int], y: Chain[Int], z: Chain[Int]) =>
      ((x ++ y) ++ z) == (x ++ (y ++ z))
    }

  property("(a +: x) = (Chain(Vector(a)) ++ x)") =
    forAll { (a: Int, x: Chain[Int]) =>
      (a +: x) == (Chain(Vector(a)) ++ x)
    }

  property("(x :+ a) = (x ++ Chain(Vector(a)))") =
    forAll { (x: Chain[Int], a: Int) =>
      (x :+ a) == (x ++ Chain(Vector(a)))
    }

  property("(x == y) -> (x.## == y.##)") =
    forAll { (x: Chain[Int], y: Chain[Int]) =>
      if (x == y) x.## == y.## else true
    }

  property("compression is consistent") =
    forAll { (x: Chain[Int]) =>
      val c = x.compress
      (x == c) && (x.## == c.##) && (x.toVector == c.toVector)
    }

  property("x.map(f).toVector = x.toVector.map(f)") =
    forAll { (x: Chain[Int], f: Int => Int) =>
      x.map(f).toVector == x.toVector.map(f)
    }

  property("x.filter(p).toVector = x.toVector.filter(p)") =
    forAll { (x: Chain[Int], p: Int => Boolean) =>
      x.filter(p).toVector == x.toVector.filter(p)
    }

  property("x.flatMap(f).toVector = x.toVector.flatMap(f)") =
    forAll { (x: Chain[Int], f: Int => Vector[Int]) =>
      x.flatMap(n => Chain(f(n))).toVector == x.toVector.flatMap(f)
    }

  property("x.foldLeft(b)(f) = x.toVector.foldLeft(b)(f)") =
    forAll { (x: Chain[Int], b: Double, f: (Double, Int) => Double) =>
      x.foldLeft(b)(f) == x.toVector.foldLeft(b)(f)
    }
  
  property("x.find(p) = x.toVector.find(p)") =
    forAll { (x: Chain[Int], p: Int => Boolean) =>
      x.find(p) == x.toVector.find(p)
    }

  property("Chain.all(x, y, z) = Chain(x) ++ Chain(y) ++ Chain(z)") =
    forAll { (x: Iterable[Int], y: Iterable[Int], z: Iterable[Int]) =>
      Chain.all(x, y, z) == (Chain(x) ++ Chain(y) ++ Chain(z))
    }
}
