/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import cn.taketoday.jdbc.Query.ArrayParameterBinder;
import cn.taketoday.jdbc.parsing.ParameterIndexHolder;
import cn.taketoday.jdbc.parsing.QueryParameter;

/**
 * <pre>
 *     createQuery("SELECT * FROM user WHERE id IN(:ids)")
 *      .addParameter("ids", 4, 5, 6)
 *      .fetch(...)
 * </pre> will generate the query :
 * <code>SELECT * FROM user WHERE id IN(4,5,6)</code><br>
 * <br>
 */
final class ArrayParameters {

  record ArrayParameter(int parameterIndex, int parameterCount) implements Comparable<ArrayParameter> {
    // parameterIndex the index of the parameter array
    // parameterCount the number of parameters to put in the query placeholder

    @Override
    public int compareTo(ArrayParameter o) {
      return Integer.compare(parameterIndex, o.parameterIndex);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof ArrayParameter that))
        return false;
      return parameterIndex == that.parameterIndex
              && parameterCount == that.parameterCount;
    }

    @Override
    public int hashCode() {
      return Objects.hash(parameterIndex, parameterCount);
    }
  }

  /**
   * Update both the query and the parameter indexes to include the array
   * parameters.
   *
   * @throws ArrayParameterBindFailedException array parameter bind failed
   */
  static String updateQueryAndParametersIndexes(
          String parsedQuery,
          HashMap<String, QueryParameter> queryParameters,
          boolean allowArrayParameters
  ) {
    ArrayList<ArrayParameter> arrayParameters
            = sortedArrayParameters(queryParameters, allowArrayParameters);
    if (arrayParameters.isEmpty()) {
      return parsedQuery;
    }

    updateMap(queryParameters, arrayParameters);
    return updateQueryWithArrayParameters(parsedQuery, arrayParameters);
  }

  /**
   * Update the indexes of each query parameter
   */
  static Map<String, QueryParameter> updateMap(
          HashMap<String, QueryParameter> queryParameters,
          ArrayList<ArrayParameter> arrayParametersSortedAsc
  ) {

    for (QueryParameter parameter : queryParameters.values()) {
      ArrayList<Integer> newParameterIndex = new ArrayList<>();

      for (int parameterIndex : parameter.getHolder()) {
        int newIdx = computeNewIndex(parameterIndex, arrayParametersSortedAsc);
        newParameterIndex.add(newIdx);
      }

      if (newParameterIndex.size() > 1) {
        parameter.setHolder(ParameterIndexHolder.valueOf(newParameterIndex));
      }
      else {
        parameter.setHolder(ParameterIndexHolder.valueOf(newParameterIndex.get(0)));
      }
    }

    return queryParameters;
  }

  /**
   * Compute the new index of a parameter given the index positions of the array
   * parameters.
   */
  static int computeNewIndex(int index, ArrayList<ArrayParameter> arrayParametersSortedAsc) {
    int newIndex = index;
    for (ArrayParameter arrayParameter : arrayParametersSortedAsc) {
      if (index > arrayParameter.parameterIndex) {
        newIndex = newIndex + arrayParameter.parameterCount - 1;
      }
      else {
        return newIndex;
      }
    }
    return newIndex;
  }

  /**
   * List all the array parameters that contains more that 1 parameters. Indeed,
   * array parameter below 1 parameter will not change the text query nor the
   * parameter indexes.
   *
   * @throws ArrayParameterBindFailedException array parameter bind failed
   */
  private static ArrayList<ArrayParameter> sortedArrayParameters(
          HashMap<String, QueryParameter> queryParameters,
          boolean allowArrayParameters
  ) {
    ArrayList<ArrayParameter> arrayParameters = new ArrayList<>();
    for (QueryParameter parameter : queryParameters.values()) {
      ParameterBinder binder = parameter.getBinder();
      if (binder instanceof ArrayParameterBinder) {
        int parameterCount = ((ArrayParameterBinder) binder).getParameterCount();
        if (parameterCount > 1) {
          if (!allowArrayParameters) {
            throw new ArrayParameterBindFailedException("Array parameters are not allowed in batch mode");
          }

          for (int index : parameter.getHolder()) {
            arrayParameters.add(new ArrayParameter(index, parameterCount));
          }
        }
      }
    }
    if (arrayParameters.size() > 1) {
      Collections.sort(arrayParameters);
    }
    return arrayParameters;
  }

  /**
   * Change the query to replace ? at each arrayParametersSortedAsc.parameterIndex
   * with ?,?,?.. multiple arrayParametersSortedAsc.parameterCount
   */
  static String updateQueryWithArrayParameters(
          String parsedQuery, ArrayList<ArrayParameter> arrayParametersSortedAsc
  ) {
    if (arrayParametersSortedAsc.isEmpty()) {
      return parsedQuery;
    }

    StringBuilder sb = new StringBuilder();

    Iterator<ArrayParameter> parameterToReplaceIt = arrayParametersSortedAsc.iterator();
    ArrayParameter nextParameterToReplace = parameterToReplaceIt.next();
    // PreparedStatement index starts at 1
    int currentIndex = 1;
    for (char c : parsedQuery.toCharArray()) {
      if (nextParameterToReplace != null && c == '?') {
        if (currentIndex == nextParameterToReplace.parameterIndex) {
          sb.append('?');
          for (int i = 1; i < nextParameterToReplace.parameterCount; i++) {
            sb.append(",?");
          }

          if (parameterToReplaceIt.hasNext()) {
            nextParameterToReplace = parameterToReplaceIt.next();
          }
          else {
            nextParameterToReplace = null;
          }
        }
        else {
          sb.append(c);
        }
        currentIndex++;
      }
      else {
        sb.append(c);
      }
    }

    return sb.toString();
  }

}
