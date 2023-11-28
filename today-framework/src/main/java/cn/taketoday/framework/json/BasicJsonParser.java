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

package cn.taketoday.framework.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.util.StringUtils;

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

  @Override
  public Map<String, Object> parseMap(String json) {
    return parseMap(json, this::parseMapInternal);
  }

  @Override
  public List<Object> parseList(String json) {
    return parseList(json, this::parseListInternal);
  }

  private List<Object> parseListInternal(String json) {
    List<Object> list = new ArrayList<>();
    json = trimLeadingCharacter(trimTrailingCharacter(json, ']'), '[').trim();
    for (String value : tokenize(json)) {
      list.add(parseInternal(value));
    }
    return list;
  }

  private Object parseInternal(String json) {
    if (json.startsWith("[")) {
      return parseListInternal(json);
    }
    if (json.startsWith("{")) {
      return parseMapInternal(json);
    }
    if (json.startsWith("\"")) {
      return trimTrailingCharacter(trimLeadingCharacter(json, '"'), '"');
    }
    try {
      return Long.valueOf(json);
    }
    catch (NumberFormatException ex) {
      // ignore
    }
    try {
      return Double.valueOf(json);
    }
    catch (NumberFormatException ex) {
      // ignore
    }
    return json;
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

  private Map<String, Object> parseMapInternal(String json) {
    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
    json = trimLeadingCharacter(trimTrailingCharacter(json, '}'), '{').trim();
    for (String pair : tokenize(json)) {
      String[] values = StringUtils.trimArrayElements(StringUtils.split(pair, ":"));
      String key = trimLeadingCharacter(trimTrailingCharacter(values[0], '"'), '"');
      Object value = parseInternal(values[1]);
      map.put(key, value);
    }
    return map;
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
    if (build.length() > 0) {
      list.add(build.toString().trim());
    }
    return list;
  }

}
