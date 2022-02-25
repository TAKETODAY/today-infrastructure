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

package cn.taketoday.core.conversion.support;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;

import cn.taketoday.core.DecoratingProxy;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.core.conversion.ConditionalConverter;
import cn.taketoday.core.conversion.ConditionalGenericConverter;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionFailedException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.Converter;
import cn.taketoday.core.conversion.ConverterFactory;
import cn.taketoday.core.conversion.ConverterNotFoundException;
import cn.taketoday.core.conversion.ConverterRegistry;
import cn.taketoday.core.conversion.GenericConverter;
import cn.taketoday.core.conversion.GenericConverter.ConvertiblePair;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.StringUtils;

/**
 * Base {@link ConversionService} implementation suitable for use in most environments.
 * Indirectly implements {@link ConverterRegistry} as registration API through the
 * {@link ConfigurableConversionService} interface.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author David Haraburda
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 16:39
 */
public class GenericConversionService implements ConfigurableConversionService {

  /**
   * General NO-OP converter used when conversion is not required.
   */
  private static final GenericConverter NO_OP_CONVERTER = new NoOpConverter("NO_OP");

  /**
   * Used as a cache entry when no converter is available.
   * This converter is never returned.
   */
  private static final GenericConverter NO_MATCH = new NoOpConverter("NO_MATCH");

  private final Converters converters = new Converters();

  private final ConcurrentReferenceHashMap<ConverterCacheKey, GenericConverter> converterCache
          = new ConcurrentReferenceHashMap<>(64);

  // ConverterRegistry implementation

  @Override
  public void addConverter(Converter<?, ?> converter) {
    ResolvableType[] typeInfo = getRequiredTypeInfo(converter.getClass(), Converter.class);
    if (typeInfo == null && converter instanceof DecoratingProxy decoratingProxy) {
      typeInfo = getRequiredTypeInfo(decoratingProxy.getDecoratedClass(), Converter.class);
    }
    if (typeInfo == null) {
      throw new IllegalArgumentException("Unable to determine source type <S> and target type <T> for your " +
              "Converter [" + converter.getClass().getName() + "]; does the class parameterize those types?");
    }
    addConverter(new ConverterAdapter(converter, typeInfo[0], typeInfo[1]));
  }

  @Override
  public <S, T> void addConverter(
          Class<S> sourceType, Class<T> targetType,
          Converter<? super S, ? extends T> converter) {
    addConverter(new ConverterAdapter(
            converter, ResolvableType.fromClass(sourceType), ResolvableType.fromClass(targetType)));
  }

  @Override
  public void addConverter(GenericConverter converter) {
    this.converters.add(converter);
    invalidateCache();
  }

  @Override
  public void addConverterFactory(ConverterFactory<?, ?> factory) {
    ResolvableType[] typeInfo = getRequiredTypeInfo(factory.getClass(), ConverterFactory.class);
    if (typeInfo == null && factory instanceof DecoratingProxy) {
      typeInfo = getRequiredTypeInfo(((DecoratingProxy) factory).getDecoratedClass(), ConverterFactory.class);
    }
    if (typeInfo == null) {
      throw new IllegalArgumentException("Unable to determine source type <S> and target type <T> for your " +
              "ConverterFactory [" + factory.getClass().getName() + "]; does the class parameterize those types?");
    }
    addConverter(new ConverterFactoryAdapter(factory,
            new ConvertiblePair(typeInfo[0].toClass(), typeInfo[1].toClass())));
  }

