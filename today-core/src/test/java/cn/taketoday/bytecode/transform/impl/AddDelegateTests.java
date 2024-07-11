/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package cn.taketoday.bytecode.transform.impl;

import junit.framework.Test;
import junit.framework.TestSuite;

import cn.taketoday.bytecode.transform.AbstractTransformTest;
import cn.taketoday.bytecode.transform.ClassTransformer;
import cn.taketoday.bytecode.transform.ClassTransformerFactory;

/**
 * @author baliuka
 */
public class AddDelegateTests extends AbstractTransformTest {

  /** Creates a new instance of TestAddDelegate */
  public AddDelegateTests(String name) {
    super(name);
  }

  public interface Interface {

    Object getDelegte();

    Object getTarget();

  }

  public void test() {

    Interface i = (Interface) this;
    assertEquals(i.getTarget(), this);

  }

  public static class ImplExclude implements Interface {

    private Object target;

    public ImplExclude(Object target) {
      this.target = target;
    }

    public Object getDelegte() {
      return this;
    }

    public Object getTarget() {
      return target;
    }
  }

  public AddDelegateTests() {
    super(null);
  }

  protected ClassTransformerFactory getTransformer() throws Exception {

    return new ClassTransformerFactory() {

      public ClassTransformer newTransformer() {

        return new AddDelegateTransformer(new Class[] { Interface.class }, ImplExclude.class);

      }

    };

  }

  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite() throws Exception {

    return new TestSuite(new AddDelegateTests().transform());

  }

}
