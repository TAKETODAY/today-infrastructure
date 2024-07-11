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

/**
 * @author Juozas
 */
public class InterceptFieldsTestsSubclassTests extends InterceptFieldsTests {

  private boolean readTest = false;
  private boolean writeTest = false;

  public InterceptFieldsTestsSubclassTests() {
    super();

  }

  public InterceptFieldsTestsSubclassTests(String name) {
    super(name);

  }

  public void testSubClass() {
    super.test();
    assertTrue("super class read field", readTest);
    assertTrue("super class write field", readTest);
  }

  public Object readObject(Object _this, String name, Object oldValue) {
    if (name.equals("field")) {
      readTest = true;
    }
    return super.readObject(_this, name, oldValue);
  }

  public Object writeObject(Object _this, String name, Object oldValue,
          Object newValue) {

    if (name.equals("field")) {
      writeTest = true;
    }

    return super.writeObject(_this, name, oldValue, newValue);
  }

  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite() throws Exception {
    return new TestSuite(new InterceptFieldsTestsSubclassTests().transform());
  }
}
