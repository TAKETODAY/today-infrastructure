/*
 * Copyright 2017 - 2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * Really basic JSON parser for when you have nothing else available. Comes with some
 * limitations with respect to the JSON specification (e.g. only supports String values),
 * so users will probably prefer to have a library handle things instead (Jackson or Snake
 * YAML are supported).
 *
 * @author Dave Syer
 * @author Jean de Klerk
 * @author Stephane Nicoll
 * @see JsonParser#lookup()
 * @since 4.0
 */
public class BasicJsonParser extends AbstractJsonParser {

  private static final int MAX_DEPTH = 1000;

  @Override
  public Map<String, Object> parseMap(String json) {
    return tryParse(() -> parseMap(json, (jsonToParse) -> parseMapInternal(0, jsonToParse)), Exception.class);
  }

  @Override
  public List<Object> parseList(String json) {
    return tryParse(() -> parseList(json, (jsonToParse) -> parseListInternal(0, jsonToParse)), Exception.class);
  }

  private List<Object> parseListInternal(int nesting, String json) {
    List<Object> list = new ArrayList<>();
    json = trimEdges(json, '[', ']').trim();
    for (String value : tokenize(json)) {
      list.add(parseInternal(nesting + 1, value));
    }
    return list;
  }

  private Object parseInternal(int nesting, String json) {
    if (nesting > MAX_DEPTH) {
      throw new IllegalStateException("JSON is too deeply nested");
    }
    if (json.startsWith("[")) {
      return parseListInternal(nesting + 1, json);
    }
    if (json.startsWith("{")) {
      return parseMapInternal(nesting + 1, json);
    }
    if (json.startsWith("\"")) {
      return trimEdges(json, '"', '"');
    }
    return parseNumber(json);
  }

  private Map<String, Object> parseMapInternal(int nesting, String json) {
    Map<String, Object> map = new LinkedHashMap<>();
    json = trimEdges(json, '{', '}').trim();
    for (String pair : tokenize(json)) {
      String[] values = StringUtils.trimArrayElements(StringUtils.split(pair, ":"));
      Assert.state(values[0].startsWith("\"") && values[0].endsWith("\""),
              "Expecting double-quotes around field names");
      String key = trimEdges(values[0], '"', '"');
      Object value = parseInternal(nesting, values[1]);
      map.put(key, value);
    }
    return map;
  }

  private Object parseNumber(String json) {
    try {
      return Long.valueOf(json);
    }
    catch (NumberFormatException ex) {
      try {
        return Double.valueOf(json);
      }
      catch (NumberFormatException ex2) {
        return json;
      }
    }
  }

  private static String trimTrailingCharacter(String string, char c) {
    if (!string.isEmpty() && string.charAt(string.length() - 1) == c) {
      return string.substring(0, string.length() - 1);
    }
    return string;
  }

  private static String trimLeadingCharacter(String string, char c) {
    if (!string.isEmpty() && string.charAt(0) == c) {
      return string.substring(1);
    }
    return string;
  }

  private static String trimEdges(String string, char leadingChar, char trailingChar) {
    return trimTrailingCharacter(trimLeadingCharacter(string, leadingChar), trailingChar);
  }

  private List<String> tokenize(String json) {
    List<String> list = new ArrayList<>();
    int index = 0;
    int inObject = 0;
    int inList = 0;
    boolean inValue = false;
    boolean inEscape = false;
    StringBuilder build = new StringBuilder();
    while (index < json.length()) {
      char current = json.charAt(index);
      if (inEscape) {
        build.append(current);
        index++;
        inEscape = false;
        continue;
      }
      if (current == '{') {
        inObject++;
      }
      if (current == '}') {
        inObject--;
      }
      if (current == '[') {
        inList++;
      }
      if (current == ']') {
        inList--;
      }
      if (current == '"') {
        inValue = !inValue;
      }
      if (current == ',' && inObject == 0 && inList == 0 && !inValue) {
        list.add(build.toString());
        build.setLength(0);
      }
      else if (current == '\\') {
        inEscape = true;
      }
      else {
        build.append(current);
      }
      index++;
    }
    if (!build.isEmpty()) {
      list.add(build.toString().trim());
    }
    return list;
  }

}
