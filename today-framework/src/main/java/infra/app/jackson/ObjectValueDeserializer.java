/*
 * Copyright 2017 - 2026 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.jackson;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

import infra.lang.Assert;
import tools.jackson.core.JsonParser;
import tools.jackson.core.TreeNode;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.NullNode;

/**
 * Helper base class for {@link ValueDeserializer} implementations that deserialize
 * objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see ObjectValueSerializer
 * @since 4.0
 */
public abstract class ObjectValueDeserializer<T> extends ValueDeserializer<T> {

  @Override
  public final T deserialize(JsonParser jp, DeserializationContext ctxt) {
    TreeNode tree = jp.readValueAsTree();
    if (tree instanceof JsonNode jsonNode) {
      return deserializeObject(jp, ctxt, jsonNode);
    }
    throw new IllegalStateException(
            "JsonParser " + jp + " produced " + tree.getClass() + " that is not a JsonNode");
  }

  /**
   * Deserialize JSON content into the value type this serializer handles.
   *
   * @param jsonParser the source parser used for reading JSON content
   * @param context context that can be used to access information about this
   * deserialization activity
   * @param tree deserialized JSON content as tree expressed using set of
   * {@link TreeNode} instances
   * @return the deserialized object
   * @see #deserialize(JsonParser, DeserializationContext)
   */
  protected abstract T deserializeObject(JsonParser jsonParser, DeserializationContext context, JsonNode tree);

  /**
   * Helper method to extract a value from the given {@code jsonNode} or return
   * {@code null} when the node itself is {@code null}.
   *
   * @param jsonNode the source node (may be {@code null})
   * @param type the data type. May be {@link String}, {@link Boolean}, {@link Long},
   * {@link Integer}, {@link Short}, {@link Double}, {@link Float}, {@link BigDecimal}
   * or {@link BigInteger}.
   * @param <D> the data type requested
   * @param <R> the result type
   * @param mapper a mapper to convert the value when it is not {@code null}
   * @return the node value or {@code null}
   */
  protected final <D, R> @Nullable R nullSafeValue(@Nullable JsonNode jsonNode, Class<D> type,
          Function<D, R> mapper) {
    D value = nullSafeValue(jsonNode, type);
    return (value != null) ? mapper.apply(value) : null;
  }

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
  protected final <D> @Nullable D nullSafeValue(@Nullable JsonNode jsonNode, Class<D> type) {
    Assert.notNull(type, "'type' must not be null");
    if (jsonNode == null) {
      return null;
    }
    if (type == String.class) {
      return (D) jsonNode.stringValue();
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
    Assert.notNull(tree, "'tree' must not be null");
    JsonNode node = tree.get(fieldName);
    Assert.state(node != null && !(node instanceof NullNode), () -> "Missing JSON field '" + fieldName + "'");
    return node;
  }

}
