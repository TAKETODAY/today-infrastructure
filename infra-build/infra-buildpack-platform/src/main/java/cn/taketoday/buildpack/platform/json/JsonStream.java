/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility class that allows JSON to be parsed and processed as it's received.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class JsonStream {

  private final ObjectMapper objectMapper;

  /**
   * Create a new {@link JsonStream} backed by the given object mapper.
   *
   * @param objectMapper the object mapper to use
   */
  public JsonStream(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Stream {@link ObjectNode object nodes} from the content as they become available.
   *
   * @param content the source content
   * @param consumer the {@link ObjectNode} consumer
   * @throws IOException on IO error
   */
  public void get(InputStream content, Consumer<ObjectNode> consumer) throws IOException {
    get(content, ObjectNode.class, consumer);
  }

  /**
   * Stream objects from the content as they become available.
   *
   * @param <T> the object type
   * @param content the source content
   * @param type the object type
   * @param consumer the {@link ObjectNode} consumer
   * @throws IOException on IO error
   */
  public <T> void get(InputStream content, Class<T> type, Consumer<T> consumer) throws IOException {
    JsonFactory jsonFactory = this.objectMapper.getFactory();
    JsonParser parser = jsonFactory.createParser(content);
    while (!parser.isClosed()) {
      JsonToken token = parser.nextToken();
      if (token != null && token != JsonToken.END_OBJECT) {
        T node = read(parser, type);
        if (node != null) {
          consumer.accept(node);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T read(JsonParser parser, Class<T> type) throws IOException {
    if (ObjectNode.class.isAssignableFrom(type)) {
      ObjectNode node = this.objectMapper.readTree(parser);
      if (node == null || node.isMissingNode() || node.isEmpty()) {
        return null;
      }
      return (T) node;
    }
    return this.objectMapper.readValue(parser, type);
  }

}
