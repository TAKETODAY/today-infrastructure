/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.core.bytecode.beans;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author baliuka
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class TestBulkBean {
  private String[] getters = {
          "getIntP", "getLongP", "getByteP", "getShortP",
          "getFloatP", "isBooleanP", "getCharP", "getDoubleP",
          "getStringP", "getId", "getName", "getPrivateName"
  };

  private String[] setters = {
          "setIntP", "setLongP", "setByteP", "setShortP",
          "setFloatP", "setBooleanP", "setCharP", "setDoubleP",
          "setStringP", "setId", "setName", "setPrivateName"
  };

  private Class[] types = {
          int.class, long.class, byte.class, short.class, float.class,
          boolean.class, char.class, double.class,
          String.class, Long.class, String.class, String.class
  };

  private final Object[] values = {
          2, 4L, (byte) 8,
          (short) 4, (float) 1.2, Boolean.TRUE,
          'S', 5.6,
          "test", 88L, "test2", "private"
  };

  @Test
  /** Test of create method, of class cn.taketoday.core.bytecode.BulkBean. */
  public void testGetInstance() throws Throwable {
    BulkBean mClass = BulkBean.create(MA.class, getters, setters, types);

    MA bean = new MA();

    mClass.setPropertyValues(bean, values);
    Object[] values1 = mClass.getPropertyValues(bean);

    for (int i = 0; i < types.length; i++) {
      assertEquals(values[i], values1[i], " property " + getters[i] + "/" + setters[i]);
    }
  }

  @Test
  public void testEmpty() throws Throwable {
    BulkBean.create(MA.class, new String[0], new String[0], new Class[0]);
  }

  @Test
  public void testBadTypes() throws Throwable {
    Class[] types2 = types.clone();
    types2[2] = String.class;
    try {
      BulkBean.create(MA.class, getters, setters, types2);
      fail("expected exception");
    }
    catch (BulkBeanException e) {
      assertEquals(2, e.getIndex());
    }
  }

  @Test
  public void testMismatchedLengths() throws Throwable {
    try {
      BulkBean.create(MA.class, getters, setters, new Class[0]);
      fail("expected exception");
    }
    catch (BulkBeanException e) {
      assertEquals(-1, e.getIndex());
    }
  }

  @Test
  public void testMissingProperty() throws Throwable {
    String[] getters2 = getters.clone();
    getters2[3] = "getChris";
    try {
      BulkBean.create(MA.class, getters2, setters, types);
      fail("expected exception");
    }
    catch (BulkBeanException e) {
      assertEquals(3, e.getIndex());
    }
  }

  @Test
  public void testSetWrongType() throws Throwable {
    BulkBean mClass = BulkBean.create(MA.class, getters, setters, types);
    MA bean = new MA();
    Object[] values2 = values.clone();
    values2[4] = new Object();
    try {
      mClass.setPropertyValues(bean, values2);
      fail("expected exception");
    }
    catch (BulkBeanException e) {
      assertEquals(4, e.getIndex());
    }
  }

  public void _testBulkBeanPerformance() throws Throwable {

    int iterations = 100000;

    System.out.println();
    System.out.println("iteration count: " + iterations);
    System.out.println();

    BulkBean mClass = new BulkBeanReflectImpl(MA.class, getters, setters, types);

    System.out.println(mClass.getClass().getName() + ": ");
    int b = performanceTest(mClass, iterations);
    System.out.println(b + " ms.   " + (b / (float) iterations) + " per iteration");
    System.out.println();

    mClass = BulkBean.create(MA.class, getters, setters, types);

    System.out.println(mClass.getClass().getName() + ": ");
    int a = performanceTest(mClass, iterations);
    System.out.println(a + " ms.   " + (a / (float) iterations) + " per iteration");

    System.out.println("factor: " + b / (float) a);

    mClass = new BulkBeanPlainImpl();

    System.out.println(mClass.getClass().getName() + ": ");
    a = performanceTest(mClass, iterations);
    System.out.println(a + " ms.   " + (a / (float) iterations) + " per iteration");
  }

  public int performanceTest(BulkBean mc, int iterations) throws Throwable {

    long start = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      MA bean = new MA(); // (MA)mc.newInstance();
      mc.setPropertyValues(bean, values);
      mc.getPropertyValues(bean, values);
    }

    return (int) (System.currentTimeMillis() - start);
  }

  private static class BulkBeanPlainImpl extends BulkBean {

    public void getPropertyValues(Object bean, Object[] values) {

      int i = 0;
      MA ma = (MA) bean;

      values[i++] = ma.getIntP();
      values[i++] = ma.getLongP();
      values[i++] = ma.getByteP();
      values[i++] = ma.getShortP();
      values[i++] = ma.getFloatP();
      values[i++] = ma.isBooleanP();
      values[i++] = ma.getCharP();
      values[i++] = ma.getDoubleP();
      values[i++] = ma.getStringP();
      values[i++] = ma.getId();
      values[i++] = ma.getName();
      values[i++] = ma.getPrivateName();

    }

    public void setPropertyValues(Object bean, Object[] values) {

      int i = 0;
      MA ma = (MA) bean;

      ma.setIntP(((Number) values[i++]).intValue());
      ma.setLongP(((Number) values[i++]).longValue());
      ma.setByteP(((Number) values[i++]).byteValue());
      ma.setShortP(((Number) values[i++]).shortValue());
      ma.setFloatP(((Number) values[i++]).floatValue());
      ma.setBooleanP(((Boolean) values[i++]).booleanValue());
      ma.setCharP(((Character) values[i++]).charValue());
      ma.setDoubleP(((Number) values[i++]).doubleValue());
      ma.setStringP((String) values[i++]);
      ma.setId((Long) values[i++]);
      ma.setName((String) values[i++]);
      ma.setPrivateName((String) values[i++]);

    }

  }

  /**
   * Generated implementation of abstract class
   * cn.taketoday.core.bytecode.BulkBean. Please fill dummy bodies of generated
   * methods.
   */
  private static class BulkBeanReflectImpl extends BulkBean {

    private Method gets[];
    private Method sets[];
    private int size;

    public BulkBeanReflectImpl(Class target, String[] getters, String[] setters, Class[] types) {
      this.target = target;
      this.types = types;
      this.getters = getters;
      this.setters = setters;

      size = this.types.length;
      gets = new Method[size];
      sets = new Method[size];

      try {

        for (int i = 0; i < size; i++) {

          if (getters[i] != null) {
            gets[i] = target.getDeclaredMethod(getters[i]);
            gets[i].setAccessible(true);
          }
          if (setters[i] != null) {
            sets[i] = target.getDeclaredMethod(setters[i], types[i]);
            sets[i].setAccessible(true);
          }

        }
      }
      catch (Exception e) {
        throw new Error(e.getClass().getName() + ":" + e.getMessage());
      }
    }

    public void getPropertyValues(Object bean, Object[] values) {

      try {
        for (int i = 0; i < size; i++) {
          if (this.gets[i] != null) {
            values[i] = gets[i].invoke(bean, (Object[]) null);
          }
        }
      }
      catch (Exception e) {
        throw new Error(e.getMessage());
      }
    }

    public void setPropertyValues(Object bean, Object[] values) {
      try {

        for (int i = 0; i < size; i++) {
          if (this.sets[i] != null) {
            sets[i].invoke(bean, values[i]);
          }
        }

      }
      catch (Exception e) {
        e.printStackTrace();
        throw new Error(e.getMessage());
      }
    }

  }

  // Add test methods here, they have to start with 'test' name.
  // for example:
  // public void testHello() {}

}
