/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import infra.jdbc.ArrayParameters.ArrayParameter;
import infra.jdbc.parsing.ParameterIndexHolder;
import infra.jdbc.parsing.QueryParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArrayParametersTests {

  @Test
  public void testUpdateParameterNamesToIndexes() {
    final ImmutableList<Integer> of = ImmutableList.of(3, 5);
    ArrayList<ArrayParameter> arrayParametersSortedAsc =
            listOf(new ArrayParameter(6, 3));

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
                    listOf(new ArrayParameter(6, 3))));
  }

  static ArrayList<ArrayParameter> listOf(
          ArrayParameter... parameter) {
    ArrayList<ArrayParameter> parameters = new ArrayList<>();
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
                            new ArrayParameter(3, 5))));

    assertEquals(
            3,
            ArrayParameters.computeNewIndex(
                    3,
                    listOf(
                            new ArrayParameter(3, 5))));

    assertEquals(
            8,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameter(3, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameter(1, 2),
                            new ArrayParameter(3, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameter(1, 2),
                            new ArrayParameter(3, 5),
                            new ArrayParameter(4, 5))));

    assertEquals(
            9,
            ArrayParameters.computeNewIndex(
                    4,
                    listOf(
                            new ArrayParameter(1, 2),
                            new ArrayParameter(3, 5),
                            new ArrayParameter(5, 5))));
  }

  @Test
  public void testUpdateQueryWithArrayParameters() {
    assertEquals(
            "SELECT * FROM user WHERE id IN(?,?,?,?,?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameter(1, 5))));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    new ArrayList<ArrayParameter>()));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameter(1, 0))));

    assertEquals(
            "SELECT * FROM user WHERE id IN(?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE id IN(?)",
                    listOf(new ArrayParameter(1, 1))));

    assertEquals(
            "SELECT * FROM user WHERE login = ? AND id IN(?,?)",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE login = ? AND id IN(?)",
                    listOf(new ArrayParameter(2, 2))));

    assertEquals(
            "SELECT * FROM user WHERE login = ? AND id IN(?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT * FROM user WHERE login = ? AND id IN(?) AND name = ?",
                    listOf(new ArrayParameter(2, 2))));

    assertEquals(
            "SELECT ... WHERE other_id IN (?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
                    listOf(
                            new ArrayParameter(1, 3),
                            new ArrayParameter(3, 3))));

    assertEquals(
            "SELECT ... WHERE other_id IN (?,?,?,?,?) login = ? AND id IN(?,?,?) AND name = ?",
            ArrayParameters.updateQueryWithArrayParameters(
                    "SELECT ... WHERE other_id IN (?) login = ? AND id IN(?) AND name = ?",
                    listOf(
                            new ArrayParameter(1, 5),
                            new ArrayParameter(3, 3))));
  }

  @Test
  void shouldReturnOriginalQueryWhenNoArrayParameters() {
    String parsedQuery = "SELECT * FROM user WHERE id = ? AND name = ?";
    HashMap<String, QueryParameter> queryParameters = new HashMap<>();

    QueryParameter idParam = new QueryParameter("id", ParameterIndexHolder.valueOf(1));
    QueryParameter nameParam = new QueryParameter("name", ParameterIndexHolder.valueOf(2));

    queryParameters.put("id", idParam);
    queryParameters.put("name", nameParam);

    String result = ArrayParameters.updateQueryAndParametersIndexes(parsedQuery, queryParameters, true);

    assertThat(result).isEqualTo(parsedQuery);
  }

  @Test
  void shouldUpdateMapWithMultipleParameterIndexes() {
    HashMap<String, QueryParameter> queryParameters = new HashMap<>();

    // Parameter with multiple indexes
    ArrayList<Integer> indexes = new ArrayList<>();
    indexes.add(1);
    indexes.add(3);
    QueryParameter param = new QueryParameter("test", ParameterIndexHolder.valueOf(indexes));

    queryParameters.put("test", param);

    ArrayList<ArrayParameter> arrayParameters = new ArrayList<>();
    arrayParameters.add(new ArrayParameter(2, 3)); // Insert 3 parameters at index 2

    Map<String, QueryParameter> result = ArrayParameters.updateMap(queryParameters, arrayParameters);

    // Index 1 should remain 1
    // Index 3 should become 5 (3 + 3 - 1)
    QueryParameter updatedParam = result.get("test");
    ArrayList<Integer> newIndexes = new ArrayList<>();
    for (int index : updatedParam.getHolder()) {
      newIndexes.add(index);
    }

    assertThat(newIndexes).containsExactly(1, 5);
  }

  @Test
  void shouldHandleEmptyArrayParametersList() {
    String query = "SELECT * FROM users WHERE id = ?";
    ArrayList<ArrayParameter> arrayParameters = new ArrayList<>();

    String result = ArrayParameters.updateQueryWithArrayParameters(query, arrayParameters);

    assertThat(result).isEqualTo(query);
  }

  @Test
  void shouldHandleArrayParameterAtBeginning() {
    String query = "SELECT * FROM users WHERE id IN(?) AND name = ?";
    ArrayList<ArrayParameter> arrayParameters = new ArrayList<>();
    arrayParameters.add(new ArrayParameter(1, 3)); // First parameter is array with 3 elements

    String result = ArrayParameters.updateQueryWithArrayParameters(query, arrayParameters);

    assertThat(result).isEqualTo("SELECT * FROM users WHERE id IN(?,?,?) AND name = ?");
  }

  @Test
  void shouldHandleConsecutiveArrayParameters() {
    String query = "SELECT * FROM users WHERE id IN(?) AND tags IN(?)";
    ArrayList<ArrayParameter> arrayParameters = new ArrayList<>();
    arrayParameters.add(new ArrayParameter(1, 2)); // First parameter array with 2 elements
    arrayParameters.add(new ArrayParameter(2, 3)); // Second parameter array with 3 elements (will be shifted)

    String result = ArrayParameters.updateQueryWithArrayParameters(query, arrayParameters);

    assertThat(result).isEqualTo("SELECT * FROM users WHERE id IN(?,?) AND tags IN(?,?,?)");
  }

}
