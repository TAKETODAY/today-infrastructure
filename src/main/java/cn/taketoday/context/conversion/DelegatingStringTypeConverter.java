/**
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

import java.util.function.Function;

import cn.taketoday.context.Ordered;

/**
 * @author TODAY <br>
 * 2019-06-06 16:06
 * @since 2.1.6
 */
public class DelegatingStringTypeConverter<T> extends StringTypeConverter implements Ordered {

  private final int order;
  private final Converter<String, T> converter;
  private final Function<Class<?>, Boolean> supportsFunction;

  public DelegatingStringTypeConverter(Function<Class<?>, Boolean> supports, Converter<String, T> converter) {
    this(supports, converter, Ordered.HIGHEST_PRECEDENCE);
  }

  public DelegatingStringTypeConverter(Function<Class<?>, Boolean> supports,
                                       Converter<String, T> converter, int order) //
  {
    this.order = order;
    this.converter = converter;
    this.supportsFunction = supports;
  }

  @Override
  public int getOrder() {
    return order;
  }

  @Override
  public boolean supports(Class<?> targetClass) {
    return supportsFunction.apply(targetClass);
  }

  @Override
  protected Object convertInternal(Class<?> targetClass, String source) {
    return converter.convert(source);
  }

  public static <T> DelegatingStringTypeConverter<T> delegate(Function<Class<?>, Boolean> supports, //
                                                              Converter<String, T> converter) {
    return new DelegatingStringTypeConverter<>(supports, converter);
  }

  public static <T> DelegatingStringTypeConverter<T> delegate(Function<Class<?>, Boolean> supports, //
                                                              Converter<String, T> converter, int order) {
    return new DelegatingStringTypeConverter<>(supports, converter, order);
  }
}
