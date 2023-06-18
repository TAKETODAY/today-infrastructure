/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author TODAY 2021/5/28 21:59
 */
@SuppressWarnings("serial")
public class BeanMappingTestBean implements Serializable {

  public BeanMappingTestBean() {
    listIndexed.add("String 0");
    listIndexed.add("String 1");
    listIndexed.add("String 2");
    listIndexed.add("String 3");
    listIndexed.add("String 4");
  }

  public BeanMappingTestBean(final String stringProperty) {
    setStringProperty(stringProperty);
  }

  public BeanMappingTestBean(final float floatProperty) {
    setFloatProperty(floatProperty);
  }

  public BeanMappingTestBean(final boolean booleanProperty) {
    setBooleanProperty(booleanProperty);
  }

  public BeanMappingTestBean(final Boolean booleanSecond) {
    setBooleanSecond(booleanSecond.booleanValue());
  }

  public BeanMappingTestBean(final float floatProperty, final String stringProperty) {
    setFloatProperty(floatProperty);
    setStringProperty(stringProperty);
  }

  public BeanMappingTestBean(final boolean booleanProperty, final String stringProperty) {
    setBooleanProperty(booleanProperty);
    setStringProperty(stringProperty);
  }

  public BeanMappingTestBean(final Boolean booleanSecond, final String stringProperty) {
    setBooleanSecond(booleanSecond.booleanValue());
    setStringProperty(stringProperty);
  }

  public BeanMappingTestBean(final Integer intProperty) {
    setIntProperty(intProperty.intValue());
  }

  public BeanMappingTestBean(final double doubleProperty) {
    setDoubleProperty(doubleProperty);
  }

  BeanMappingTestBean(final int intProperty) {
    setIntProperty(intProperty);
  }

  protected BeanMappingTestBean(final boolean booleanProperty, final boolean booleanSecond, final String stringProperty) {
    setBooleanProperty(booleanProperty);
    setBooleanSecond(booleanSecond);
    setStringProperty(stringProperty);
  }

  public BeanMappingTestBean(final List<Object> listIndexed) {
    this.listIndexed = listIndexed;
  }

  public BeanMappingTestBean(final String[][] string2dArray) {
    this.string2dArray = string2dArray;
  }

  /**
   * A boolean property.
   */
  private boolean booleanProperty = true;

  public boolean getBooleanProperty() {
    return booleanProperty;
  }

  public void setBooleanProperty(final boolean booleanProperty) {
    this.booleanProperty = booleanProperty;
  }

  /**
   * A boolean property that uses an "is" method for the getter.
   */
  private boolean booleanSecond = true;

  public boolean isBooleanSecond() {
    return booleanSecond;
  }

  public void setBooleanSecond(final boolean booleanSecond) {
    this.booleanSecond = booleanSecond;
  }

  /**
   * A byte property.
   */
  private byte byteProperty = (byte) 121;

  public byte getByteProperty() {
    return this.byteProperty;
  }

  public void setByteProperty(final byte byteProperty) {
    this.byteProperty = byteProperty;
  }

  /**
   * A java.util.Date property.
   */
  private java.util.Date dateProperty;

  public java.util.Date getDateProperty() {
    return dateProperty;
  }

  public void setDateProperty(final java.util.Date dateProperty) {
    this.dateProperty = dateProperty;
  }

  /**
   * A java.util.Date property.
   */
  private java.util.Date[] dateArrayProperty;

  public java.util.Date[] getDateArrayProperty() {
    return dateArrayProperty;
  }

  public void setDateArrayProperty(final java.util.Date[] dateArrayProperty) {
    this.dateArrayProperty = dateArrayProperty;
  }

  /**
   * A double property.
   */
  private double doubleProperty = 321.0;

  public double getDoubleProperty() {
    return this.doubleProperty;
  }

  public void setDoubleProperty(final double doubleProperty) {
    this.doubleProperty = doubleProperty;
  }

  /**
   * An "indexed property" accessible via both array and subscript
   * based getters and setters.
   */
  private String[] dupProperty =
          { "Dup 0", "Dup 1", "Dup 2", "Dup 3", "Dup 4" };

  public String[] getDupProperty() {
    return this.dupProperty;
  }

  public String getDupProperty(final int index) {
    return this.dupProperty[index];
  }

  public void setDupProperty(final int index, final String value) {
    this.dupProperty[index] = value;
  }

  public void setDupProperty(final String[] dupProperty) {
    this.dupProperty = dupProperty;
  }

