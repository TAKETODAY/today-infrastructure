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

package cn.taketoday.web.support;

import cn.taketoday.core.TypeDescriptor;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.web.MessageBodyConverter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.MethodParameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author TODAY 2021/9/4 22:49
 */
public class JacksonMessageBodyConverter extends MessageBodyConverter {
  private ObjectMapper mapper;

  public JacksonMessageBodyConverter() {
    this(new ObjectMapper());
  }

  public JacksonMessageBodyConverter(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  protected void writeInternal(RequestContext context, Object noneNullMessage) throws IOException {
    obtainMapper().writeValue(context.getOutputStream(), noneNullMessage);
  }

  @Override
  public Object read(RequestContext context, MethodParameter parameter) throws IOException {
    final ObjectMapper mapper = obtainMapper();
    final JsonNode body = mapper.readTree(context.getInputStream());
    return readInternal(mapper, body, parameter);
  }

  @Override
  public Object read(String message, MethodParameter parameter) throws IOException {
    final ObjectMapper mapper = obtainMapper();
    final JsonNode body = mapper.readTree(message);
    return readInternal(mapper, body, parameter);
  }

  private Object readInternal(
          ObjectMapper mapper, JsonNode body, MethodParameter parameter) throws JsonProcessingException {
    if (body != null) {
      // Json node
      if (parameter.is(JsonNode.class)) {
        return body;
      }

      // style: [{"name":"today","age":21},{"name":"YHJ","age":22}]
      if (body.isArray()) {
        if (parameter.isCollection()) {
          final Collection<Object> ret = CollectionUtils.createCollection(parameter.getParameterClass(), body.size());
          final Class<?> valueType = getCollectionValueType(parameter.getTypeDescriptor());
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
        // fallback to first one
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
    return null;
  }

  protected Class<?> getCollectionValueType(TypeDescriptor parameter) {
    TypeDescriptor valueType = parameter.getGeneric(Collection.class);
    if (valueType == null || valueType.is(Object.class)) {
      throw new UnsupportedOperationException("Not support " + parameter);
    }
    return valueType.getType();
  }

  /**
   * @return {@link ObjectMapper} must not be null
   */
  private ObjectMapper obtainMapper() {
    final ObjectMapper mapper = getMapper();
    Assert.state(mapper != null, "No ObjectMapper.");
    return mapper;
  }

  /**
   * Set a ObjectMapper
   */
  public void setMapper(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public ObjectMapper getMapper() {
    return mapper;
  }

}
