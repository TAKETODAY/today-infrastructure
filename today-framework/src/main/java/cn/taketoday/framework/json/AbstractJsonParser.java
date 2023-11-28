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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;

import cn.taketoday.util.ReflectionUtils;

/**
 * Base class for parsers wrapped or implemented in this package.
 *
 * @author Anton Telechev
 * @author Phillip Webb
 * @since 4.0
 */
public abstract class AbstractJsonParser implements JsonParser {

  protected final Map<String, Object> parseMap(String json, Function<String, Map<String, Object>> parser) {
    return trimParse(json, "{", parser);
  }

  protected final List<Object> parseList(String json, Function<String, List<Object>> parser) {
    return trimParse(json, "[", parser);
  }

  protected final <T> T trimParse(String json, String prefix, Function<String, T> parser) {
    String trimmed = (json != null) ? json.trim() : "";
    if (trimmed.startsWith(prefix)) {
      return parser.apply(trimmed);
    }
    throw new JsonParseException();
  }

  protected final <T> T tryParse(Callable<T> parser, Class<? extends Exception> check) {
    try {
      return parser.call();
    }
    catch (Exception ex) {
      if (check.isAssignableFrom(ex.getClass())) {
        throw new JsonParseException(ex);
      }
      ReflectionUtils.rethrowRuntimeException(ex);
      throw new IllegalStateException(ex);
    }
  }

}
