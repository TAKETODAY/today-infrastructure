/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