  @Override
  public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
    this.converters.remove(sourceType, targetType);
    invalidateCache();
  }

  // ConversionService implementation

  @Override
  public boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType) {
    Assert.notNull(targetType, "Target type to convert to cannot be null");
    return canConvert((sourceType != null ? TypeDescriptor.valueOf(sourceType) : null),
            TypeDescriptor.valueOf(targetType));
  }

  @Override
  public boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
    Assert.notNull(targetType, "Target type to convert to cannot be null");
    if (sourceType == null) {
      return true;
    }
    GenericConverter converter = getConverter(sourceType, targetType);
    return converter != null;
  }

  /**
   * Return whether conversion between the source type and the target type can be bypassed.
   * <p>More precisely, this method will return true if objects of sourceType can be
   * converted to the target type by returning the source object unchanged.
   *
   * @param sourceType context about the source type to convert from
   * (may be {@code null} if source is {@code null})
   * @param targetType context about the target type to convert to (required)
   * @return {@code true} if conversion can be bypassed; {@code false} otherwise
   * @throws IllegalArgumentException if targetType is {@code null}
   */
  public boolean canBypassConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
    Assert.notNull(targetType, "Target type to convert to cannot be null");
    if (sourceType == null) {
      return true;
    }
    GenericConverter converter = getConverter(sourceType, targetType);
    return converter == NO_OP_CONVERTER;
  }

  @Override
  @SuppressWarnings("unchecked")
  @Nullable
  public <T> T convert(@Nullable Object source, Class<T> targetType) {
    Assert.notNull(targetType, "Target type to convert to cannot be null");
    return (T) convert(source, TypeDescriptor.fromObject(source), TypeDescriptor.valueOf(targetType));
  }

  @Override
  @Nullable
  public Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
    Assert.notNull(targetType, "Target type to convert to cannot be null");
    if (sourceType == null) {
      Assert.isTrue(source == null, "Source must be [null] if source type == [null]");
      return handleResult(null, targetType, convertNullSource(null, targetType));
    }
    if (source != null && !sourceType.getObjectType().isInstance(source)) {
      throw new IllegalArgumentException("Source to convert from must be an instance of [" +
              sourceType + "]; instead it was a [" + source.getClass().getName() + "]");
    }
    GenericConverter converter = getConverter(sourceType, targetType);
    if (converter != null) {
      Object result = ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
      return handleResult(sourceType, targetType, result);
    }
    return handleConverterNotFound(source, sourceType, targetType);
  }

  /**
   * Convenience operation for converting a source object to the specified targetType,
   * where the target type is a descriptor that provides additional conversion context.
   * Simply delegates to {@link #convert(Object, TypeDescriptor, TypeDescriptor)} and
   * encapsulates the construction of the source type descriptor using
   * {@link TypeDescriptor#fromObject(Object)}.
   *
   * @param source the source object
   * @param targetType the target type
   * @return the converted value
   * @throws ConversionException if a conversion exception occurred
   * @throws IllegalArgumentException if targetType is {@code null},
   * or sourceType is {@code null} but source is not {@code null}
   */
  @Nullable
  public Object convert(@Nullable Object source, TypeDescriptor targetType) {
    return convert(source, TypeDescriptor.fromObject(source), targetType);
  }

  @Override
  public String toString() {
    return this.converters.toString();
  }

  // Protected template methods

  /**
   * Template method to convert a {@code null} source.
   * <p>The default implementation returns {@code null} or the Java 8
   * {@link java.util.Optional#empty()} instance if the target type is
   * {@code java.util.Optional}. Subclasses may override this to return
   * custom {@code null} objects for specific target types.
   *
   * @param sourceType the source type to convert from
   * @param targetType the target type to convert to
   * @return the converted null object
   */
  @Nullable
  protected Object convertNullSource(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (targetType.getObjectType() == Optional.class) {
      return Optional.empty();
    }
    return null;
  }

  /**
   * Hook method to lookup the converter for a given sourceType/targetType pair.
   * First queries this ConversionService's converter cache.
   * On a cache miss, then performs an exhaustive search for a matching converter.
   * If no converter matches, returns the default converter.
   *
   * @param sourceType the source type to convert from
   * @param targetType the target type to convert to
   * @return the generic converter that will perform the conversion,
   * or {@code null} if no suitable converter was found
   * @see #getDefaultConverter(TypeDescriptor, TypeDescriptor)
   */
  @Nullable
  public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
    ConverterCacheKey key = new ConverterCacheKey(sourceType, targetType);
    GenericConverter converter = this.converterCache.get(key);
    if (converter != null) {
      return converter != NO_MATCH ? converter : null;
    }

    converter = converters.find(sourceType, targetType);
    if (converter == null) {
      converter = getDefaultConverter(sourceType, targetType);
    }

    if (converter != null) {
      converterCache.put(key, converter);
      return converter;
    }

    converterCache.put(key, NO_MATCH);
    return null;
  }

  /**
   * Return the default converter if no converter is found for the given sourceType/targetType pair.
   * <p>Returns a NO_OP Converter if the source type is assignable to the target type.
   * Returns {@code null} otherwise, indicating no suitable converter could be found.
   *
   * @param sourceType the source type to convert from
   * @param targetType the target type to convert to
   * @return the default generic converter that will perform the conversion
   */
  @Nullable
  protected GenericConverter getDefaultConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
    return sourceType.isAssignableTo(targetType) ? NO_OP_CONVERTER : null;
  }

  // Internal helpers

  @Nullable
  private ResolvableType[] getRequiredTypeInfo(Class<?> converterClass, Class<?> genericIfc) {
    ResolvableType resolvableType = ResolvableType.fromClass(converterClass).as(genericIfc);
    ResolvableType[] generics = resolvableType.getGenerics();
    if (generics.length < 2) {
      return null;
    }
    Class<?> sourceType = generics[0].resolve();
    Class<?> targetType = generics[1].resolve();
    if (sourceType == null || targetType == null) {
      return null;
    }
    return generics;
  }

  private void invalidateCache() {
    this.converterCache.clear();
  }

  @Nullable
  private Object handleConverterNotFound(
          @Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {

    if (source == null) {
      assertNotPrimitiveTargetType(sourceType, targetType);
      return null;
    }
    if ((sourceType == null || sourceType.isAssignableTo(targetType))
            && targetType.getObjectType().isInstance(source)) {
      return source;
    }
    throw new ConverterNotFoundException(sourceType, targetType);
  }

  @Nullable
  private Object handleResult(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType, @Nullable Object result) {
    if (result == null) {
      assertNotPrimitiveTargetType(sourceType, targetType);
    }
    return result;
  }

  private void assertNotPrimitiveTargetType(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
    if (targetType.isPrimitive()) {
      throw new ConversionFailedException(sourceType, targetType, null,
              new IllegalArgumentException("A null value cannot be assigned to a primitive type"));
    }
  }

  /**
   * Adapts a {@link Converter} to a {@link GenericConverter}.
   */
  @SuppressWarnings("unchecked")
  private final class ConverterAdapter implements ConditionalGenericConverter {

    private final ConvertiblePair typeInfo;
    private final ResolvableType targetType;
    private final Converter<Object, Object> converter;

    public ConverterAdapter(Converter<?, ?> converter, ResolvableType sourceType, ResolvableType targetType) {
      this.converter = (Converter<Object, Object>) converter;
      this.typeInfo = new ConvertiblePair(sourceType.toClass(), targetType.toClass());
      this.targetType = targetType;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(this.typeInfo);
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      // Check raw type first...
      if (this.typeInfo.getTargetType() != targetType.getObjectType()) {
        return false;
      }
      // Full check for complex generic type match required?
      ResolvableType rt = targetType.getResolvableType();
      if (!(rt.getType() instanceof Class)
              && !rt.isAssignableFrom(this.targetType)
              && !this.targetType.hasUnresolvableGenerics()) {
        return false;
      }
      return !(this.converter instanceof ConditionalConverter conditionalConverter)
              || conditionalConverter.matches(sourceType, targetType);
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
        return convertNullSource(sourceType, targetType);
      }
      return this.converter.convert(source);
    }

    @Override
    public String toString() {
      return this.typeInfo + " : " + this.converter;
    }
  }

  /**
   * Adapts a {@link ConverterFactory} to a {@link GenericConverter}.
   */
  @SuppressWarnings("unchecked")
  private final class ConverterFactoryAdapter implements ConditionalGenericConverter {

    private final ConvertiblePair typeInfo;
    private final ConverterFactory<Object, Object> converterFactory;

    public ConverterFactoryAdapter(ConverterFactory<?, ?> converterFactory, ConvertiblePair typeInfo) {
      this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
      this.typeInfo = typeInfo;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return Collections.singleton(this.typeInfo);
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
      boolean matches = true;
      if (this.converterFactory instanceof ConditionalConverter conditionalConverter) {
        matches = conditionalConverter.matches(sourceType, targetType);
      }
      if (matches) {
        Converter<?, ?> converter = this.converterFactory.getConverter(targetType.getType());
        if (converter instanceof ConditionalConverter conditionalConverter) {
          matches = conditionalConverter.matches(sourceType, targetType);
        }
      }
      return matches;
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      if (source == null) {
        return convertNullSource(sourceType, targetType);
      }
      return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
    }

    @Override
    public String toString() {
      return typeInfo + " : " + this.converterFactory;
    }
  }

  /**
   * Key for use with the converter cache.
   */
  private record ConverterCacheKey(TypeDescriptor sourceType, TypeDescriptor targetType)
          implements Comparable<ConverterCacheKey> {

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof ConverterCacheKey otherKey)) {
        return false;
      }
      return (this.sourceType.equals(otherKey.sourceType)) &&
              this.targetType.equals(otherKey.targetType);
    }

    @Override
    public int hashCode() {
      return (this.sourceType.hashCode() * 29 + this.targetType.hashCode());
    }

    @Override
    public String toString() {
      return ("ConverterCacheKey [sourceType = " + this.sourceType +
              ", targetType = " + this.targetType + "]");
    }

    @Override
    public int compareTo(ConverterCacheKey other) {
      int result = this.sourceType.getResolvableType().toString()
              .compareTo(other.sourceType.getResolvableType().toString());
      if (result == 0) {
        result = this.targetType.getResolvableType().toString()
                .compareTo(other.targetType.getResolvableType().toString());
      }
      return result;
    }
  }

  /**
   * Manages all converters registered with the service.
   */
  private static class Converters {

    private final CopyOnWriteArraySet<GenericConverter> globalConverters
            = new CopyOnWriteArraySet<>();

    private final ConcurrentHashMap<ConvertiblePair, ConvertersForPair> converters
            = new ConcurrentHashMap<>(256);

    public void add(GenericConverter converter) {
      Set<ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
      if (convertibleTypes == null) {
        Assert.state(converter instanceof ConditionalConverter,
                "Only conditional converters may return null convertible types");
        this.globalConverters.add(converter);
      }
      else {
        for (ConvertiblePair convertiblePair : convertibleTypes) {
          getMatchableConverters(convertiblePair).add(converter);
        }
      }
    }

    private ConvertersForPair getMatchableConverters(ConvertiblePair convertiblePair) {
      return this.converters.computeIfAbsent(convertiblePair, k -> new ConvertersForPair());
    }

    public void remove(Class<?> sourceType, Class<?> targetType) {
      this.converters.remove(new ConvertiblePair(sourceType, targetType));
    }

    /**
     * Find a {@link GenericConverter} given a source and target type.
     * <p>This method will attempt to match all possible converters by working
     * through the class and interface hierarchy of the types.
     *
     * @param sourceType the source type
     * @param targetType the target type
     * @return a matching {@link GenericConverter}, or {@code null} if none found
     */
    @Nullable
    public GenericConverter find(TypeDescriptor sourceType, TypeDescriptor targetType) {
      // Search the full type hierarchy
      List<Class<?>> sourceCandidates = getClassHierarchy(sourceType.getType());
      List<Class<?>> targetCandidates = getClassHierarchy(targetType.getType());
      for (Class<?> sourceCandidate : sourceCandidates) {
        for (Class<?> targetCandidate : targetCandidates) {
          ConvertiblePair convertiblePair = new ConvertiblePair(sourceCandidate, targetCandidate);
          GenericConverter converter = getRegisteredConverter(sourceType, targetType, convertiblePair);
          if (converter != null) {
            return converter;
          }
        }
      }
      return null;
    }

    @Nullable
    private GenericConverter getRegisteredConverter(
            TypeDescriptor sourceType, TypeDescriptor targetType, ConvertiblePair convertiblePair) {

      // Check specifically registered converters
      ConvertersForPair convertersForPair = converters.get(convertiblePair);
      if (convertersForPair != null) {
        GenericConverter converter = convertersForPair.getConverter(sourceType, targetType);
        if (converter != null) {
          return converter;
        }
      }
      // Check ConditionalConverters for a dynamic match
      for (GenericConverter globalConverter : globalConverters) {
        if (((ConditionalConverter) globalConverter).matches(sourceType, targetType)) {
          return globalConverter;
        }
      }
      return null;
    }

    /**
     * Returns an ordered class hierarchy for the given type.
     *
     * @param type the type
     * @return an ordered list of all classes that the given type extends or implements
     */
    private List<Class<?>> getClassHierarchy(Class<?> type) {
      HashSet<Class<?>> visited = new HashSet<>(20);
      ArrayList<Class<?>> hierarchy = new ArrayList<>(20);
      addToClassHierarchy(0, ClassUtils.resolvePrimitiveIfNecessary(type), false, hierarchy, visited);
      boolean array = type.isArray();

      int i = 0;
      while (i < hierarchy.size()) {
        Class<?> candidate = hierarchy.get(i);
        candidate = (array ? candidate.getComponentType() : ClassUtils.resolvePrimitiveIfNecessary(candidate));
        Class<?> superclass = candidate.getSuperclass();
        if (superclass != null && superclass != Object.class && superclass != Enum.class) {
          addToClassHierarchy(i + 1, candidate.getSuperclass(), array, hierarchy, visited);
        }
        addInterfacesToClassHierarchy(candidate, array, hierarchy, visited);
        i++;
      }

      if (Enum.class.isAssignableFrom(type)) {
        addToClassHierarchy(hierarchy.size(), Enum.class, array, hierarchy, visited);
        addToClassHierarchy(hierarchy.size(), Enum.class, false, hierarchy, visited);
        addInterfacesToClassHierarchy(Enum.class, array, hierarchy, visited);
      }

      addToClassHierarchy(hierarchy.size(), Object.class, array, hierarchy, visited);
      addToClassHierarchy(hierarchy.size(), Object.class, false, hierarchy, visited);
      return hierarchy;
    }

    private void addInterfacesToClassHierarchy(
            Class<?> type, boolean asArray, List<Class<?>> hierarchy, Set<Class<?>> visited) {

      for (Class<?> implementedInterface : type.getInterfaces()) {
        addToClassHierarchy(hierarchy.size(), implementedInterface, asArray, hierarchy, visited);
      }
    }

    private void addToClassHierarchy(
            int index, Class<?> type, boolean asArray, List<Class<?>> hierarchy, Set<Class<?>> visited) {

      if (asArray) {
        type = Array.newInstance(type, 0).getClass();
      }
      if (visited.add(type)) {
        hierarchy.add(index, type);
      }
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("ConversionService converters =\n");
      for (String converterString : getConverterStrings()) {
        builder.append('\t').append(converterString).append('\n');
      }
      return builder.toString();
    }

    private List<String> getConverterStrings() {
      ArrayList<String> converterStrings = new ArrayList<>();
      for (ConvertersForPair convertersForPair : this.converters.values()) {
        converterStrings.add(convertersForPair.toString());
      }
      Collections.sort(converterStrings);
      return converterStrings;
    }
  }

  /**
   * Manages converters registered with a specific {@link ConvertiblePair}.
   */
  private static class ConvertersForPair {

    private final ConcurrentLinkedDeque<GenericConverter> converters = new ConcurrentLinkedDeque<>();

    public void add(GenericConverter converter) {
      this.converters.addFirst(converter);
    }

    @Nullable
    public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
      for (GenericConverter converter : this.converters) {
        if (!(converter instanceof ConditionalGenericConverter conditional)
                || conditional.matches(sourceType, targetType)) {
          return converter;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return StringUtils.collectionToCommaDelimitedString(this.converters);
    }
  }

  /**
   * Internal converter that performs no operation.
   */
  private record NoOpConverter(String name) implements GenericConverter {

    @Override
    @Nullable
    public Set<ConvertiblePair> getConvertibleTypes() {
      return null;
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      return source;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

}