  /**
   * A float property.
   */
  private float floatProperty = (float) 123.0;

  public float getFloatProperty() {
    return this.floatProperty;
  }

  public void setFloatProperty(final float floatProperty) {
    this.floatProperty = floatProperty;
  }

  /**
   * An integer array property accessed as an array.
   */
  private int[] intArray = { 0, 10, 20, 30, 40 };

  public int[] getIntArray() {
    return this.intArray;
  }

  public void setIntArray(final int[] intArray) {
    this.intArray = intArray;
  }

  /**
   * An integer array property accessed as an indexed property.
   */
  private final int[] intIndexed = { 0, 10, 20, 30, 40 };

  public int getIntIndexed(final int index) {
    return intIndexed[index];
  }

  public void setIntIndexed(final int index, final int value) {
    intIndexed[index] = value;
  }

  /**
   * An integer property.
   */
  private int intProperty = 123;

  public int getIntProperty() {
    return this.intProperty;
  }

  public void setIntProperty(final int intProperty) {
    this.intProperty = intProperty;
  }

  /**
   * A List property accessed as an indexed property.
   */
  private List<Object> listIndexed = new ArrayList<>();

  public List<Object> getListIndexed() {
    return listIndexed;
  }

  /**
   * A long property.
   */
  private long longProperty = 321;

  public long getLongProperty() {
    return this.longProperty;
  }

  public void setLongProperty(final long longProperty) {
    this.longProperty = longProperty;
  }

  /**
   * A mapped property with only a getter and setter for a Map.
   */
  private Map<String, Object> mapProperty = null;

  public Map<String, Object> getMapProperty() {
    // Create the map the very first time
    if (mapProperty == null) {
      mapProperty = new HashMap<>();
      mapProperty.put("First Key", "First Value");
      mapProperty.put("Second Key", "Second Value");
    }
    return mapProperty;
  }

  public void setMapProperty(Map<String, Object> mapProperty) {
    // Create the map the very first time
    if (mapProperty == null) {
      mapProperty = new HashMap<>();
      mapProperty.put("First Key", "First Value");
      mapProperty.put("Second Key", "Second Value");
    }
    this.mapProperty = mapProperty;
  }

  /**
   * A mapped property that has String keys and Object values.
   */
  private HashMap<String, Object> mappedObjects = null;

  public Object getMappedObjects(final String key) {
    // Create the map the very first time
    if (mappedObjects == null) {
      mappedObjects = new HashMap<>();
      mappedObjects.put("First Key", "First Value");
      mappedObjects.put("Second Key", "Second Value");
    }
    return mappedObjects.get(key);
  }

  public void setMappedObjects(final String key, final Object value) {
    // Create the map the very first time
    if (mappedObjects == null) {
      mappedObjects = new HashMap<>();
      mappedObjects.put("First Key", "First Value");
      mappedObjects.put("Second Key", "Second Value");
    }
    mappedObjects.put(key, value);
  }

  /**
   * A mapped property that has String keys and String values.
   */
  private HashMap<String, String> mappedProperty = null;

  public String getMappedProperty(final String key) {
    // Create the map the very first time
    if (mappedProperty == null) {
      mappedProperty = new HashMap<>();
      mappedProperty.put("First Key", "First Value");
      mappedProperty.put("Second Key", "Second Value");
    }
    return mappedProperty.get(key);
  }

  public void setMappedProperty(final String key, final String value) {
    // Create the map the very first time
    if (mappedProperty == null) {
      mappedProperty = new HashMap<>();
      mappedProperty.put("First Key", "First Value");
      mappedProperty.put("Second Key", "Second Value");
    }
    mappedProperty.put(key, value);
  }

  /**
   * A mapped property that has String keys and int values.
   */
  private HashMap<String, Integer> mappedIntProperty = null;

  public int getMappedIntProperty(final String key) {
    // Create the map the very first time
    if (mappedIntProperty == null) {
      mappedIntProperty = new HashMap<>();
      mappedIntProperty.put("One", 1);
      mappedIntProperty.put("Two", 2);
    }
    final Integer x = mappedIntProperty.get(key);
    return x == null ? 0 : x;
  }

  public void setMappedIntProperty(final String key, final int value) {
    mappedIntProperty.put(key, value);
  }

  /**
   * A nested reference to another test bean (populated as needed).
   */
  private BeanMappingTestBean nested = null;

  public BeanMappingTestBean getNested() {
    if (nested == null) {
      nested = new BeanMappingTestBean();
    }
    return nested;
  }

