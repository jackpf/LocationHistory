package com.jackpf.locationhistory.server.testutil

import org.specs2.mutable.Specification
import org.specs2.specification.{After, Before}

abstract class DefaultSpecification extends Specification {

  /** A little helper for test syntax
    * Allows us to write tests like
    *
    *  trait MyContext extends DefaultScope {
    *    //...
    *  }
    *
    *  "my test" >> in(new MyContext {}) { context =>
    *    //...
    *  }
    */
  protected def in[C <: DefaultScope, T](c: C)(f: C => T): T = {
    if (c.isInstanceOf[Before]) c.asInstanceOf[Before].before: Unit
    val r = f(c)
    if (c.isInstanceOf[After]) c.asInstanceOf[After].after: Unit
    r
  }
}

trait DefaultScope
