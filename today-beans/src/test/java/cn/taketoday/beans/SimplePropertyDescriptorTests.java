/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans;

import org.junit.jupiter.api.Test;

import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Chris Beams
 * @see ExtendedBeanInfoTests
 */
public class SimplePropertyDescriptorTests {

  @Test
  public void toStringOutput() throws IntrospectionException, SecurityException, NoSuchMethodException {
    {
      Object pd = new ExtendedBeanInfo.SimplePropertyDescriptor("foo", null, null);
      assertThat(pd.toString()).contains(
              "PropertyDescriptor[name=foo, propertyType=null, readMethod=null");
    }
    {
      class C {
        @SuppressWarnings("unused")
        public Object setFoo(String foo) { return null; }
      }
      Method m = C.class.getMethod("setFoo", String.class);
      Object pd = new ExtendedBeanInfo.SimplePropertyDescriptor("foo", null, m);
      assertThat(pd.toString()).contains(
              "PropertyDescriptor[name=foo",
              "propertyType=class java.lang.String",
              "readMethod=null, writeMethod=public java.lang.Object");
    }
    {
      Object pd = new ExtendedBeanInfo.SimpleIndexedPropertyDescriptor("foo", null, null, null, null);
      assertThat(pd.toString()).contains(
              "PropertyDescriptor[name=foo, propertyType=null, indexedPropertyType=null");
    }
    {
      class C {
        @SuppressWarnings("unused")
        public Object setFoo(int i, String foo) { return null; }
      }
      Method m = C.class.getMethod("setFoo", int.class, String.class);
      Object pd = new ExtendedBeanInfo.SimpleIndexedPropertyDescriptor("foo", null, null, null, m);
      assertThat(pd.toString()).contains(
              "PropertyDescriptor[name=foo, propertyType=null",
              "indexedPropertyType=class java.lang.String",
              "indexedWriteMethod=public java.lang.Object");
    }
  }

  @Test
  public void nonIndexedEquality() throws IntrospectionException, SecurityException, NoSuchMethodException {
    Object pd1 = new ExtendedBeanInfo.SimplePropertyDescriptor("foo", null, null);
    assertThat(pd1).isEqualTo(pd1);

    Object pd2 = new ExtendedBeanInfo.SimplePropertyDescriptor("foo", null, null);
    assertThat(pd1).isEqualTo(pd2);
    assertThat(pd2).isEqualTo(pd1);

    @SuppressWarnings("unused")
    class C {
      public Object setFoo(String foo) { return null; }

      public String getFoo() { return null; }
    }
    Method wm1 = C.class.getMethod("setFoo", String.class);
    Object pd3 = new ExtendedBeanInfo.SimplePropertyDescriptor("foo", null, wm1);
    assertThat(pd1).isNotEqualTo(pd3);
    assertThat(pd3).isNotEqualTo(pd1);

    Method rm1 = C.class.getMethod("getFoo");
    Object pd4 = new ExtendedBeanInfo.SimplePropertyDescriptor("foo", rm1, null);
    assertThat(pd1).isNotEqualTo(pd4);
    assertThat(pd4).isNotEqualTo(pd1);

    Object pd5 = new PropertyDescriptor("foo", null, null);
    assertThat(pd1).isEqualTo(pd5);
    assertThat(pd5).isEqualTo(pd1);

    Object pd6 = "not a PD";
    assertThat(pd1).isNotEqualTo(pd6);
    assertThat(pd6).isNotEqualTo(pd1);

    Object pd7 = null;
    assertThat(pd1).isNotEqualTo(pd7);
    assertThat(pd7).isNotEqualTo(pd1);
  }

  @Test
  public void indexedEquality() throws IntrospectionException, SecurityException, NoSuchMethodException {
    Object pd1 = new ExtendedBeanInfo.SimpleIndexedPropertyDescriptor("foo", null, null, null, null);
    assertThat(pd1).isEqualTo(pd1);

    Object pd2 = new ExtendedBeanInfo.SimpleIndexedPropertyDescriptor("foo", null, null, null, null);
    assertThat(pd1).isEqualTo(pd2);
    assertThat(pd2).isEqualTo(pd1);

    @SuppressWarnings("unused")
    class C {
      public Object setFoo(int i, String foo) { return null; }

      public String getFoo(int i) { return null; }
    }
    Method wm1 = C.class.getMethod("setFoo", int.class, String.class);
    Object pd3 = new ExtendedBeanInfo.SimpleIndexedPropertyDescriptor("foo", null, null, null, wm1);
    assertThat(pd1).isNotEqualTo(pd3);
    assertThat(pd3).isNotEqualTo(pd1);

    Method rm1 = C.class.getMethod("getFoo", int.class);
    Object pd4 = new ExtendedBeanInfo.SimpleIndexedPropertyDescriptor("foo", null, null, rm1, null);
    assertThat(pd1).isNotEqualTo(pd4);
    assertThat(pd4).isNotEqualTo(pd1);

    Object pd5 = new IndexedPropertyDescriptor("foo", null, null, null, null);
    assertThat(pd1).isEqualTo(pd5);
    assertThat(pd5).isEqualTo(pd1);

    Object pd6 = "not a PD";
    assertThat(pd1).isNotEqualTo(pd6);
    assertThat(pd6).isNotEqualTo(pd1);

    Object pd7 = null;
    assertThat(pd1).isNotEqualTo(pd7);
    assertThat(pd7).isNotEqualTo(pd1);
  }

}
