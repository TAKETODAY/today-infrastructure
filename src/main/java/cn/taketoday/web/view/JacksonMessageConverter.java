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

package cn.taketoday.web.view;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.condition.ConditionalOnClass;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import cn.taketoday.web.resolver.RequestBodyParsingException;

/**
 * support {@link JsonNode} {@link Collection}, POJO, Array
 *
 * @author TODAY 2021/3/10 11:36
 * @since 3.0
 */
@MissingBean
@ConditionalOnClass("com.fasterxml.jackson.databind.ObjectMapper")
public class JacksonMessageConverter
        extends AbstractMessageConverter implements MessageConverter {

  private ObjectMapper mapper; //TODO config object mapper

  public JacksonMessageConverter() {
    this(new ObjectMapper());
  }

  public JacksonMessageConverter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  void writeInternal(RequestContext context, Object noneNullMessage) throws IOException {
    mapper.writeValue(context.getOutputStream(), noneNullMessage);
  }

  @Override
  public Object read(RequestContext context, MethodParameter parameter) throws IOException {
    final ObjectMapper mapper = obtainMapper();

    final JsonNode body = getBody(context, mapper);
    if (body != null) {
      // Json node
      if (parameter.is(JsonNode.class)) {
        return body;
      }

      try {
        // style: [{"name":"today","age":21},{"name":"YHJ","age":22}]
        if (body.isArray()) {
          if (parameter.isCollection()) {
            final Collection<Object> ret = CollectionUtils.createCollection(parameter.getParameterClass(), body.size());
            final Class<?> valueType = getValueType(parameter);
            for (final JsonNode node : body) {
              final Object value = mapper.treeToValue(node, valueType);
              ret.add(value);
            }
            return ret;
          }
          if (parameter.isArray()) {
            List<Object> objects = new ArrayList<>();
            final Class<?> valueType = parameter.getComponentType();
            for (final JsonNode node : body) {
              final Object value = mapper.treeToValue(node, valueType);
              objects.add(value);
            }

            final Object[] original = objects.toArray();
            final Object ret = Array.newInstance(valueType, objects.size());
            System.arraycopy(original, 0, ret, 0, objects.size());
            return ret;
          }
          final JsonNode jsonNode = body.get(0);
          if (jsonNode != null) {
            return mapper.treeToValue(jsonNode, parameter.getParameterClass());
          }
          // null
        }
        else {
          // TODO 类型判断
          return mapper.treeToValue(body, parameter.getParameterClass());
        }
      }
      catch (JsonProcessingException e) {
        throw new RequestBodyParsingException("Request body read failed", e);
      }
    }
    return null;
  }

  protected Class<?> getValueType(MethodParameter parameter) {
    final Type generics = parameter.getGenerics(0);
    if (generics instanceof Class) {
      return (Class<?>) generics;
    }
    throw new ConfigurationException("Not support " + parameter);
  }

  JsonNode getBody(RequestContext context, ObjectMapper mapper) throws IOException {
    final Object body = context.requestBody();
    if (body == null) {
      try {
        final JsonNode jsonNode = mapper.readTree(context.getInputStream());
        // cache json node
        context.requestBody(jsonNode);
        return jsonNode;
      }
      catch (JsonParseException e) {
        throw new RequestBodyParsingException("Request body read failed", e);
      }
    }
    else if (body instanceof JsonNode) {
      return (JsonNode) body;
    }
    return null;
  }

  /**
   * @return {@link ObjectMapper} must not be null
   */
  ObjectMapper obtainMapper() {
    final ObjectMapper mapper = getMapper();
    Assert.state(mapper != null, "No ObjectMapper.");
    return mapper;
  }

  public void setMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public ObjectMapper getMapper() {
    return mapper;
  }
}
