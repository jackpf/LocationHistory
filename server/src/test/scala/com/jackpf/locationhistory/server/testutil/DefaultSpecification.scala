package com.jackpf.locationhistory.server.testutil

import org.specs2.mutable.Specification

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
  protected def in[C <: DefaultScope, T](c: C)(f: C => T): T = f(c)
}

trait DefaultScope
