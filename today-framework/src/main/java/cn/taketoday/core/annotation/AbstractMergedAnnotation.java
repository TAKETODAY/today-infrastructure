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

package cn.taketoday.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for {@link MergedAnnotation} implementations.
 *
 * @param <A> the annotation type
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 4.0
 */
abstract class AbstractMergedAnnotation<A extends Annotation> implements MergedAnnotation<A> {

  @Nullable
  private volatile A synthesizedAnnotation;

  @Override
  public boolean isDirectlyPresent() {
    return isPresent() && getDistance() == 0;
  }

  @Override
  public boolean isMetaPresent() {
    return isPresent() && getDistance() > 0;
  }

  @Override
  public boolean hasNonDefaultValue(String attributeName) {
    return !hasDefaultValue(attributeName);
  }

  @Override
  public byte getByte(String attributeName) {
    return getRequiredAttributeValue(attributeName, Byte.class);
  }

  @Override
  public byte[] getByteArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, byte[].class);
  }

  @Override
  public boolean getBoolean(String attributeName) {
    return getRequiredAttributeValue(attributeName, Boolean.class);
  }

  @Override
  public boolean[] getBooleanArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, boolean[].class);
  }

  @Override
  public char getChar(String attributeName) {
    return getRequiredAttributeValue(attributeName, Character.class);
  }

  @Override
  public char[] getCharArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, char[].class);
  }

  @Override
  public short getShort(String attributeName) {
    return getRequiredAttributeValue(attributeName, Short.class);
  }

  @Override
  public short[] getShortArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, short[].class);
  }

  @Override
  public int getInt(String attributeName) {
    return getRequiredAttributeValue(attributeName, Integer.class);
  }

  @Override
  public int[] getIntArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, int[].class);
  }

  @Override
  public long getLong(String attributeName) {
    return getRequiredAttributeValue(attributeName, Long.class);
  }

  @Override
  public long[] getLongArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, long[].class);
  }

  @Override
  public double getDouble(String attributeName) {
    return getRequiredAttributeValue(attributeName, Double.class);
  }

  @Override
  public double[] getDoubleArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, double[].class);
  }

  @Override
  public float getFloat(String attributeName) {
    return getRequiredAttributeValue(attributeName, Float.class);
  }

  @Override
  public float[] getFloatArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, float[].class);
  }

  @Override
  public String getString(String attributeName) {
    return getRequiredAttributeValue(attributeName, String.class);
  }

  @Override
  public String[] getStringArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, String[].class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Class<T> getClass(String attributeName) {
    return getRequiredAttributeValue(attributeName, Class.class);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Class<T>[] getClassArray(String attributeName) {
    return getRequiredAttributeValue(attributeName, Class[].class);
  }

  @Override
  public <E extends Enum<E>> E getEnum(String attributeName, Class<E> type) {
    Assert.notNull(type, "Type must not be null");
    return getRequiredAttributeValue(attributeName, type);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <E extends Enum<E>> E[] getEnumArray(String attributeName, Class<E> type) {
    Assert.notNull(type, "Type must not be null");
    Class<?> arrayType = Array.newInstance(type, 0).getClass();
    return (E[]) getRequiredAttributeValue(attributeName, arrayType);
  }

  @Override
  public Optional<Object> getValue(String attributeName) {
    return getValue(attributeName, Object.class);
  }

  @Override
  public <T> Optional<T> getValue(String attributeName, Class<T> type) {
    return Optional.ofNullable(getAttributeValue(attributeName, type));
  }

  @Override
  public Optional<Object> getDefaultValue(String attributeName) {
    return getDefaultValue(attributeName, Object.class);
  }

  @Override
  public MergedAnnotation<A> filterDefaultValues() {
    return filterAttributes(this::hasNonDefaultValue);
  }

  @Override
  public AnnotationAttributes asAnnotationAttributes(Adapt... adaptations) {
    return asMap(mergedAnnotation -> new AnnotationAttributes(mergedAnnotation.getType()), adaptations);
  }

  @Override
  public Optional<A> synthesize(Predicate<? super MergedAnnotation<A>> condition) throws NoSuchElementException {
    return (condition.test(this) ? Optional.of(synthesize()) : Optional.empty());
  }

  @Override
  public A synthesize() {
    if (!isPresent()) {
      throw new NoSuchElementException("Unable to synthesize missing annotation");
    }
    A synthesized = this.synthesizedAnnotation;
    if (synthesized == null) {
      synchronized(this) {
        synthesized = this.synthesizedAnnotation;
        if (synthesized == null) {
          synthesized = createSynthesizedAnnotation();
          this.synthesizedAnnotation = synthesized;
        }
      }
    }
    return synthesized;
  }

  private <T> T getRequiredAttributeValue(String attributeName, Class<T> type) {
    T value = getAttributeValue(attributeName, type);
    if (value == null) {
      throw new NoSuchElementException("No attribute named '" + attributeName +
              "' present in merged annotation " + getType().getName());
    }
    return value;
  }

  /**
   * Get the underlying attribute value.
   *
   * @param attributeName the attribute name
   * @param type the type to return (see {@link MergedAnnotation} class
   * documentation for details)
   * @return the attribute value or {@code null} if the value is not found and
   * is not required
   * @throws IllegalArgumentException if the source type is not compatible
   * @throws NoSuchElementException if the value is required but not found
   */
  @Nullable
  protected abstract <T> T getAttributeValue(String attributeName, Class<T> type);

  /**
   * Factory method used to create the synthesized annotation.
   * <p>If the source is an annotation that is not <em>synthesizable</em>, it
   * will be returned unmodified.
   * <p>Consult the documentation for {@link MergedAnnotation#synthesize()}
   * for an explanation of what is considered synthesizable.
   */
  protected abstract A createSynthesizedAnnotation();

}
