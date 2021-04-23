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

package cn.taketoday.web.registry;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.IntFunction;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Ordered;
import cn.taketoday.context.utils.InvalidMediaTypeException;
import cn.taketoday.context.utils.MediaType;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.handler.HandlerMethod;
import lombok.Setter;

/**
 * @author TODAY 2021/4/21 23:57
 * @since 3.0
 */
@Setter
public class MappingInfo implements Ordered {

  private final String[] value;
  private final String[] produces;

  private final MediaType[] consumes;
  private final RequestParameter[] params;
  private final RequestMethod[] method;

  private final HandlerMethod handler;

  /**
   * @throws InvalidMediaTypeException
   *         if the media type (consumes) value cannot be parsed
   */
  MappingInfo(String[] value, String[] produces, String[] consumes,
              String[] params, RequestMethod[] method, HandlerMethod handler) {
    this.value = value;
    this.handler = handler;
    this.method = compute(method);
    this.produces = compute(produces);
    this.consumes = compute(consumes, MediaType::valueOf, MediaType[]::new);
    this.params = compute(params, RequestParameter::parse, RequestParameter[]::new);
  }

  private static <T, R> R[] compute(T[] values, Function<T, R> converter, IntFunction<R[]> array) {
    if (ObjectUtils.isNotEmpty(values)) {
      final R[] ret = array.apply(values.length);
      int i = 0;
      for (final T value : values) {
        ret[i++] = converter.apply(value);
      }
      return ret;
    }
    return null;
  }

  private static <T> T[] compute(T[] values) {
    if (ObjectUtils.isNotEmpty(values)) {
      return values;
    }
    return null;
  }

  /**
   * @throws InvalidMediaTypeException
   *         if the media type (consumes) value cannot be parsed
   */
  MappingInfo(ActionMapping mapping, HandlerMethod handler) {
    this(mapping.value(), mapping.produces(), mapping.consumes(),
         mapping.params(), mapping.method(), handler);
  }

  /**
   * @throws InvalidMediaTypeException
   *         if the media type (consumes) value cannot be parsed
   */
  MappingInfo(AnnotationAttributes attributes, HandlerMethod handler) {
    this(attributes.getStringArray(Constant.VALUE), attributes.getStringArray("produces"),
         attributes.getStringArray("consumes"), attributes.getStringArray("params"),
         attributes.getAttribute("method", RequestMethod[].class), handler);
  }

  public MappingInfo(MappingInfo mapping, HandlerMethod handler) {
    this.handler = handler;
    this.value = mapping.value;
    this.params = mapping.params;
    this.method = mapping.method;
    this.produces = mapping.produces;
    this.consumes = mapping.consumes;
  }

  public HandlerMethod getHandler() {
    return handler;
  }

  public String[] value() {
    return value;
  }

  public RequestMethod[] method() {
    return method;
  }

  public MediaType[] consumes() {
    return consumes;
  }

  public RequestParameter[] params() {
    return params;
  }

  public String[] produces() {
    return produces;
  }

  @Override
  public String toString() {
    return handler.toString();
  }

  // order

  @Override
  public int getOrder() {
    final Method method = handler.getMethod();
    final int handlerOrder = OrderUtils.getOrder(method);
    final int paramsOrder = params == null ? 0 : params.length;
    final int consumesOrder = consumes == null ? 0 : consumes.length;
    return handlerOrder + consumesOrder + paramsOrder;
  }
}
