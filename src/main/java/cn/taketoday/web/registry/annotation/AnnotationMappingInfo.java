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

package cn.taketoday.web.registry.annotation;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.IntFunction;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.OrderUtils;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.InvalidMediaTypeException;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.util.pattern.PathMatchInfo;
import cn.taketoday.web.util.pattern.PathPattern;

/**
 * @author TODAY 2021/4/21 23:57
 * @since 3.0
 */
final class AnnotationMappingInfo implements Ordered {

  private final String[] path;

  private final MediaType[] produces;
  private final MediaType[] consumes;
  private final HttpMethod[] method;
  private final RequestParameter[] params;

  private final ActionMappingAnnotationHandler handler;

  // it's pathPattern
  private final PathPattern pathPattern;

  /**
   * @throws InvalidMediaTypeException if the media type (consumes) value cannot be parsed
   */
  AnnotationMappingInfo(String[] path, String[] produces, String[] consumes,
                        String[] params, HttpMethod[] method, ActionMappingAnnotationHandler handler,
                        PathPattern pathPattern) {
    this.path = path;
    this.handler = handler;
    this.pathPattern = pathPattern;
    this.method = compute(method);
    this.produces = compute(produces, MediaType::valueOf, MediaType[]::new);
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
   * @throws InvalidMediaTypeException if the media type (consumes) value cannot be parsed
   */
  AnnotationMappingInfo(ActionMapping mapping, ActionMappingAnnotationHandler handler) {
    this(mapping.path(), mapping.produces(), mapping.consumes(),
            mapping.params(), mapping.method(), handler, null);
  }

  /**
   * @throws InvalidMediaTypeException if the media type (consumes) value cannot be parsed
   */
  AnnotationMappingInfo(PathPattern pathPattern, AnnotationAttributes attributes, ActionMappingAnnotationHandler handler) {
    this(attributes.getStringArray("path"), attributes.getStringArray("produces"),
            attributes.getStringArray("consumes"), attributes.getStringArray("params"),
            attributes.getRequiredAttribute("method", HttpMethod[].class), handler, pathPattern);
  }

  public AnnotationMappingInfo(AnnotationMappingInfo mapping, ActionMappingAnnotationHandler handler) {
    this.handler = handler;
    this.path = mapping.path;
    this.params = mapping.params;
    this.method = mapping.method;
    this.produces = mapping.produces;
    this.consumes = mapping.consumes;
    this.pathPattern = mapping.pathPattern;
  }

  //

  // @since 4.0
  @Nullable
  public PathMatchInfo matches(PathContainer pathContainer) {
    return pathPattern.matchAndExtract(pathContainer);
  }

  //

  public ActionMappingAnnotationHandler getHandler() {
    return handler;
  }

  public PathPattern getPathPattern() {
    return pathPattern;
  }

  public String[] path() {
    return path;
  }

  public HttpMethod[] method() {
    return method;
  }

  public MediaType[] consumes() {
    return consumes;
  }

  public RequestParameter[] params() {
    return params;
  }

  public MediaType[] produces() {
    return produces;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();

    appendString(builder, Constant.BLANK, method);
    appendString(builder, "consumes: ", consumes);
    appendString(builder, "produces: ", produces);
    appendString(builder, "params: ", params);
    builder.append(" ")
            .append(handler);
    return builder.toString();
  }

  private static void appendString(StringBuilder builder, String prefix, Object[] array) {
    if (array != null) {
      builder.append(prefix)
              .append(Arrays.toString(array));
    }
  }

  // order

  @Override
  public int getOrder() { // FIXME
    final HandlerMethod method = handler.getMethod();
    final int handlerOrder = OrderUtils.getOrder(method.getMethod());
    final int paramsOrder = params == null ? 0 : params.length;
    final int consumesOrder = consumes == null ? 0 : consumes.length;
    return handlerOrder + consumesOrder + paramsOrder;
  }
}
