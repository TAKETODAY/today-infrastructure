/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.factory;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author TODAY 2021/5/2 22:18
 */
public class BeanPropertiesTests {

  @Data
  static class VO {
    int age;
    float aFloat;
    String name;
    double aDouble;
    String missed;
  }

  @Data
  static class DTO {
    int age;
    float aFloat;
    double aDouble;
    String name;
  }

  @Test
  public void test() {
    final VO vo = new VO();
    vo.age = 10;
    vo.aDouble = 10.1D;
    vo.aFloat = 10.2f;
    vo.name = "TODAY";
    vo.missed = "missed";

    final DTO dto = new DTO();
    BeanProperties.copy(vo, dto);

    assertThat(dto).isNotNull();
    assertThat(dto.age).isEqualTo(vo.age);
    assertThat(dto.aDouble).isEqualTo(vo.aDouble);
    assertThat(dto.aFloat).isEqualTo(vo.aFloat);
    assertThat(dto.name).isNotNull().isEqualTo(vo.name);

    //

    final DTO copy = BeanProperties.copy(vo, DTO.class);

    assertThat(copy).isNotNull();
    assertThat(copy.age).isEqualTo(vo.age);
    assertThat(copy.aDouble).isEqualTo(vo.aDouble);
    assertThat(copy.aFloat).isEqualTo(vo.aFloat);
    assertThat(copy.name).isNotNull().isEqualTo(vo.name);
  }

  @Test
  public void ignoreProperties() {
    final VO vo = new VO();
    vo.age = 10;
    vo.aDouble = 10.1D;
    vo.aFloat = 10.2f;
    vo.name = "TODAY";
    vo.missed = "missed";

    final DTO dto = new DTO();
    BeanProperties.copy(vo, dto, "name");

    assertThat(dto).isNotNull();
    assertThat(dto.name).isNull();

    final DTO copy = BeanProperties.copy(vo, DTO.class, "name");

    assertThat(copy).isNotNull();
    assertThat(copy.age).isEqualTo(vo.age);
    assertThat(copy.aDouble).isEqualTo(vo.aDouble);
    assertThat(copy.aFloat).isEqualTo(vo.aFloat);
    assertThat(copy.name).isNull();

  }

  /**
   * Test copy() when the origin is a a {@code Map}.
   */
  @Test
  public void testCopyPropertiesMap() {

    final Map<String, Object> map = new HashMap<>();
    map.put("booleanProperty", "false");
    map.put("byteProperty", "111");
    map.put("doubleProperty", "333.0");
    map.put("dupProperty", new String[] { "New 0", "New 1", "New 2" });
    map.put("floatProperty", "222.0");
    map.put("intArray", new String[] { "0", "100", "200" });
    map.put("intProperty", "111");
    map.put("longProperty", "444");
    map.put("shortProperty", "555");
    map.put("stringProperty", "New String Property");
    final TestBean bean = new TestBean();
    try {
      BeanProperties.copy(map, bean);
    }
    catch (final Throwable t) {
      Assert.fail("Threw " + t);
    }

    // Scalar properties
    assertFalse("booleanProperty", bean.getBooleanProperty());
    assertEquals("byteProperty", (byte) 111,
                 bean.getByteProperty());
    assertEquals("doubleProperty", 333.0,
                 bean.getDoubleProperty(), 0.005);
    assertEquals("floatProperty", (float) 222.0,
                 bean.getFloatProperty(), (float) 0.005);
    assertEquals("longProperty", 111,
                 bean.getIntProperty());
    assertEquals("longProperty", 444,
                 bean.getLongProperty());
    assertEquals("shortProperty", (short) 555,
                 bean.getShortProperty());
    assertEquals("stringProperty", "New String Property",
                 bean.getStringProperty());

    // Indexed Properties
    final String[] dupProperty = bean.getDupProperty();
    assertNotNull("dupProperty present", dupProperty);
    assertEquals("dupProperty length", 3, dupProperty.length);
    assertEquals("dupProperty[0]", "New 0", dupProperty[0]);
    assertEquals("dupProperty[1]", "New 1", dupProperty[1]);
    assertEquals("dupProperty[2]", "New 2", dupProperty[2]);
    final int[] intArray = bean.getIntArray();
    assertNotNull("intArray present", intArray);
    assertEquals("intArray length", 3, intArray.length);
    assertEquals("intArray[0]", 0, intArray[0]);
    assertEquals("intArray[1]", 100, intArray[1]);
    assertEquals("intArray[2]", 200, intArray[2]);

  }

  /**
   * Test the copy() method from a standard JavaBean.
   */
  @Test
  public void testCopyPropertiesStandard() {
    final TestBean bean = new TestBean();

    // Set up an origin bean with customized properties
    final TestBean orig = new TestBean();
    orig.setBooleanProperty(false);
    orig.setByteProperty((byte) 111);
    orig.setDoubleProperty(333.33);
    orig.setDupProperty(new String[] { "New 0", "New 1", "New 2" });
    orig.setIntArray(new int[] { 100, 200, 300 });
    orig.setIntProperty(333);
    orig.setLongProperty(3333);
    orig.setShortProperty((short) 33);
    orig.setStringArray(new String[] { "New 0", "New 1" });
    orig.setStringProperty("Custom string");

    // Copy the origin bean to our destination test bean
    try {
      BeanProperties.copy(orig, bean);
    }
    catch (final Exception e) {
      Assert.fail("Threw exception: " + e);
    }

    // Validate the results for scalar properties
    assertFalse("Copied boolean property", bean.getBooleanProperty());
    assertEquals("Copied byte property", (byte) 111, bean.getByteProperty());
    assertEquals("Copied double property",
                 333.33,
                 bean.getDoubleProperty(),
                 0.005);
    assertEquals("Copied int property",
                 333,
                 bean.getIntProperty());
    assertEquals("Copied long property",
                 3333,
                 bean.getLongProperty());
    assertEquals("Copied short property",
                 (short) 33,
                 bean.getShortProperty());
    assertEquals("Copied string property",
                 "Custom string",
                 bean.getStringProperty());

    // Validate the results for array properties
    final String[] dupProperty = bean.getDupProperty();
    assertNotNull("dupProperty present", dupProperty);
    assertEquals("dupProperty length", 3, dupProperty.length);
    assertEquals("dupProperty[0]", "New 0", dupProperty[0]);
    assertEquals("dupProperty[1]", "New 1", dupProperty[1]);
    assertEquals("dupProperty[2]", "New 2", dupProperty[2]);
    final int[] intArray = bean.getIntArray();
    assertNotNull("intArray present", intArray);
    assertEquals("intArray length", 3, intArray.length);
    assertEquals("intArray[0]", 100, intArray[0]);
    assertEquals("intArray[1]", 200, intArray[1]);
    assertEquals("intArray[2]", 300, intArray[2]);
    final String[] stringArray = bean.getStringArray();
    assertNotNull("stringArray present", stringArray);
    assertEquals("stringArray length", 2, stringArray.length);
    assertEquals("stringArray[0]", "New 0", stringArray[0]);
    assertEquals("stringArray[1]", "New 1", stringArray[1]);

  }

}
