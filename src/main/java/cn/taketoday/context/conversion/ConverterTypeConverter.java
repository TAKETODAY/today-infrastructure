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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.GenericTypeResolver;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;

/**
 * @author TODAY 2021/1/6 21:44
 * @since 3.0
 */
public class ConverterTypeConverter
        extends OrderedSupport implements TypeConverter {
  private static final ConverterTypeConverter sharedInstance = new ConverterTypeConverter();

  static {
    registerDefaultConverters(sharedInstance);
    sharedInstance.setOrder(HIGHEST_PRECEDENCE + 1);
  }

  private final HashMap<Class<?>, List<GenericConverter>> converterMap = new HashMap<>();

  @Override
  public boolean supports(final Class<?> targetClass, final Object source) {
    final List<GenericConverter> converters = getGenericConverters(targetClass);
    if (!CollectionUtils.isEmpty(converters)) {
      for (final GenericConverter converter : converters) {
        if (converter.supports(source)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public Object convert(final Class<?> targetClass, final Object source) {
    final List<GenericConverter> converters = getGenericConverters(targetClass);
    if (!CollectionUtils.isEmpty(converters)) {
      for (final GenericConverter converter : converters) {
        if (converter.supports(source)) {
          return converter.convert(source);
        }
      }
    }
    throw new ConversionException(
            "There isn't a 'cn.taketoday.context.conversion.Converter' to convert: ["
                    + source + "] '" + source.getClass() + "' to target class: [" + targetClass + "]");
  }

  protected List<GenericConverter> getGenericConverters(Class<?> targetClass) {
    final List<GenericConverter> ret = converterMap.get(targetClass);
    return CollectionUtils.isEmpty(ret)
           ? converterMap.get(Object.class)
           : ret;
  }

  public void addConverter(Converter<?, ?> converter) {
    if (converter instanceof TypeCapable) {
      addConverter(((TypeCapable) converter).getType(), converter);
    }
    else {
      Assert.notNull(converter, "converter must not be null");
      final Class<?>[] generics = GenericTypeResolver.resolveTypeArguments(converter.getClass(), Converter.class);
      if (ObjectUtils.isNotEmpty(generics)) {
        final Class<?> targetClass = generics[1];
        final Class<?> sourceClass = generics[0];
        addConverter(targetClass, sourceClass, converter);
      }
      else {
        throw new ConfigurationException("can't register get converter's target class");
      }
    }
  }

  public void addConverters(Converter<?, ?>... converters) {
    if (ObjectUtils.isNotEmpty(converters)) {
      for (final Converter<?, ?> converter : converters) {
        addConverter(converter);
      }
    }
  }

  public void addConverter(Class<?> targetClass, Converter<?, ?> converter) {
    Assert.notNull(converter, "converter must not be null");
    final Class<?>[] generics = GenericTypeResolver.resolveTypeArguments(converter.getClass(), Converter.class);
    if (ObjectUtils.isNotEmpty(generics)) {
      addConverter(targetClass, generics[0], converter);
    }
    else
      throw new ConfigurationException("can't register get converter's source class");
  }

  public void addConverter(Class<?> targetClass, Class<?> sourceClass, Converter<?, ?> converter) {
    Assert.notNull(converter, "converter must not be null");
    Assert.notNull(targetClass, "targetClass must not be null");
    Assert.notNull(sourceClass, "sourceClass must not be null");

    List<GenericConverter> converters =
            converterMap.computeIfAbsent(targetClass, s -> new LinkedList<>());
    converters.add(new GenericConverter(sourceClass, converter));
    // order support
    OrderUtils.reversedSort(converters);
  }

  public Map<Class<?>, List<GenericConverter>> getConverterMap() {
    return converterMap;
  }

  public static void registerDefaultConverters(ConverterTypeConverter converter) {
    converter.addConverters(
            new IntegerConverter(int.class),
            new IntegerConverter(Integer.class),
            new LongConverter(Long.class),
            new LongConverter(long.class),
            new DoubleConverter(Double.class),
            new DoubleConverter(double.class),
            new FloatConverter(float.class),
            new FloatConverter(Float.class),
            new ByteConverter(Byte.class),
            new ByteConverter(byte.class),
            new ShortConverter(short.class),
            new ShortConverter(Short.class),
            new BigDecimalConverter(BigDecimal.class),

            new ClassConverter(),
            new CharsetConverter(),
            new DurationConverter(),
            new DataSizeConverter(),
            new MimeTypeConverter(),
            new MediaTypeConverter()

    );
  }

  public static ConverterTypeConverter getSharedInstance() {
    return sharedInstance;
  }

}
