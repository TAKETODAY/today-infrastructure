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

package cn.taketoday.framework.json;

import java.util.List;
import java.util.Map;

import cn.taketoday.util.ClassUtils;

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
    if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", JsonParser.class)) {
      return new JacksonJsonParser();
    }
    if (ClassUtils.isPresent("com.google.gson.Gson", JsonParser.class)) {
      return new GsonJsonParser();
    }
    return new BasicJsonParser();
  }

}
