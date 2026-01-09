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

import java.util.List;
import java.util.Map;

import infra.util.ClassUtils;

/**
 * Parser that can read JSON formatted strings into {@link Map}s or {@link List}s.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JsonParser#lookup()
 * @see BasicJsonParser
 * @see JacksonJsonParser
 * @see GsonJsonParser
 * @since 4.0
 */
public interface JsonParser {

  /**
   * Parse the specified JSON string into a Map.
   *
   * @param json the JSON to parse
   * @return the parsed JSON as a map
   * @throws JsonParseException if the JSON cannot be parsed
   */
  Map<String, Object> parseMap(String json) throws JsonParseException;

  /**
   * Parse the specified JSON string into a List.
   *
   * @param json the JSON to parse
   * @return the parsed JSON as a list
   * @throws JsonParseException if the JSON cannot be parsed
   */
  List<Object> parseList(String json) throws JsonParseException;

  /**
   * Static factory for the "best" JSON parser available on the classpath. Tries
   * Jackson, then Gson, Snake YAML, and then falls back to the {@link BasicJsonParser}.
   *
   * @return a {@link JsonParser}
   */
  static JsonParser lookup() {
    if (ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", JsonParser.class)) {
      return new JacksonJsonParser();
    }
    if (ClassUtils.isPresent("com.google.gson.Gson", JsonParser.class)) {
      return new GsonJsonParser();
    }
    return new BasicJsonParser();
  }

}
