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

package cn.taketoday.jdbc.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.jdbc.parsing.QueryParameter;
import cn.taketoday.jdbc.parsing.SqlParameterParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * User: dimzon Date: 4/9/14 Time: 4:44 AM
 */
public class NamedParameterTest {

  private SqlParameterParser sqlParameterParsingStrategy = new SqlParameterParser();

  /*
   A type cast specifies a conversion from one data type to another.
   PostgreSQL accepts two equivalent syntaxes for type casts:
   CAST ( expression AS type )
   expression::type
   */
  @Test
  public void testPostgresSqlCastSyntax() throws Exception {
    Map<String, QueryParameter> map = new HashMap<>();
    String preparedQuery = sqlParameterParsingStrategy.parse("select :foo", map);
    assertEquals("select ?", preparedQuery);
    assertThat(map.size()).isEqualTo(1);
    final QueryParameter parameter = map.get("foo");

    List<Integer> integers = new ArrayList<>();
    parameter.getHolder().forEach(integers::add);
    assertThat(integers.size()).isEqualTo(1);
    assertThat(integers.get(0)).isEqualTo(1);

    map.clear();
    preparedQuery = sqlParameterParsingStrategy.parse("select (:foo)::uuid", map);
    assertEquals("select (?)::uuid", preparedQuery);
  }

  @Test
  public void testStringConstant() throws Exception {
    Map<String, QueryParameter> map = new HashMap<>();
    String preparedQuery = sqlParameterParsingStrategy.parse("select ':foo'", map);
    assertEquals("select ':foo'", preparedQuery);
  }
}
