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

package cn.taketoday.context.conversion;

import java.io.File;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.DataSize;
import cn.taketoday.context.utils.MediaType;
import cn.taketoday.context.utils.MimeType;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.ReflectionUtils;
import cn.taketoday.context.utils.ResourceUtils;
import cn.taketoday.context.utils.StringUtils;

import static cn.taketoday.context.conversion.DelegatingStringSourceTypeConverter.delegate;
import static cn.taketoday.context.utils.OrderUtils.reversedSort;

/**
 * @author TODAY 2021/3/20 22:42
 * @since 3.0
 */
public class DefaultConversionService implements ConversionService {
  private static DefaultConversionService sharedInstance = new DefaultConversionService();

  private TypeConverter[] converters;

  static {
    sharedInstance.registerDefaultConverters();
  }

  public void registerDefaultConverters() {
    setConverters(new StringSourceEnumConverter(),
                  new StringSourceResourceConverter(),
                  new PrimitiveClassConverter(),
                  ConverterTypeConverter.getSharedInstance(),
                  delegate((c) -> c == MimeType.class, MimeType::valueOf),
                  delegate((c) -> c == MediaType.class, MediaType::valueOf),
                  new StringSourceConstructorConverter(),
                  delegate((c) -> c == Class.class, source -> {
                    try {
                      return Class.forName(source);
                    }
                    catch (ClassNotFoundException e) {
                      throw new ConversionException(e);
                    }
                  }),
                  delegate((c) -> c == DataSize.class, DataSize::parse),
                  delegate((c) -> c == Charset.class, Charset::forName),
                  delegate((c) -> c == Duration.class, ConvertUtils::parseDuration),
                  delegate((c) -> c == Boolean.class || c == boolean.class, Boolean::parseBoolean),
                  new ArrayToCollectionConverter(),
                  new ArrayStringArrayConverter(),
                  new StringSourceArrayConverter(),
                  new ArraySourceToSingleConverter()//
    );
  }

  /**
   * Get Target {@link TypeConverter}
   *
   * @param source
   *         input source
   * @param targetClass
   *         convert to target class
   *
   * @return TypeConverter
   */
  public TypeConverter getConverter(Object source, Class<?> targetClass) {
    for (TypeConverter converter : getConverters()) {
      if (converter.supports(targetClass, source)) {
        return converter;
      }
    }
    return null;
  }

  public TypeConverter[] getConverters() {
    return converters;
  }

  public void setConverters(TypeConverter... cts) {
    Assert.notNull(cts, "TypeConverter must not be null");
    synchronized (ConvertUtils.class) {
      converters = reversedSort(cts);
    }
  }

  @Override
  public boolean canConvert(Object source, Class<?> targetClass) {
    return getConverter(source, targetClass) != null;
  }

  @Override
  public Object convert(Object source, Class<?> targetClass) {
    if (source == null) {
      return null;
    }
    Assert.notNull(targetClass, "targetClass must not be null");
    if (targetClass.isInstance(source)) {
      return source;
    }
    final TypeConverter typeConverter = getConverter(source, targetClass);
    if (typeConverter == null) {
      throw new ConversionException(
              "There isn't a 'cn.taketoday.context.conversion.TypeConverter' to convert: ["
                      + source + "] '" + source.getClass() + "' to target class: [" + targetClass + "]");
    }
    return typeConverter.convert(targetClass, source);
  }

