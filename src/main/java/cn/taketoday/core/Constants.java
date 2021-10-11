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
package cn.taketoday.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;

/**
 * This class can be used to parse other classes containing constant definitions
 * in public static final members. The {@code asXXXX} methods of this class
 * allow these constant values to be accessed via their string names.
 *
 * <p>
 * Consider class Foo containing {@code public final static int CONSTANT1 = 66;}
 * An instance of this class wrapping {@code Foo.class} will return the constant
 * value of 66 from its {@code asNumber} method given the argument
 * {@code "CONSTANT1"}.
 *
 * <p>
 * This class is ideal for use in PropertyEditors, enabling them to recognize
 * the same names as the constants themselves, and freeing them from maintaining
 * their own mapping.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class Constants {

  /** The name of the introspected class */
  private final String className;

  /** Map from String field name to object value */
  private final HashMap<String, Object> fieldCache = new HashMap<>();

  /**
   * Create a new Constants converter class wrapping the given class.
   * <p>
   * All <b>public</b> static final variables will be exposed, whatever their
   * type.
   *
   * @param clazz
   *         the class to analyze
   *
   * @throws IllegalArgumentException
   *         if the supplied {@code clazz} is {@code null}
   */
  public Constants(Class<?> clazz) {
    Assert.notNull(clazz, "class must not be null");
    this.className = clazz.getName();
    Field[] fields = clazz.getFields();
    for (Field field : fields) {

      int modifiers = field.getModifiers();

      if ((Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers))) {
        String name = field.getName();
        try {
          Object value = field.get(null);
          this.fieldCache.put(name, value);
        }
        catch (IllegalAccessException ex) {
          // just leave this field and continue
        }
      }
    }
  }

  /**
   * Return the name of the analyzed class.
   */
  public final String getClassName() {
    return this.className;
  }

  /**
   * Return the number of constants exposed.
   */
  public final int getSize() {
    return this.fieldCache.size();
  }

  /**
   * Exposes the field cache to subclasses: a Map from String field name to object
   * value.
   */
  protected final Map<String, Object> getFieldCache() {
    return this.fieldCache;
  }

  /**
   * Return a constant value cast to a Number.
   *
   * @param code
   *         the name of the field (never {@code null})
   *
   * @return the Number value
   *
   * @throws ConstantException
   *         if the field name wasn't found or if the type wasn't compatible
   *         with Number
   * @see #asObject
   */
  public Number asNumber(String code) throws ConstantException {
    Object obj = asObject(code);
    if (!(obj instanceof Number)) {
      throw new ConstantException(this.className, code, "not a Number");
    }
    return (Number) obj;
  }

  /**
   * Return a constant value as a String.
   *
   * @param code
   *         the name of the field (never {@code null})
   *
   * @return the String value Works even if it's not a string (invokes
   * {@code toString()}).
   *
   * @throws ConstantException
   *         if the field name wasn't found
   * @see #asObject
   */
  public String asString(String code) throws ConstantException {
    return asObject(code).toString();
  }

  /**
   * Parse the given String (upper or lower case accepted) and return the
   * appropriate value if it's the name of a constant field in the class that
   * we're analysing.
   *
   * @param code
   *         the name of the field (never {@code null})
   *
   * @return the Object value
   *
   * @throws ConstantException
   *         if there's no such field
   */
  public Object asObject(String code) throws ConstantException {
    String codeToUse = code.toUpperCase(Locale.ENGLISH);
    Object val = this.fieldCache.get(codeToUse);
    if (val == null) {
      throw new ConstantException(this.className, codeToUse, "not found");
    }
    return val;
  }

  /**
   * Return all names of the given group of constants.
   * <p>
   * Note that this method assumes that constants are named in accordance with the
   * standard Java convention for constant values (i.e. all uppercase). The
   * supplied {@code namePrefix} will be uppercased (in a locale-insensitive
   * fashion) prior to the main logic of this method kicking in.
   *
   * @param namePrefix
   *         prefix of the constant names to search (may be {@code null})
   *
   * @return the set of constant names
   */
  public Set<String> getNames(String namePrefix) {
    String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : Constant.BLANK);
    Set<String> names = new HashSet<>();
    for (String code : this.fieldCache.keySet()) {
      if (code.startsWith(prefixToUse)) {
        names.add(code);
      }
    }
    return names;
  }

  /**
   * Return all names of the group of constants for the given bean property name.
   *
   * @param propertyName
   *         the name of the bean property
   *
   * @return the set of values
   *
   * @see #propertyToConstantNamePrefix
   */
  public Set<String> getNamesForProperty(String propertyName) {
    return getNames(propertyToConstantNamePrefix(propertyName));
  }

  /**
   * Return all names of the given group of constants.
   * <p>
   * Note that this method assumes that constants are named in accordance with the
   * standard Java convention for constant values (i.e. all uppercase). The
   * supplied {@code nameSuffix} will be uppercased (in a locale-insensitive
   * fashion) prior to the main logic of this method kicking in.
   *
   * @param nameSuffix
   *         suffix of the constant names to search (may be {@code null})
   *
   * @return the set of constant names
   */
  public Set<String> getNamesForSuffix(String nameSuffix) {
    String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
    Set<String> names = new HashSet<>();
    for (String code : this.fieldCache.keySet()) {
      if (code.endsWith(suffixToUse)) {
        names.add(code);
      }
    }
    return names;
  }

  /**
   * Return all values of the given group of constants.
   * <p>
   * Note that this method assumes that constants are named in accordance with the
   * standard Java convention for constant values (i.e. all uppercase). The
   * supplied {@code namePrefix} will be uppercased (in a locale-insensitive
   * fashion) prior to the main logic of this method kicking in.
   *
   * @param namePrefix
   *         prefix of the constant names to search (may be {@code null})
   *
   * @return the set of values
   */
  public Set<Object> getValues(String namePrefix) {
    String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
    Set<Object> values = new HashSet<>();
    this.fieldCache.forEach((code, value) -> {
      if (code.startsWith(prefixToUse)) {
        values.add(value);
      }
    });
    return values;
  }

  /**
   * Return all values of the group of constants for the given bean property name.
   *
   * @param propertyName
   *         the name of the bean property
   *
   * @return the set of values
   *
   * @see #propertyToConstantNamePrefix
   */
  public Set<Object> getValuesForProperty(String propertyName) {
    return getValues(propertyToConstantNamePrefix(propertyName));
  }

  /**
   * Return all values of the given group of constants.
   * <p>
   * Note that this method assumes that constants are named in accordance with the
   * standard Java convention for constant values (i.e. all uppercase). The
   * supplied {@code nameSuffix} will be uppercased (in a locale-insensitive
   * fashion) prior to the main logic of this method kicking in.
   *
   * @param nameSuffix
   *         suffix of the constant names to search (may be {@code null})
   *
   * @return the set of values
   */
  public Set<Object> getValuesForSuffix(String nameSuffix) {
    String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
    Set<Object> values = new HashSet<>();
    this.fieldCache.forEach((code, value) -> {
      if (code.endsWith(suffixToUse)) {
        values.add(value);
      }
    });
    return values;
  }

  /**
   * Look up the given value within the given group of constants.
   * <p>
   * Will return the first match.
   *
   * @param value
   *         constant value to look up
   * @param namePrefix
   *         prefix of the constant names to search (may be {@code null})
   *
   * @return the name of the constant field
   *
   * @throws ConstantException
   *         if the value wasn't found
   */
  public String toCode(Object value, String namePrefix) throws ConstantException {
    String prefixToUse = (namePrefix != null ? namePrefix.trim().toUpperCase(Locale.ENGLISH) : "");
    for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
      if (entry.getKey().startsWith(prefixToUse) && entry.getValue().equals(value)) {
        return entry.getKey();
      }
    }
    throw new ConstantException(this.className, prefixToUse, value);
  }

  /**
   * Look up the given value within the group of constants for the given bean
   * property name. Will return the first match.
   *
   * @param value
   *         constant value to look up
   * @param propertyName
   *         the name of the bean property
   *
   * @return the name of the constant field
   *
   * @throws ConstantException
   *         if the value wasn't found
   * @see #propertyToConstantNamePrefix
   */
  public String toCodeForProperty(Object value, String propertyName) throws ConstantException {
    return toCode(value, propertyToConstantNamePrefix(propertyName));
  }

  /**
   * Look up the given value within the given group of constants.
   * <p>
   * Will return the first match.
   *
   * @param value
   *         constant value to look up
   * @param nameSuffix
   *         suffix of the constant names to search (may be {@code null})
   *
   * @return the name of the constant field
   *
   * @throws ConstantException
   *         if the value wasn't found
   */
  public String toCodeForSuffix(Object value, String nameSuffix) throws ConstantException {
    String suffixToUse = (nameSuffix != null ? nameSuffix.trim().toUpperCase(Locale.ENGLISH) : "");
    for (Map.Entry<String, Object> entry : this.fieldCache.entrySet()) {
      if (entry.getKey().endsWith(suffixToUse) && entry.getValue().equals(value)) {
        return entry.getKey();
      }
    }
    throw new ConstantException(this.className, suffixToUse, value);
  }

  /**
   * Convert the given bean property name to a constant name prefix.
   * <p>
   * Uses a common naming idiom: turning all lower case characters to upper case,
   * and prepending upper case characters with an underscore.
   * <p>
   * Example: "imageSize" -> "IMAGE_SIZE"<br>
   * Example: "imagesize" -> "IMAGESIZE".<br>
   * Example: "ImageSize" -> "_IMAGE_SIZE".<br>
   * Example: "IMAGESIZE" -> "_I_M_A_G_E_S_I_Z_E"
   *
   * @param propertyName
   *         the name of the bean property
   *
   * @return the corresponding constant name prefix
   *
   * @see #getValuesForProperty
   * @see #toCodeForProperty
   */
  public String propertyToConstantNamePrefix(String propertyName) {
    StringBuilder parsedPrefix = new StringBuilder();
    for (int i = 0; i < propertyName.length(); i++) {
      char c = propertyName.charAt(i);
      if (Character.isUpperCase(c)) {
        parsedPrefix.append('_');
        parsedPrefix.append(c);
      }
      else {
        parsedPrefix.append(Character.toUpperCase(c));
      }
    }
    return parsedPrefix.toString();
  }

  /**
   * Exception thrown when the {@link Constants} class is asked for an invalid
   * constant name.
   */
  @SuppressWarnings("serial")
  public static class ConstantException extends IllegalArgumentException {

    /**
     * Thrown when an invalid constant name is requested.
     *
     * @param className
     *         name of the class containing the constant definitions
     * @param field
     *         invalid constant name
     * @param message
     *         description of the problem
     */
    public ConstantException(String className, String field, String message) {
      super("Field '" + field + "' " + message + " in class [" + className + "]");
    }

    /**
     * Thrown when an invalid constant value is looked up.
     *
     * @param className
     *         name of the class containing the constant definitions
     * @param namePrefix
     *         prefix of the searched constant names
     * @param value
     *         the looked up constant value
     */
    public ConstantException(String className, String namePrefix, Object value) {
      super("No '" + namePrefix + "' field with value '" + value + "' found in class [" + className + "]");
    }
  }

}
