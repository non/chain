## Chain

### Dedication

> all day long they work so hard / 'til the sun is going down /
> working on the highways and byways / and wearing, wearing a frown /
> you hear them moaning their lives away / then you hear somebody say: /
>
> that's the sound of the men / working on the chain gang /
> that's the sound of the men / working on the chain gang /
>
> --Sam Cooke, "Chain Gang" (1960)

### Overview

Lots of small collections got you down? Tired of paying O(n) to
concatenate lists, or generating a lot of garbage prepending to a
vector? If so, Chain is for you!

Chain is a small library that supports efficient concatenation across
many collection types, as well as efficient iteration across the
results.

```scala
import chain.Chain

val xs: Vector[Long] = ...
val ys: Vector[Long] = ...

// copies the entire contents of xs and ys
// before performing the summation
(xs ++ ys).foldLeft(0L)(_ + _)

// does not copy anything, just iterates over
// xs and ys in turn.
(Chain(xs) ++ Chain(ys)).foldLeft(0L)(_ + _)
```

This example is somewhat contrived, but I bet you have lots of code
that builds intermediate collections solely to iterate over
them. Chain can help make that code more efficient.

### Quick Start

Chain supports Scala 2.10, 2.11, and 2.12.

To include Chain in your projects, you can use the following
`build.sbt` snippet:

```scala
libraryDependencies += "org.spire-math" %% "chain" % "0.2.0"
```

Chain also supports Scala.js. To use Chain in your Scala.js projects,
include the following `build.sbt` snippet:

```scala
libraryDependencies += "org.spire-math" %%% "chain" % "0.2.0"
```

### Details

Chain can wrap any `Iterable[A]` values, and supports concatenation
between mixed collection types. Here's an example that shows off a
number of Chain's capabilities:

```scala
import chain.Chain

val ws: Iterable[Int] = List(1,2,3)
val xs: List[Int] = List(4,5,6)
val ys: Vector[Int] = Vector(7,8,9,10,11)
val zs: Option[Int] = Some(12)

val a = Chain(ws) ++ Chain(xs) // no copying
val b = Chain.all(ys, zs)      // same as ys ++ zs
val c = a ++ b                 // still no copying
val d = 9 +: c :+ 100          // supports prepend/append

c.toVector           // Vector(1,2,3,4,5,6,7,8,9,10,11,12)
c.iterator.toList    // List(1,2,3,4,5,6,7,8,9,10,11,12)
c.foreach(println)   // prints 1-12
c.find(_ > 6)        // Some(7)
c.forall(_ >= 0)     // true
c.exists(_ > 100)    // false
c.map(_ * 2)         // Chain(2,4,6,8,10,12,14,16,18,20,22,24)
c.filter(_ % 3 == 0) // Chain(3,6,9,12)
```

(Note that `.toString` evaluates the entire contents of the Chain, so
displaying a chain value in the REPL will force iteration over the
contents of the chain.)

Chain is sealed and consists of two concrete case classes:

 * `Chain.Elems` wraps a single collection.
 * `Chain.Concat` represents a single `++` invocation.

Together these types create a tree. (Since we do not need to support
arbitrary insertion into the tree, there is no need to balance it.)
Iteration over the tree takes advantage of an in-memory stack to
efficiently walk the contents in O(n) time.

Concatenating chains is always O(1), and iteration is always O(n).

Empty Chains can be obtained by `Chain.empty[A]` and are represented
as a singleton `Chain.Empty` which is a `Chain(Nil)`. This value is
immutable and can be shared safely. Chains with a single element are
constructed by `Chain.single(x)` which constructs `Chain(x :: Nil)`
instances. This is done transparently in the case of `+:` and `:+`.
These encoding are relatively efficient although if you are working
entirely with single elements a more efficient data structure is
possible.

Some operations that transform the Chain will need to allocate a new
collection (either directly, or wrapped in a new `Chain[A]`). The
comments explain the exact performance characteristics of each method,
but here is a quick list of the methods which will allocate a new
collection:

 * `.map`: always allocates a new collection.
 * `.flatMap`: always allocates a new collection.
 * `.filter`: always allocates a new collection.
 * `.compress`: when not already compressed, allocates a new collection.
 * `.toVector`: usually allocates a new `Vector[A]`.

(If your chain is a `Chain.Elems` wrapping a `Vector[A]`, then
`.toVector` just return that vector directly.)

### Caveats

To avoid inheriting inefficient methods (such as `.size`), `Chain`
itself does not extend any Scala collection interfaces. However
`.toIterable` uses a very thin wrapper to support `Iterable[A]`, so if
you need to interoperate with the Scala collections hierarchy you can
use that method.

Currently Chain supports the most commonly-used collection
methods. Most of the rest should be easy to implement using
`.iterator`, `.foldLeft`, or .`find`. Pull requests to add more of
these methods will be gladly accepted.

The design of Chain assumes that the (relatively small) overhead of
using `Iterator[A]` internally is acceptable. In the case of a large
number of very small (or empty) collections this could be less
efficient than simply accumulating those values into a single
collection. The `.compress` method can be used in these situations.

Chain can be thought of as a limited kind of rope that is specialized
to Scala collections (specifically `Iterable[A]`). You can imagine a
similar (but more principled) data structure that is based around a
type class like `Foldable` instead.

### Future Work

Additional benchmarking and profiling would be great. Almost any chain
method implemented with `.iterator` could be specialized if it proved
to be a hotspot.

It might be nice to have a few different types to support various
expected work loads and collection shapes. The current approach leans
towards supporting large collections.

As mentioned above, it would be great to have a story for using type
classes instead of `Iterable[A]` (either via an abstraction or a new
type). It could also be nice to have a version which supported lazy
filtering/mapping (although in many cases this can be emulated with
things like `.iterator.filter`).

### Copyright and License

All code is available to you under the MIT license, available at
http://opensource.org/licenses/mit-license.php.

Copyright Erik Osheim, 2016-2017.
