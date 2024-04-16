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

package cn.taketoday.reflect;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import cn.taketoday.util.ObjectUtils;

import static cn.taketoday.reflect.ReadOnlyPropertyAccessor.classToString;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/4/16 14:07
 */
class PropertyAccessorTests implements WithAssertions {

  @Nested
  class PublicProperty {

    @Test
    void publicAccessor() throws Exception {
      PropertyAccessor name = PropertyAccessor.forField(BeanTest.class.getDeclaredField("name"));

      BeanTest beanTest = new BeanTest();
      name.set(beanTest, "name");

      assertThat(beanTest.name).isEqualTo("name");
      assertThat(beanTest.name).isEqualTo(name.get(beanTest));
    }

    @Test
    void primitiveBean() throws Exception {
      primitiveBeanTest("intValue", 31234);
      primitiveBeanTest("longValue", 41L);
      primitiveBeanTest("byteValue", (byte) 11);
      primitiveBeanTest("shortValue", (short) 31);
      primitiveBeanTest("booleanValue", true);
      primitiveBeanTest("booleanValue", false);
      primitiveBeanTest("doubleValue", 1143.89);
      primitiveBeanTest("floatValue", 112.4f);
      primitiveBeanTest("charValue", 'c');
    }

    @Test
    void readOnlyBean() throws Exception {
      readOnlyTest("byteValue", (byte) 1);
      readOnlyTest("shortValue", (short) 2);
      readOnlyTest("intValue", 3);
      readOnlyTest("longValue", 4L);
      readOnlyTest("booleanValue", true);
      readOnlyTest("doubleValue", 6.0);
      readOnlyTest("floatValue", 7f);
      readOnlyTest("charValue", '8');
      readOnlyTest("stringValue", "9");
    }

    <T> void readOnlyTest(String name, T actual) throws Exception {
      ReadOnlyBean bean = new ReadOnlyBean();

      PropertyAccessor accessor = PropertyAccessor.forField(ReadOnlyBean.class.getDeclaredField(name));
      assertThatThrownBy(() -> accessor.set(bean, actual))
              .isInstanceOf(ReflectionException.class)
              .hasMessage("Can't set value '%s' to '%s' read only property".formatted(ObjectUtils.toHexString(actual), classToString(bean)));

      assertThat(actual).isEqualTo(accessor.get(bean));
    }

    <T> void primitiveBeanTest(String name, T actual) throws Exception {
      PrimitiveBean bean = new PrimitiveBean();

      PropertyAccessor accessor = PropertyAccessor.forField(PrimitiveBean.class.getDeclaredField(name));
      accessor.set(bean, actual);
      assertThat(actual).isEqualTo(accessor.get(bean));
    }
  }

  static class BeanTest {
    public String name;
  }

  static class PrimitiveBean {
    public byte byteValue;

    public short shortValue;

    public int intValue;

    public long longValue;

    public boolean booleanValue;

    public double doubleValue;

    public float floatValue;

    public char charValue;
  }

  static class ReadOnlyBean {
    public final byte byteValue = 1;

    public final short shortValue = 2;

    public final int intValue = 3;

    public final long longValue = 4;

    public final boolean booleanValue = true;

    public final double doubleValue = 6;

    public final float floatValue = 7;

    public final char charValue = '8';

    public final String stringValue = "9";

  }

}