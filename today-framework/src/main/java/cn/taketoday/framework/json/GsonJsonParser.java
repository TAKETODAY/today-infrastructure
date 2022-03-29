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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper to adapt {@link Gson} to a {@link JsonParser}.
 *
 * @author Dave Syer
 * @author Jean de Klerk
 * @see JsonParser#lookup()
 * @since 4.0
 */
public class GsonJsonParser extends AbstractJsonParser {

  private static final TypeToken<?> MAP_TYPE = new MapTypeToken();

  private static final TypeToken<?> LIST_TYPE = new ListTypeToken();

  private final Gson gson = new GsonBuilder().create();

  @Override
  public Map<String, Object> parseMap(String json) {
    return parseMap(json, (trimmed) -> this.gson.fromJson(trimmed, MAP_TYPE.getType()));
  }

  @Override
  public List<Object> parseList(String json) {
    return parseList(json, (trimmed) -> this.gson.fromJson(trimmed, LIST_TYPE.getType()));
  }

  private static final class MapTypeToken extends TypeToken<Map<String, Object>> {

  }

  private static final class ListTypeToken extends TypeToken<List<Object>> {

  }

}
