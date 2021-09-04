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

package cn.taketoday.web.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.core.Assert;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.GenericDescriptor;
import cn.taketoday.web.ObjectNotationProcessor;

/**
 * jackson {@link ObjectNotationProcessor} implementation
 *
 * @author TODAY 2021/5/17 13:24
 * @see ObjectMapper
 * @since 3.0.1
 */
public class JacksonObjectNotationProcessor extends ObjectNotationProcessor {
  private ObjectMapper mapper;

  public JacksonObjectNotationProcessor() {
    this(new ObjectMapper());
  }

  public JacksonObjectNotationProcessor(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void write(Writer output, Object noneNullMessage) throws IOException {
    obtainMapper().writeValue(output, noneNullMessage);
  }

  /**
   * @throws JsonParseException
   *         if underlying input contains invalid content
   *         of type {@link JsonParser} supports (JSON for default case)
   */
  @Override
  public Object read(String message, GenericDescriptor descriptor) throws IOException {
    final ObjectMapper mapper = obtainMapper();
    final JsonNode body = mapper.readTree(message);
    return readInternal(mapper, body, descriptor);
  }

  /**
   * @throws JsonParseException
   *         if underlying input contains invalid content
   *         of type {@link JsonParser} supports (JSON for default case)
   */
  @Override
  public Object read(InputStream source, GenericDescriptor descriptor) throws IOException {
    final ObjectMapper mapper = obtainMapper();
    final JsonNode body = mapper.readTree(source);
    return readInternal(mapper, body, descriptor);
  }

  private Object readInternal(
          ObjectMapper mapper, JsonNode body, GenericDescriptor descriptor) throws JsonProcessingException {
    if (body != null) {
      // Json node
      if (descriptor.is(JsonNode.class)) {
        return body;
      }

      // style: [{"name":"today","age":21},{"name":"YHJ","age":22}]
      if (body.isArray()) {
        if (descriptor.isCollection()) {
          final Collection<Object> ret = CollectionUtils.createCollection(descriptor.getType(), body.size());
          final Class<?> valueType = getCollectionValueType(descriptor);
          for (final JsonNode node : body) {
            final Object value = mapper.treeToValue(node, valueType);
            ret.add(value);
          }
          return ret;
        }
        if (descriptor.isArray()) {
          List<Object> objects = new ArrayList<>();
          final Class<?> valueType = descriptor.getComponentType();
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
          return mapper.treeToValue(jsonNode, descriptor.getType());
        }
        // null
      }
      else {
        // TODO 类型判断
        return mapper.treeToValue(body, descriptor.getType());
      }
    }
    return null;
  }

  protected Class<?> getCollectionValueType(GenericDescriptor parameter) {
    GenericDescriptor valueType = parameter.getGeneric(Collection.class);
    if (valueType.is(Object.class)) {
      throw new ConfigurationException("Not support " + parameter);
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
