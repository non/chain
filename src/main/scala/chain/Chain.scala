package chain

import scala.annotation.tailrec

/**
 * Chain wraps Iterable[A] instances to enable fast concatenation and
 * traversal.
 */
sealed abstract class Chain[+A] {

  /**
   * Concatenate two Chains.
   *
   * This is a fast O(1) operation: it simply returns a new
   * Chain.Concat instance wrapping the arguments.
   */
  def ++[A1 >: A](that: Chain[A1]): Chain[A1] =
    Chain.Concat(this, that)

  /**
   * Append a value to a Chain.
   *
   * (x :+ a) is equivalent to (x ++ Chain.single(a)).
   */
  def :+[A1 >: A](a: A1): Chain[A1] =
    Chain.Concat(this, Chain.Elems(a :: Nil))

  /**
   * Prepend a value to a Chain.
   *
   * (a +: x) is equivalent to (Chain.single(a) ++ x).
   */
  def +:[A1 >: A](a: A1): Chain[A1] =
    Chain.Concat(Chain.Elems(a :: Nil), this)

  /**
   * Return an iterator over the contents of this Chain.
   *
   * This is a fast O(1) operation, although traversing the iterator
   * is itself an O(n) operation (which also uses O(n) heap).
   */
  def iterator: Iterator[A] =
    this match {
      case Chain.Elems(values) => values.iterator
      case vec => new Chain.ChainIterator(vec)
    }

  /**
   * Compress a Chain, removing its internal structure.
   *
   * In general, this is an O(n) operation which will compact this
   * Chain into a single Chain.Elems instance wrapping a
   * vector. However, if the Chain is already compressed
   * (i.e. Chain.Elems) this does not do any work or allocate a new
   * Chain.
   *
   * When working with a large number of very small collections, this
   * method can have a big impact.
   */
  def compress: Chain[A] =
    this match {
      case Chain.Elems(_) => this
      case _ => Chain.Elems(iterator.toVector)
    }

  /**
   * Translate a Chain using the given function.
   *
   * This is an O(n) operation.
   *
   * The resulting Chain will naturally be compressed.
   */
  def map[B](f: A => B): Chain[B] =
    Chain.Elems(iterator.map(f).toVector)

  /**
   * Translate a Chain using the given function.
   *
   * This is an O(n * m) operation, where n is the length of this
   * Chain and m represents the average length of the Chain instances
   * produced by f.
   */
  def flatMap[B](f: A => Chain[B]): Chain[B] =
    Chain.Elems(iterator.flatMap(a => f(a).iterator).toVector)

  /**
   * Filter out some elements of a Chain given a predicate.
   *
   * This is an O(n) operation.
   *
   * The resulting Chain will naturally be compressed.
   */
  def filter(p: A => Boolean): Chain[A] =
    Chain.Elems(iterator.filter(p).toVector)

  /**
   * Combine the elements of a Chain into a single value, using a
   * starter value and an associative function.
   */
  def foldLeft[B](b: B)(f: (B, A) => B): B = {
    @tailrec def loop(b0: B, v: Chain[A], stack: List[Chain[A]]): B =
      v match {
        case Chain.Elems(values) =>
          val b1 = values.foldLeft(b0)(f)
          stack match {
            case h :: t => loop(b1, h, t)
            case Nil => b1
          }
        case Chain.Concat(lhs, rhs) =>
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
    @tailrec def loop(v: Chain[A], stack: List[Chain[A]]): Option[A] =
      v match {
        case Chain.Elems(values) =>
          values.find(p) match {
            case None =>
              stack match {
                case h :: t => loop(h, t)
                case Nil => None
              }
            case some =>
              some
          }
        case Chain.Concat(lhs, rhs) =>
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
   * Loop over this Chain, applying the given function.
   *
   * This is an O(n) operation.
   */
  def foreach(f: A => Unit): Unit =
    iterator.foreach(f)

  /**
   * Allow this Chain to be used where Iterable[A] is required.
   *
   * By default Chain does not extend Iterable[A], to avoid inheriting
   * inefficient methods from that API.
   *
   * This is a fast O(1) operation.
   */
  def toIterable: Iterable[A] =
    new Chain.IterableChain(this)

  /**
   * Conver this Chain to a Vector.
   *
   * This is an O(n) operation.
   */
  def toVector: Vector[A] =
    this match {
      case Chain.Elems(values) => values.toVector
      case _ => iterator.toVector
    }

  /**
   * Compare two Chain instances.
   */
  def compare[A1 >: A](that: Chain[A1])(implicit ev: Ordering[A1]): Int = {
    val it0 = this.iterator
    val it1 = that.iterator
    while (it0.hasNext && it1.hasNext) {
      val c = ev.compare(it0.next, it1.next)
      if (c != 0) return c
    }
    if (it0.hasNext) 1 else if (it1.hasNext) -1 else 0
  }

  /**
   * Produce a string representation of this Chain.
   *
   * This is an O(n) operation, which will display the entire contents
   * of the Chain.
   */
  override def toString: String =
    iterator.mkString("Chain(", ", ", ")")

  /**
   * Universal equality for Chain.
   *
   * In the worst-case this is an O(n) operation, but may be faster
   * due to type mistmatches or finding unequal elements early in the
   * Chains.
   */
  override def equals(that: Any): Boolean =
    that match {
      case that: Chain[_] =>
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
   * Hash codes for Chain.
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

object Chain {

  // a single collection of values
  case class Elems[A](values: Iterable[A]) extends Chain[A]

  // concatenations
  case class Concat[A](lhs: Chain[A], rhs: Chain[A]) extends Chain[A]

  // a shared "empty" vec instance
  val Empty: Chain[Nothing] = Elems(Nil)

  /**
   * Produce an empty Chain[A].
   */
  def empty[A]: Chain[A] = Empty

  /**
   * Wrap a single A value in a Chain[A].
   */
  def single[A](a: A): Chain[A] = Elems(a :: Nil)

  /**
   * Wrap a collection in a Chain[A].
   */
  def apply[A](values: Iterable[A]): Chain[A] = Elems(values)

  /**
   * Iterator for a Chain[A].
   *
   * This is where the magic happens. To efficiently traverse into
   * Chain.Concat instances, we build our own stack on the heap. When
   * we descend into the LHS of a Concat we push the RHS onto this
   * stack, so that when we exhaust the LHS we will remember to get an
   * iterator from the RHS too.
   *
   * As mentioned earlier, iterating over ChainIterator[A] is an O(n)
   * operation, that also consumes O(n) heap (the previously-mentioned
   * stack.)
   */
  class ChainIterator[A](vec: Chain[A]) extends Iterator[A] {
    var it: Iterator[A] = Iterator.empty
    var stack: List[Chain[A]] = Nil

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

    @tailrec private def descend(v: Chain[A]): Unit = {
      @tailrec def loop(v: Chain[A]): Boolean = {
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
   * Wrapper for Chain[A] that provides Iterable[A].
   */
  class IterableChain[+A](vec: Chain[A]) extends Iterable[A] {
    def iterator: Iterator[A] = vec.iterator
  }
}
