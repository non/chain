package vec

import scala.annotation.tailrec

/**
 * Vec wraps Iterable[A] instances to enable fast concatenation and
 * traversal.
 *
 * 
 */
sealed abstract class Vec[+A] {

  /**
   * Concatenate two Vecs.
   *
   * This is a fast O(1) operation: it simply returns a new Vec.Concat
   * instance wrapping the arguments.
   */
  def ++[A1 >: A](that: Vec[A1]): Vec[A1] =
    Vec.Concat(this, that)

  /**
   * Append a value to a Vec.
   *
   * (x :+ a) is equivalent to (x ++ Vec.single(a)).
   */
  def :+[A1 >: A](a: A1): Vec[A1] =
    Vec.Concat(this, Vec.Elems(a :: Nil))

  /**
   * Prepend a value to a Vec.
   *
   * (a +: x) is equivalent to (Vec.single(a) ++ x).
   */
  def +:[A1 >: A](a: A1): Vec[A1] =
    Vec.Concat(Vec.Elems(a :: Nil), this)

  /**
   * Return an iterator over the contents of this Vec.
   *
   * This is a fast O(1) operation, although traversing the iterator
   * is itself an O(n) operation (which also uses O(n) heap).
   */
  def iterator: Iterator[A] =
    this match {
      case Vec.Elems(values) => values.iterator
      case vec => new Vec.VecIterator(vec)
    }

  /**
   * Compress a Vec, removing its internal structure.
   *
   * In general, this is an O(n) operation which will compact this Vec
   * into a single Vec.Elems instance wrapping a vector. However, if
   * the Vec is already compressed (i.e. Vec.Elems) this does not do
   * any work or allocate a new Vec.
   *
   * When working with a large number of very small collections, this
   * method can have a big impact.
   */
  def compress: Vec[A] =
    this match {
      case Vec.Elems(_) => this
      case _ => Vec.Elems(iterator.toVector)
    }

  /**
   * Translate a Vec using the given function.
   *
   * This is an O(n) operation.
   *
   * The resulting Vec will naturally be compressed.
   */
  def map[B](f: A => B): Vec[B] =
    Vec.Elems(iterator.map(f).toVector)

  /**
   * Translate a Vec using the given function.
   *
   * This is an O(n * m) operation, where n is the length of this Vec
   * and m represents the average length of the Vec instances produced
   * by f.
   */
  def flatMap[B](f: A => Vec[B]): Vec[B] =
    Vec.Elems(iterator.flatMap(a => f(a).iterator).toVector)

  /**
   * Filter out some elements of a Vec given a predicate.
   *
   * This is an O(n) operation.
   *
   * The resulting Vec will naturally be compressed.
   */
  def filter(p: A => Boolean): Vec[A] =
    Vec.Elems(iterator.filter(p).toVector)

  /**
   * Combine the elements of a Vec into a single value, using a
   * starter value and an associative function.
   */
  def foldLeft[B](b: B)(f: (B, A) => B): B = {
    @tailrec def loop(b0: B, v: Vec[A], stack: List[Vec[A]]): B =
      v match {
        case Vec.Elems(values) =>
          val b1 = values.foldLeft(b0)(f)
          stack match {
            case h :: t => loop(b1, h, t)
            case Nil => b1
          }
        case Vec.Concat(lhs, rhs) =>
          loop(b0, lhs, rhs :: stack)
      }
    loop(b, this, Nil)
  }

  /**
   * Search for the first element that satisfies the given predicate.
   *
   * In the worst-case this is an O(n) operation, but it will
   * short-circuit as soon as a single match is found.
   */
  def find(p: A => Boolean): Option[A] = {
    @tailrec def loop(v: Vec[A], stack: List[Vec[A]]): Option[A] =
      v match {
        case Vec.Elems(values) =>
          values.find(p) match {
            case None =>
              stack match {
                case h :: t => loop(h, t)
                case Nil => None
              }
            case some =>
              some
          }
        case Vec.Concat(lhs, rhs) =>
          loop(lhs, rhs :: stack)
      }
    loop(this, Nil)
  }

  /**
   * Return whether the predicate is true for all elements or not.
   */
  def forall(p: A => Boolean): Boolean =
    find(a => !p(a)).isEmpty

  /**
   * Return whether the predicate is true for any elements or not.
   */
  def exists(p: A => Boolean): Boolean =
    find(p).nonEmpty

  /**
   * Loop over this Vec, applying the given function.
   *
   * This is an O(n) operation.
   */
  def foreach(f: A => Unit): Unit =
    iterator.foreach(f)

