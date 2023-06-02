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

package cn.taketoday.framework.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import cn.taketoday.lang.Assert;

/**
 * Helper base class for {@link JsonDeserializer} implementations that deserialize
 * objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @see JsonObjectSerializer
 * @since 4.0
 */
public abstract class JsonObjectDeserializer<T> extends JsonDeserializer<T> {

  @Override
  public final T deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
    try {
      ObjectCodec codec = jp.getCodec();
      JsonNode tree = codec.readTree(jp);
      return deserializeObject(jp, ctxt, codec, tree);
    }
    catch (Exception ex) {
      if (ex instanceof IOException) {
        throw (IOException) ex;
      }
      throw new JsonMappingException(jp, "Object deserialize error", ex);
    }
  }

  /**
   * Deserialize JSON content into the value type this serializer handles.
   *
   * @param jsonParser the source parser used for reading JSON content
   * @param context context that can be used to access information about this
   * deserialization activity
   * @param codec the {@link ObjectCodec} associated with the parser
   * @param tree deserialized JSON content as tree expressed using set of
   * {@link TreeNode} instances
   * @return the deserialized object
   * @throws IOException on error
   * @see #deserialize(JsonParser, DeserializationContext)
   */
  protected abstract T deserializeObject(
          JsonParser jsonParser, DeserializationContext context, ObjectCodec codec, JsonNode tree)
          throws IOException;

  /**
   * Helper method to extract a value from the given {@code jsonNode} or return
   * {@code null} when the node itself is {@code null}.
   *
   * @param jsonNode the source node (may be {@code null})
   * @param type the data type. May be {@link String}, {@link Boolean}, {@link Long},
   * {@link Integer}, {@link Short}, {@link Double}, {@link Float}, {@link BigDecimal}
   * or {@link BigInteger}.
   * @param <D> the data type requested
   * @return the node value or {@code null}
   */
  @SuppressWarnings({ "unchecked" })
  protected final <D> D nullSafeValue(JsonNode jsonNode, Class<D> type) {
    Assert.notNull(type, "Type must not be null");
    if (jsonNode == null) {
      return null;
    }
    if (type == String.class) {
      return (D) jsonNode.textValue();
    }
    if (type == Boolean.class) {
      return (D) Boolean.valueOf(jsonNode.booleanValue());
    }
    if (type == Long.class) {
      return (D) Long.valueOf(jsonNode.longValue());
    }
    if (type == Integer.class) {
      return (D) Integer.valueOf(jsonNode.intValue());
    }
    if (type == Short.class) {
      return (D) Short.valueOf(jsonNode.shortValue());
    }
    if (type == Double.class) {
      return (D) Double.valueOf(jsonNode.doubleValue());
    }
    if (type == Float.class) {
      return (D) Float.valueOf(jsonNode.floatValue());
    }
    if (type == BigDecimal.class) {
      return (D) jsonNode.decimalValue();
    }
    if (type == BigInteger.class) {
      return (D) jsonNode.bigIntegerValue();
    }
    throw new IllegalArgumentException("Unsupported value type " + type.getName());
  }

  /**
   * Helper method to return a {@link JsonNode} from the tree.
   *
   * @param tree the source tree
   * @param fieldName the field name to extract
   * @return the {@link JsonNode}
   */
  protected final JsonNode getRequiredNode(JsonNode tree, String fieldName) {
    Assert.notNull(tree, "Tree must not be null");
    JsonNode node = tree.get(fieldName);
    Assert.state(node != null && !(node instanceof NullNode), () -> "Missing JSON field '" + fieldName + "'");
    return node;
  }

}
