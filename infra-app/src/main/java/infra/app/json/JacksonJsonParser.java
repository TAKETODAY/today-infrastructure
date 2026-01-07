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

package infra.app.json;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Thin wrapper to adapt Jackson 2 {@link ObjectMapper} to {@link JsonParser}.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see JsonParser#lookup()
 * @since 4.0
 */
public class JacksonJsonParser extends AbstractJsonParser {

  private static final MapTypeReference MAP_TYPE = new MapTypeReference();

  private static final ListTypeReference LIST_TYPE = new ListTypeReference();

  private @Nullable JsonMapper jsonMapper; // Late binding

  /**
   * Creates an instance with the specified {@link JsonMapper}.
   *
   * @param jsonMapper the JSON mapper to use
   */
  public JacksonJsonParser(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  /**
   * Creates an instance with a default {@link JsonMapper} that is created lazily.
   */
  public JacksonJsonParser() {
  }

  @Override
  public Map<String, Object> parseMap(@Nullable String json) {
    return tryParse(() -> getJsonMapper().readValue(json, MAP_TYPE), Exception.class);
  }

  @Override
  public List<Object> parseList(@Nullable String json) {
    return tryParse(() -> getJsonMapper().readValue(json, LIST_TYPE), Exception.class);
  }

  private JsonMapper getJsonMapper() {
    if (this.jsonMapper == null) {
      this.jsonMapper = new JsonMapper();
    }
    return this.jsonMapper;
  }

  private static final class MapTypeReference extends TypeReference<Map<String, Object>> {

  }

  private static final class ListTypeReference extends TypeReference<List<Object>> {

  }

}