  /**
   * Allow this Vec to be used where Iterable[A] is required.
   *
   * By default Vec does not extend Iterable[A], to avoid inheriting
   * inefficient methods from that API.
   *
   * This is a fast O(1) operation.
   */
  def toIterable: Iterable[A] =
    new Vec.IterableVec(this)

  /**
   * Conver this Vec to a Vector.
   *
   * This is an O(n) operation.
   */
  def toVector: Vector[A] =
    this match {
      case Vec.Elems(values) => values.toVector
      case _ => iterator.toVector
    }

  /**
   * Compare two Vec instances.
   */
  def compare[A1 >: A](that: Vec[A1])(implicit ev: Ordering[A1]): Int = {
    val it0 = this.iterator
    val it1 = that.iterator
    while (it0.hasNext && it1.hasNext) {
      val c = ev.compare(it0.next, it1.next)
      if (c != 0) return c
    }
    if (it0.hasNext) 1 else if (it1.hasNext) -1 else 0
  }

  /**
   * Produce a string representation of this Vec.
   *
   * This is an O(n) operation, which will display the entire contents
   * of the Vec.
   */
  override def toString: String =
    iterator.mkString("Vec(", ", ", ")")

  /**
   * Universal equality for Vec.
   *
   * In the worst-case this is an O(n) operation, but may be faster
   * due to type mistmatches or finding unequal elements early in the
   * Vecs.
   */
  override def equals(that: Any): Boolean =
    that match {
      case that: Vec[_] =>
        val it0 = this.iterator
        val it1 = that.iterator
        while (it0.hasNext && it1.hasNext) {
          if (it0.next != it1.next) return false
        }
        it0.hasNext == it1.hasNext
      case _ =>
        false
    }

  /**
   * Hash codes for Vec.
   *
   * This is an O(n) operation. It is consistent with equals. This
   * means that if (x == y) then (x.hashCode == y.hashCode).
   */
  override def hashCode: Int = {
    var x = 0xfabca777
    val it = iterator
    while (it.hasNext) {
      val e = it.next
      x = x * 17 + (if (e == null) 0 else e.##)
    }
    x
  }
}

object Vec {

  // a single collection of values
  case class Elems[A](values: Iterable[A]) extends Vec[A]

  // concatenations
  case class Concat[A](lhs: Vec[A], rhs: Vec[A]) extends Vec[A]

  // a shared "empty" vec instance
  val Empty: Vec[Nothing] = Elems(Nil)

  /**
   * Produce an empty Vec[A].
   */
  def empty[A]: Vec[A] = Empty

  /**
   * Wrap a single A value in a Vec[A].
   */
  def single[A](a: A): Vec[A] = Elems(a :: Nil)

  /**
   * Wrap a collection in a Vec[A].
   */
  def apply[A](values: Iterable[A]): Vec[A] = Elems(values)

  /**
   * Iterator for a Vec[A].
   *
   * This is where the magic happens. To efficiently traverse into
   * Vec.Concat instances, we build our own stack on the heap. When we
   * descend into the LHS of a Concat we push the RHS onto this stack,
   * so that when we exhaust the LHS we will remember to get an
   * iterator from the RHS too.
   *
   * As mentioned earlier, iterating over VecIterator[A] is an O(n)
   * operation, that also consumes O(n) heap (the previously-mentioned
   * stack.)
   */
  class VecIterator[A](vec: Vec[A]) extends Iterator[A] {
    var it: Iterator[A] = Iterator.empty
    var stack: List[Vec[A]] = Nil

    descend(vec)

    def ascend(): Unit =
      if (it.hasNext) () else {
        stack match {
          case Nil =>
            ()
          case h :: t =>
            stack = t
            descend(h)
        }
      }

    @tailrec private def descend(v: Vec[A]): Unit = {
      @tailrec def loop(v: Vec[A]): Boolean = {
        v match {
          case Concat(lhs, rhs) =>
            stack = rhs :: stack
            loop(lhs)
          case Elems(values) =>
            it = values.iterator
            if (it.hasNext) true else false
        }
      }
      if (loop(v)) () else stack match {
        case Nil =>
          ()
        case h :: t =>
          stack = t
          descend(h)
      }
    }

    def hasNext(): Boolean =
      if (it.hasNext) true else {
        ascend()
        it.hasNext
      }

    def next(): A =
      if (it.hasNext) it.next else {
        ascend()
        it.next
      }
  }

  /**
   * Wrapper for Vec[A] that provides Iterable[A].
   */
  class IterableVec[+A](vec: Vec[A]) extends Iterable[A] {
    def iterator: Iterator[A] = vec.iterator
  }
}
