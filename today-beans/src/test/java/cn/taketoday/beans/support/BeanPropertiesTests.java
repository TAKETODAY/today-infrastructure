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

package cn.taketoday.beans.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.factory.BeanMappingTestBean;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

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
    assertFalse(bean.getBooleanProperty(), "booleanProperty");
    assertEquals((byte) 111, bean.getByteProperty(), "byteProperty");
    assertEquals(333.0, bean.getDoubleProperty(), 0.005, "doubleProperty");
    assertEquals((float) 222.0, bean.getFloatProperty(), (float) 0.005, "floatProperty");
    assertEquals(111, bean.getIntProperty(), "longProperty");
    assertEquals(444, bean.getLongProperty(), "longProperty");
    assertEquals((short) 555, bean.getShortProperty(), "shortProperty");
    assertEquals("New String Property", bean.getStringProperty(), "stringProperty");

    // Indexed Properties
    final String[] dupProperty = bean.getDupProperty();
    assertNotNull(dupProperty, "dupProperty present");
    assertEquals(3, dupProperty.length, "dupProperty length");
    assertEquals("New 0", dupProperty[0], "dupProperty[0]");
    assertEquals("New 1", dupProperty[1], "dupProperty[1]");
    assertEquals("New 2", dupProperty[2], "dupProperty[2]");
    final int[] intArray = bean.getIntArray();
    assertNotNull(intArray, "intArray present");
    assertEquals(3, intArray.length, "intArray length");
    assertEquals(0, intArray[0], "intArray[0]");
    assertEquals(100, intArray[1], "intArray[1]");
    assertEquals(200, intArray[2], "intArray[2]");

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
    assertFalse(bean.getBooleanProperty(), "Copied boolean property");
    assertEquals((byte) 111, bean.getByteProperty(), "Copied byte property");
    assertEquals(333.33, bean.getDoubleProperty(), 0.005, "Copied double property");
    assertEquals(333, bean.getIntProperty(), "Copied int property");
    assertEquals(3333, bean.getLongProperty(), "Copied long property");
    assertEquals((short) 33, bean.getShortProperty(), "Copied short property");
    assertEquals("Custom string", bean.getStringProperty(), "Copied string property");

    // Validate the results for array properties
    final String[] dupProperty = bean.getDupProperty();
    assertNotNull(dupProperty, "dupProperty present");
    assertEquals(3, dupProperty.length, "dupProperty length");
    assertEquals("New 0", dupProperty[0], "dupProperty[0]");
    assertEquals("New 1", dupProperty[1], "dupProperty[1]");
    assertEquals("New 2", dupProperty[2], "dupProperty[2]");
    final int[] intArray = bean.getIntArray();
    assertNotNull(intArray, "intArray present");
    assertEquals(3, intArray.length, "intArray length");
    assertEquals(100, intArray[0], "intArray[0]");
    assertEquals(200, intArray[1], "intArray[1]");
    assertEquals(300, intArray[2], "intArray[2]");
    final String[] stringArray = bean.getStringArray();
    assertNotNull(stringArray, "stringArray present");
    assertEquals(2, stringArray.length, "stringArray length");
    assertEquals("New 0", stringArray[0], "stringArray[0]");
    assertEquals("New 1", stringArray[1], "stringArray[1]");

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

    assertEquals(100, bean.getIntIndexed(0), "intIndexed[0] is 100");
    assertEquals(10, bean.getIntIndexed(1), "intIndexed[1] is 10");
    assertEquals(120, bean.getIntIndexed(2), "intIndexed[2] is 120");
    assertEquals(30, bean.getIntIndexed(3), "intIndexed[3] is 30");
    assertEquals(140, bean.getIntIndexed(4), "intIndexed[4] is 140");

    map.clear();
    map.put("stringIndexed[1]", "New String 1");
    map.put("stringIndexed[3]", "New String 3");

    BeanProperties.populate(bean, map);

    assertEquals("String 0", bean.getStringIndexed(0), "stringIndexed[0] is \"String 0\"");
    assertEquals("New String 1", bean.getStringIndexed(1), "stringIndexed[1] is \"New String 1\"");
    assertEquals("String 2", bean.getStringIndexed(2), "stringIndexed[2] is \"String 2\"");
    assertEquals("New String 3", bean.getStringIndexed(3), "stringIndexed[3] is \"New String 3\"");
    assertEquals("String 4", bean.getStringIndexed(4), "stringIndexed[4] is \"String 4\"");

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
    assertNotNull(intArray, "intArray is present");
    assertEquals(3, intArray.length, "intArray length");
    assertEquals(123, intArray[0], "intArray[0]");
    assertEquals(456, intArray[1], "intArray[1]");
    assertEquals(789, intArray[2], "intArray[2]");
    stringArray = bean.getStringArray();
    assertNotNull(stringArray, "stringArray is present");
    assertEquals(2, stringArray.length, "stringArray length");
    assertEquals("New String 0", stringArray[0], "stringArray[0]");
    assertEquals("New String 1", stringArray[1], "stringArray[1]");
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

    assertEquals("New First Value", bean.getMappedProperty("First Key"), "mappedProperty(First Key)");
    assertEquals("New Third Value", bean.getMappedProperty("Third Key"), "mappedProperty(Third Key)");
    Assertions.assertNull(bean.getMappedProperty("Fourth Key"), "mappedProperty(Fourth Key");

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

    assertFalse(bean.getNested().getBooleanProperty(), "booleanProperty is false");
    Assertions.assertTrue(bean.getNested().isBooleanSecond(), "booleanSecond is true");
    assertEquals(432.0, bean.getNested().getDoubleProperty(), 0.005, "doubleProperty is 432.0");
    assertEquals((float) 123.0, bean.getNested().getFloatProperty(), (float) 0.005, "floatProperty is 123.0");
    assertEquals(543, bean.getNested().getIntProperty(), "intProperty is 543");
    assertEquals(321, bean.getNested().getLongProperty(), "longProperty is 321");
    assertEquals((short) 654, bean.getNested().getShortProperty(), "shortProperty is 654");
    assertEquals("This is a string", bean.getNested().getStringProperty(), "stringProperty is \"This is a string\"");
    assertEquals("New writeOnlyProperty value", bean.getNested().getWriteOnlyPropertyValue(), "writeOnlyProperty is \"New writeOnlyProperty value\"");
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
//    map.put("longProperty", ""); // null
    map.put("nullProperty", null);
    map.put("shortProperty", "654");
    // stringProperty is left at "This is a string"
    map.put("writeOnlyProperty", "New writeOnlyProperty value");
    map.put("readOnlyProperty", "New readOnlyProperty value");

    BeanProperties.populate(bean, map);

    assertFalse(bean.getBooleanProperty(), "booleanProperty is false");
    Assertions.assertTrue(bean.isBooleanSecond(), "booleanSecond is true");
    assertEquals((byte) 111, bean.getByteProperty(), "byteProperty is 111");
    assertEquals(432.0, bean.getDoubleProperty(), 0.005, "doubleProperty is 432.0");
    assertEquals((float) 123.0, bean.getFloatProperty(), (float) 0.005, "floatProperty is 123.0");
    assertEquals(543, bean.getIntProperty(), "intProperty is 543");
//    assertEquals("longProperty is 0", 0, bean.getLongProperty());
    Assertions.assertNull(bean.getNullProperty(), "nullProperty is null");
    assertEquals((short) 654, bean.getShortProperty(), "shortProperty is 654");
    assertEquals("This is a string", bean.getStringProperty(), "stringProperty is \"This is a string\"");
    assertEquals("New writeOnlyProperty value", bean.getWriteOnlyPropertyValue(), "writeOnlyProperty is \"New writeOnlyProperty value\"");
    assertEquals("Read Only String Property", bean.getReadOnlyProperty(), "readOnlyProperty is \"Read Only String Property\"");
  }

}
