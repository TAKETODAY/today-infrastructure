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

import cn.taketoday.bytecode.core.CodeGenerationException;
import cn.taketoday.bytecode.transform.AbstractTransformTest;
import cn.taketoday.bytecode.transform.ClassTransformer;
import cn.taketoday.bytecode.transform.ClassTransformerFactory;

/**
 * @author baliuka
 */
public class AddClassInitTests extends AbstractTransformTest {

  static Class<?> registred;

  static int i = 0;

  static {

    i = 11;

  }

  public static void register(Class<?> cls) {

    registred = cls;

  }

  public AddClassInitTests() { }

  public void testInitTransform() {
    assertEquals(i, 11);
  }

  public void testRegistred() {

    assertNotNull(registred);

  }

  public AddClassInitTests(String s) {
    super(s);
  }

  protected ClassTransformerFactory getTransformer() throws Exception {

    return new ClassTransformerFactory() {

      public ClassTransformer newTransformer() {
        try {
          return new AddStaticInitTransformer(AddClassInitTests.class.getMethod("register", Class.class));
        }
        catch (Exception e) {
          throw new CodeGenerationException(e);
        }
      }
    };

  }

  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite() throws Exception {
    return new TestSuite(new AddClassInitTests().transform());
  }

}
