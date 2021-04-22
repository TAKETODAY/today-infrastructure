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

package cn.taketoday.web.annotation;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.utils.InvalidMediaTypeException;
import cn.taketoday.context.utils.MediaType;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.handler.HandlerMethod;
import lombok.Setter;

/**
 * @author TODAY 2021/4/21 23:57
 * @since 3.0
 */
@Setter
public class MappingInfo {

  private final String[] value;
  private final String[] produces;

  private final MediaType[] consumes;
  private final RequestParameter[] params;
  private final RequestMethod[] method;

  private final HandlerMethod handler;

  public MappingInfo(String[] value, String[] produces, String[] consumes,
                     String[] params, RequestMethod[] method, HandlerMethod handler) {
    this.value = value;
    this.produces = produces;
    this.handler = handler;
    if (ObjectUtils.isNotEmpty(consumes)) {
      MediaType[] mediaTypes = new MediaType[consumes.length];
      int i = 0;
      for (final String consume : consumes) {
        mediaTypes[i++] = MediaType.valueOf(consume);
      }
      this.consumes = mediaTypes;
    }
    else {
      this.consumes = null;
    }
    if (ObjectUtils.isNotEmpty(params)) {
      int i = 0;
      RequestParameter[] parameters = new RequestParameter[params.length];
      for (final String param : params) {
        parameters[i++] = RequestParameter.parse(param);
      }
      this.params = parameters;
    }
    else {
      this.params = null;
    }

    if (ObjectUtils.isNotEmpty(method)) {
      this.method = method;
    }
    else {
      this.method = null;
    }
  }

  /**
   * @throws InvalidMediaTypeException
   *         if the media type (consumes) value cannot be parsed
   */
  public MappingInfo(ActionMapping mapping, HandlerMethod handler) {
    this(mapping.value(), mapping.produces(), mapping.consumes(),
         mapping.params(), mapping.method(), handler);
  }

  public MappingInfo(AnnotationAttributes attributes, HandlerMethod handler) {
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
}
