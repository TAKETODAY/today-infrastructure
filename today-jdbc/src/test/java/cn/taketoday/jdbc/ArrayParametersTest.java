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

package cn.taketoday.jdbc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.jdbc.parsing.ParameterIndexHolder;
import cn.taketoday.jdbc.parsing.QueryParameter;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayParametersTest {

  @Test
  public void testUpdateParameterNamesToIndexes() {
    final ImmutableList<Integer> of = ImmutableList.of(3, 5);
    ArrayList<ArrayParameters.ArrayParameter> arrayParametersSortedAsc =
            listOf(new ArrayParameters.ArrayParameter(6, 3));

    QueryParameter parameter = new QueryParameter("paramName", ParameterIndexHolder.valueOf(of));

    ImmutableMap<String, QueryParameter> paramName2 = ImmutableMap.of("paramName", parameter);
    Map<String, QueryParameter> paramName =
            ArrayParameters.updateMap(Maps.newHashMap(paramName2), arrayParametersSortedAsc);

    assertEquals(ImmutableMap.of("paramName", new QueryParameter("paramName", ParameterIndexHolder.valueOf(ImmutableList.of(3, 5)))),
            paramName);

    parameter = new QueryParameter("paramName", ParameterIndexHolder.valueOf(ImmutableList.of(3, 7)));

    ImmutableMap<String, QueryParameter> paramName1 = ImmutableMap.of("paramName", parameter);

    assertEquals(
            ImmutableMap.of("paramName", new QueryParameter("paramName", ParameterIndexHolder.valueOf(ImmutableList.of(3, 9)))),
            ArrayParameters.updateMap(
                    Maps.newHashMap(paramName1),
                    listOf(new ArrayParameters.ArrayParameter(6, 3))));
  }

  static ArrayList<ArrayParameters.ArrayParameter> listOf(
          ArrayParameters.ArrayParameter... parameter) {
    ArrayList<ArrayParameters.ArrayParameter> parameters = new ArrayList<>();
    Collections.addAll(parameters, parameter);
    return parameters;
  }

  @Test
  public void testComputeNewIndex() {

    assertEquals(
            2,
            ArrayParameters.computeNewIndex(
                    2,
                    listOf(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            3,
            ArrayParameters.computeNewIndex(
                    3,
                    listOf(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            8,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5),
                            new ArrayParameters.ArrayParameter(4, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 2),
                            new ArrayParameters.ArrayParameter(3, 5),
                            new ArrayParameters.ArrayParameter(5, 5))));
  }

  @Test
  public void testUpdateQueryWithArrayParameters() {
    assertEquals(
            "SELECT * FROM user WHERE id IN(?,?,?,?,?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameters.ArrayParameter(1, 5))));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    new ArrayList<ArrayParameters.ArrayParameter>()));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameters.ArrayParameter(1, 0))));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameters.ArrayParameter(1, 1))));

    assertEquals(
            "SELECT * FROM user WHERE login = ? AND id IN(?,?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE login = ? AND id IN(?)",
                    listOf(new ArrayParameters.ArrayParameter(2, 2))));

    assertEquals(
            "SELECT * FROM user WHERE login = ? AND id IN(?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE login = ? AND id IN(?) AND name = ?",
                    listOf(new ArrayParameters.ArrayParameter(2, 2))));

    assertEquals(
            "SELECT ... WHERE other_id IN (?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 3),
                            new ArrayParameters.ArrayParameter(3, 3))));

    assertEquals(
            "SELECT ... WHERE other_id IN (?,?,?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
                    listOf(
                            new ArrayParameters.ArrayParameter(1, 5),
                            new ArrayParameters.ArrayParameter(3, 3))));
  }

}
