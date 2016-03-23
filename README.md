## Chain

### Dedication

> all day long they work so hard / 'til the sun is going down
> working on the highways and byways / and wearing, wearing a frown
> you hear them moaning their lives away / then you hear somebody say:
>
> that's the sound of the men / working on the chain gang
> that's the sound of the men / working on the chain gang
>
> --Sam Cooke, "Chain Gang" (1960)

### Overview

Lots of small collections got you down? Tired of paying O(n) to
concatenate lists, or generating a lot of garbage prepending to a
vector? If so, chain is for you!

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

Chain supports Scala 2.10 and 2.11.

To include Chain in your projects, you can use the following
`build.sbt` snippet:

```scala
libraryDependencies += "org.spire-math" %% "chain" % "0.1.0"
```

Chain also supports Scala.js. To use Chain in your Scala.js projects,
include the following `build.sbt` snippet:

```scala
libraryDependencies += "org.spire-math" %%% "chain" % "0.1.0"
```

### Details

Chain can wrap any `Iterable[A]` values, and support concatenation
between mixed collection types:

```scala
import chain.Chain

val xs: List[Int] = ...
val ys: Vector[Int] = ...
val zs: Option[Int] = ...

val c = Chain(xs) ++ Chain(ys) ++ Chain(zs)
```

Chain is sealed and consists of two concrete case classes:

 * `Chain.Elems` wraps a single collection.
 * `Chain.Concat` represents a single `++` invocation.

Together these types create a tree. (Since we do not need to support
arbitrary insertion into the tree, there is no need to balance it.)
Iteration over the tree takes advantage of an in-memory stack to
efficiently walk the contents in O(n) time.

Concatenating chains is always O(1), and iteration is always O(n).

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
method implemented with `.iterator` could be specialized if it provded
to be a hotspot.

It might be nice to have a few different types to support various
expected work loads and collection shapes. The current approach leans
towards supporting large collections.

As mentioned above, it would be great to have a story for using type
classes instead of `Iterable[A]` (either via an abstraction or a new
type).

### Copyright and License

All code is available to you under the MIT license, available at
http://opensource.org/licenses/mit-license.php.

Copyright Erik Osheim, 2016.
