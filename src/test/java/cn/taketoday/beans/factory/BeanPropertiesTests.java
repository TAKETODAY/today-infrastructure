/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.support.BeanProperties;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    final BeanMappingTestBean bean = new BeanMappingTestBean();
    try {
      BeanProperties.copy(map, bean);
    }
    catch (final Throwable t) {
      fail("Threw " + t);
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
    final BeanMappingTestBean bean = new BeanMappingTestBean();

    // Set up an origin bean with customized properties
    final BeanMappingTestBean orig = new BeanMappingTestBean();
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
      fail("Threw exception: " + e);
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

  // populate

  /**
   * Test populate() method on individual array elements.
   */
  @Test
  public void testPopulateArrayElements() {

    final HashMap<String, Object> map = new HashMap<>();
    map.put("intIndexed[0]", "100");
    map.put("intIndexed[2]", "120");
    map.put("intIndexed[4]", "140");
    BeanMappingTestBean bean = new BeanMappingTestBean();

    BeanProperties.populate(bean, map);

    assertEquals("intIndexed[0] is 100",
            100, bean.getIntIndexed(0));
    assertEquals("intIndexed[1] is 10",
            10, bean.getIntIndexed(1));
    assertEquals("intIndexed[2] is 120",
            120, bean.getIntIndexed(2));
    assertEquals("intIndexed[3] is 30",
            30, bean.getIntIndexed(3));
    assertEquals("intIndexed[4] is 140",
            140, bean.getIntIndexed(4));

    map.clear();
    map.put("stringIndexed[1]", "New String 1");
    map.put("stringIndexed[3]", "New String 3");

    BeanProperties.populate(bean, map);

    assertEquals("stringIndexed[0] is \"String 0\"",
            "String 0", bean.getStringIndexed(0));
    assertEquals("stringIndexed[1] is \"New String 1\"",
            "New String 1", bean.getStringIndexed(1));
    assertEquals("stringIndexed[2] is \"String 2\"",
            "String 2", bean.getStringIndexed(2));
    assertEquals("stringIndexed[3] is \"New String 3\"",
            "New String 3", bean.getStringIndexed(3));
    assertEquals("stringIndexed[4] is \"String 4\"",
            "String 4", bean.getStringIndexed(4));

  }

  /**
   * Test populate() method on array properties as a whole.
   */
  @Test
  public void testPopulateArrayProperties() {
    BeanMappingTestBean bean = new BeanMappingTestBean();

    final HashMap<String, Object> map = new HashMap<>();
    int[] intArray = new int[] { 123, 456, 789 };
    map.put("intArray", intArray);
    String[] stringArray = new String[]
            { "New String 0", "New String 1" };
    map.put("stringArray", stringArray);

    BeanProperties.populate(bean, map);

    intArray = bean.getIntArray();
    assertNotNull("intArray is present", intArray);
    assertEquals("intArray length",
            3, intArray.length);
    assertEquals("intArray[0]", 123, intArray[0]);
    assertEquals("intArray[1]", 456, intArray[1]);
    assertEquals("intArray[2]", 789, intArray[2]);
    stringArray = bean.getStringArray();
    assertNotNull("stringArray is present", stringArray);
    assertEquals("stringArray length", 2, stringArray.length);
    assertEquals("stringArray[0]", "New String 0", stringArray[0]);
    assertEquals("stringArray[1]", "New String 1", stringArray[1]);

  }

  /**
   * Test populate() on mapped properties.
   */
  @Test
  public void testPopulateMapped() {
    BeanMappingTestBean bean = new BeanMappingTestBean();

    final HashMap<String, Object> map = new HashMap<>();
    map.put("mappedProperty[First Key])", "New First Value");
    map.put("mappedProperty[Third Key]", "New Third Value");

    BeanProperties.populate(bean, map);

    assertEquals("mappedProperty(First Key)",
            "New First Value",
            bean.getMappedProperty("First Key"));
    assertEquals("mappedProperty(Third Key)",
            "New Third Value",
            bean.getMappedProperty("Third Key"));
    assertNull("mappedProperty(Fourth Key",
            bean.getMappedProperty("Fourth Key"));

  }

  /**
   * Test populate() method on nested properties.
   */
  @Test
  public void testPopulateNested() {
    BeanMappingTestBean bean = new BeanMappingTestBean();

    final HashMap<String, Object> map = new HashMap<>();
    map.put("nested.booleanProperty", "false");
    // booleanSecond is left at true
    map.put("nested.doubleProperty", "432.0");
    // floatProperty is left at 123.0
    map.put("nested.intProperty", "543");
    // longProperty is left at 321
    map.put("nested.shortProperty", "654");
    // stringProperty is left at "This is a string"
    map.put("nested.writeOnlyProperty", "New writeOnlyProperty value");

    BeanProperties.populate(bean, map);

    assertFalse("booleanProperty is false", bean.getNested().getBooleanProperty());
    assertTrue("booleanSecond is true",
            bean.getNested().isBooleanSecond());
    assertEquals("doubleProperty is 432.0",
            432.0,
            bean.getNested().getDoubleProperty(),
            0.005);
    assertEquals("floatProperty is 123.0",
            (float) 123.0,
            bean.getNested().getFloatProperty(),
            (float) 0.005);
    assertEquals("intProperty is 543",
            543, bean.getNested().getIntProperty());
    assertEquals("longProperty is 321",
            321, bean.getNested().getLongProperty());
    assertEquals("shortProperty is 654",
            (short) 654, bean.getNested().getShortProperty());
    assertEquals("stringProperty is \"This is a string\"",
            "This is a string",
            bean.getNested().getStringProperty());
    assertEquals("writeOnlyProperty is \"New writeOnlyProperty value\"",
            "New writeOnlyProperty value",
            bean.getNested().getWriteOnlyPropertyValue());

  }

  /**
   * Test populate() method on scalar properties.
   */
  @Test
  public void testPopulateScalar() {
    BeanMappingTestBean bean = new BeanMappingTestBean();

    bean.setNullProperty("Non-null value");

    final HashMap<String, Object> map = new HashMap<>();
    map.put("booleanProperty", "false");
    // booleanSecond is left at true
    map.put("byteProperty", "111");
    map.put("doubleProperty", "432.0");
    // floatProperty is left at 123.0
    map.put("intProperty", "543");
    map.put("longProperty", ""); // null
    map.put("nullProperty", null);
    map.put("shortProperty", "654");
    // stringProperty is left at "This is a string"
    map.put("writeOnlyProperty", "New writeOnlyProperty value");
    map.put("readOnlyProperty", "New readOnlyProperty value");

    BeanProperties.populate(bean, map);

    assertFalse("booleanProperty is false", bean.getBooleanProperty());
    assertTrue("booleanSecond is true", bean.isBooleanSecond());
    assertEquals("byteProperty is 111",
            (byte) 111, bean.getByteProperty());
    assertEquals("doubleProperty is 432.0",
            432.0, bean.getDoubleProperty(),
            0.005);
    assertEquals("floatProperty is 123.0",
            (float) 123.0, bean.getFloatProperty(),
            (float) 0.005);
    assertEquals("intProperty is 543",
            543, bean.getIntProperty());
    assertEquals("longProperty is 0", 0, bean.getLongProperty());
    assertNull("nullProperty is null",
            bean.getNullProperty());
    assertEquals("shortProperty is 654",
            (short) 654, bean.getShortProperty());
    assertEquals("stringProperty is \"This is a string\"",
            "This is a string", bean.getStringProperty());
    assertEquals("writeOnlyProperty is \"New writeOnlyProperty value\"",
            "New writeOnlyProperty value",
            bean.getWriteOnlyPropertyValue());
    assertEquals("readOnlyProperty is \"Read Only String Property\"",
            "Read Only String Property",
            bean.getReadOnlyProperty());
  }

}