  /**
   * Another nested reference to another test bean,
   */
  private BeanMappingTestBean anotherNested = null;

  public BeanMappingTestBean getAnotherNested() {
    return anotherNested;
  }

  public void setAnotherNested(final BeanMappingTestBean anotherNested) {
    this.anotherNested = anotherNested;
  }

  /*
   * Another nested reference to a bean containing mapp properties
   */
  public class MappedTestBean {
    public void setValue(final String key, final String val) { }

    public String getValue(final String key) { return "Mapped Value"; }
  }

  private MappedTestBean mappedNested = null;

  public MappedTestBean getMappedNested() {
    if (mappedNested == null) {
      mappedNested = new MappedTestBean();
    }
    return mappedNested;
  }

  /**
   * A String property with an initial value of null.
   */
  private String nullProperty = null;

  public String getNullProperty() {
    return this.nullProperty;
  }

  public void setNullProperty(final String nullProperty) {
    this.nullProperty = nullProperty;
  }

  /**
   * A read-only String property.
   */
  private final String readOnlyProperty = "Read Only String Property";

  public String getReadOnlyProperty() {
    return this.readOnlyProperty;
  }

  /**
   * A short property.
   */
  private short shortProperty = (short) 987;

  public short getShortProperty() {
    return this.shortProperty;
  }

  public void setShortProperty(final short shortProperty) {
    this.shortProperty = shortProperty;
  }

  /**
   * A String array property accessed as a String.
   */
  private String[] stringArray =
          { "String 0", "String 1", "String 2", "String 3", "String 4" };

  public String[] getStringArray() {
    return this.stringArray;
  }

  public void setStringArray(final String[] stringArray) {
    this.stringArray = stringArray;
  }

  /**
   * A String array property accessed as an indexed property.
   */
  private final String[] stringIndexed =
          { "String 0", "String 1", "String 2", "String 3", "String 4" };

  public String getStringIndexed(final int index) {
    return stringIndexed[index];
  }

  public void setStringIndexed(final int index, final String value) {
    stringIndexed[index] = value;
  }

  private String[][] string2dArray = new String[][] { new String[] { "1", "2", "3" }, new String[] { "4", "5", "6" } };

  public String[] getString2dArray(final int index) {
    return string2dArray[index];
  }

  /**
   * A String property.
   */
  private String stringProperty = "This is a string";

  public String getStringProperty() {
    return this.stringProperty;
  }

  public void setStringProperty(final String stringProperty) {
    this.stringProperty = stringProperty;
  }

  /**
   * A write-only String property.
   */
  private String writeOnlyProperty = "Write Only String Property";

  public String getWriteOnlyPropertyValue() {
    return this.writeOnlyProperty;
  }

  public void setWriteOnlyProperty(final String writeOnlyProperty) {
    this.writeOnlyProperty = writeOnlyProperty;
  }

  /**
   * <p>An invalid property that has two boolean getters (getInvalidBoolean
   * and isInvalidBoolean) plus a String setter (setInvalidBoolean).  By the
   * rules described in the JavaBeans Specification, this will be considered
   * a read-only boolean property, using isInvalidBoolean() as the getter.</p>
   */
  private boolean invalidBoolean = false;

  public boolean getInvalidBoolean() {
    return this.invalidBoolean;
  }

  public boolean isInvalidBoolean() {
    return this.invalidBoolean;
  }

  public void setInvalidBoolean(final String invalidBoolean) {
    this.invalidBoolean = "true".equalsIgnoreCase(invalidBoolean) ||
            "yes".equalsIgnoreCase(invalidBoolean) ||
            "1".equalsIgnoreCase(invalidBoolean);
  }

  /**
   * A static variable that is accessed and updated via static methods
   * for MethodUtils testing.
   */
  private static int counter = 0;

  /**
   * Gets the current value of the counter.
   */
  public static int currentCounter() {

    return counter;

  }

  /**
   * Increment the current value of the counter by 1.
   */
  public static void incrementCounter() {

    incrementCounter(1);

  }

  /**
   * Increment the current value of the counter by the specified amount.
   *
   * @param amount Amount to be added to the current counter
   */
  public static void incrementCounter(final int amount) {

    counter += amount;

  }

  /**
   * Increments the current value of the count by the
   * specified amount * 2. It has the same name
   * as the method above so as to test the looseness
   * of getMethod.
   */
  public static void incrementCounter(final Number amount) {
    counter += 2 * amount.intValue();
  }

}