  /**
   * Add {@link TypeConverter} to {@link #converters}
   *
   * @param converters
   *         {@link TypeConverter} object
   *
   * @since 2.1.6
   */
  public void addConverter(TypeConverter... converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      final List<TypeConverter> typeConverters = new ArrayList<>();
      Collections.addAll(typeConverters, converters);
      addConverter(typeConverters);
    }
  }

  /**
   * Add a list of {@link TypeConverter} to {@link #converters}
   *
   * @param converters
   *         {@link TypeConverter} object
   *
   * @since 2.1.6
   */
  public void addConverter(List<TypeConverter> converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      if (getConverters() != null) {
        Collections.addAll(converters, getConverters());
      }
      setConverters(converters.toArray(new TypeConverter[converters.size()]));
    }
  }

  // static

  public static void setSharedInstance(DefaultConversionService sharedInstance) {
    DefaultConversionService.sharedInstance = sharedInstance;
  }

  public static DefaultConversionService getSharedInstance() {
    return sharedInstance;
  }

  // TypeConverter

  /**
   * @author TODAY 2019-06-06 15:50
   * @since 2.1.6
   */
  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class StringSourceResourceConverter extends StringSourceTypeConverter {

    @Override
    public boolean supports(Class<?> targetClass) {
      return targetClass == Resource.class
              || targetClass == URI.class
              || targetClass == URL.class
              || targetClass == File.class
              || targetClass == Resource[].class;
    }

    @Override
    protected Object convertInternal(Class<?> targetClass, String source) {

      try {
        if (targetClass == Resource[].class) {
          return ResourceUtils.getResources(source);
        }
        final Resource resource = ResourceUtils.getResource(source);
        if (targetClass == File.class) {
          return resource.getFile();
        }
        if (targetClass == URL.class) {
          return resource.getLocation();
        }
        if (targetClass == URI.class) {
          return resource.getLocation().toURI();
        }
        return resource;
      }
      catch (Throwable e) {
        throw new ConversionException(e);
      }
    }
  }

  /**
   * @author TODAY <br>
   * 2019-06-06 15:50
   * @since 2.1.6
   */
  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class StringSourceEnumConverter extends StringSourceTypeConverter {

    @Override
    public boolean supports(Class<?> targetClass) {
      return targetClass.isEnum();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object convertInternal(Class<?> targetClass, String source) {
      return Enum.valueOf((Class<Enum>) targetClass, source);
    }
  }

  /**
   * @author TODAY <br>
   * 2019-06-06 15:50
   * @since 2.1.6
   */
  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class StringSourceArrayConverter extends StringSourceTypeConverter {

    @Override
    public boolean supports(Class<?> targetClass) {
      return targetClass.isArray();
    }

    @Override
    protected Object convertInternal(Class<?> targetClass, String source) {
      final Class<?> componentType = targetClass.getComponentType();

      final String[] split = StringUtils.split(source);

      final Object arrayValue = Array.newInstance(componentType, split.length);
      for (int i = 0; i < split.length; i++) {
        Array.set(arrayValue, i, ConvertUtils.convert(split[i], componentType));
      }
      return arrayValue;
    }
  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class ArrayToCollectionConverter implements TypeConverter {

    @Override
    public boolean supports(Class<?> targetClass, Object source) {
      return CollectionUtils.isCollection(targetClass) && source.getClass().isArray();
    }

    @Override
    public Object convert(Class<?> targetClass, Object source) {
      final int length = Array.getLength(source);
      final Collection<Object> ret = CollectionUtils.createCollection(targetClass, length);
      for (int i = 0; i < length; i++) {
        ret.add(Array.get(source, i));
      }
      return ret;
    }
  }

  /**
   * @since 3.0
   */
  @Order(Ordered.LOWEST_PRECEDENCE - Ordered.HIGHEST_PRECEDENCE)
  static class ArraySourceToSingleConverter implements TypeConverter {

    @Override
    public boolean supports(Class<?> targetClass, Object source) {
      return !targetClass.isArray() && source.getClass().isArray() && Array.getLength(source) > 0;
    }

    @Override
    public Object convert(Class<?> targetClass, Object source) {
      final Object content = Array.get(source, 0);
      return ConvertUtils.convert(content, targetClass);
    }
  }

  /**
   * @author TODAY <br>
   * 2019-07-20 00:54
   * @since 2.1.6
   */
  @Order(Ordered.HIGHEST_PRECEDENCE)
  static class ArrayStringArrayConverter implements TypeConverter {

    @Override
    public boolean supports(Class<?> targetClass, Object source) {
      return targetClass.isArray() && source.getClass().isArray();
    }

    @Override
    public Object convert(Class<?> targetClass, Object source) {
      final int length = Array.getLength(source);
      final Class<?> componentType = targetClass.getComponentType();
      final Object instance = Array.newInstance(componentType, length);
      for (int i = 0; i < length; i++) {
        final Object value = ConvertUtils.convert(Array.get(source, i), componentType);
        Array.set(instance, i, value);
      }
      return instance;
    }
  }

  /**
   * @author TODAY <br>
   * 2019-06-06 16:12
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  static class StringSourceConstructorConverter extends StringSourceTypeConverter {

    @Override
    public boolean supports(Class<?> targetClass) {
      try {
        targetClass.getDeclaredConstructor(String.class);
        return true;
      }
      catch (NoSuchMethodException e) {
        return false;
      }
    }

    @Override
    protected Object convertInternal(Class<?> targetClass, String source) {
      try {
        return ReflectionUtils.accessibleConstructor(targetClass, String.class)
                .newInstance(source);
      }
      catch (Throwable e) {
        throw new ConversionException(e);
      }
    }
  }

  /**
   * @author TODAY <br>
   * 2019-06-19 12:28
   */
  @Order(Ordered.LOWEST_PRECEDENCE)
  static class PrimitiveClassConverter implements TypeConverter {

    @Override
    public boolean supports(Class<?> targetClass, Object source) {

      if (targetClass.isArray()) {
        targetClass = targetClass.getComponentType();
      }

      return (targetClass == boolean.class && source instanceof Boolean) //
              || (targetClass == long.class && source instanceof Long)//
              || (targetClass == int.class && source instanceof Integer)//
              || (targetClass == float.class && source instanceof Float)//
              || (targetClass == short.class && source instanceof Short)//
              || (targetClass == double.class && source instanceof Double)//
              || (targetClass == char.class && source instanceof Character)//
              || (targetClass == byte.class && source instanceof Byte);
    }

    @Override
    public Object convert(Class<?> targetClass, Object source) {
      return source; // auto convert
    }
  }

}
