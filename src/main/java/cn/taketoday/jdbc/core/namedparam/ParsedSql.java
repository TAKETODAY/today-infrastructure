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

package cn.taketoday.jdbc.core.namedparam;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds information about a parsed SQL statement.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ParsedSql {

  private final String originalSql;

  private int namedParameterCount;
  private int totalParameterCount;
  private int unnamedParameterCount;

  private final ArrayList<String> parameterNames = new ArrayList<>();
  private final ArrayList<int[]> parameterIndexes = new ArrayList<>();

  /**
   * Create a new instance of the {@link ParsedSql} class.
   *
   * @param originalSql the SQL statement that is being (or is to be) parsed
   */
  ParsedSql(String originalSql) {
    this.originalSql = originalSql;
  }

  /**
   * Return the SQL statement that is being parsed.
   */
  String getOriginalSql() {
    return this.originalSql;
  }

  /**
   * Add a named parameter parsed from this SQL statement.
   *
   * @param parameterName the name of the parameter
   * @param startIndex the start index in the original SQL String
   * @param endIndex the end index in the original SQL String
   */
  void addNamedParameter(String parameterName, int startIndex, int endIndex) {
    this.parameterNames.add(parameterName);
    this.parameterIndexes.add(new int[] { startIndex, endIndex });
  }

  /**
   * Return all of the parameters (bind variables) in the parsed SQL statement.
   * Repeated occurrences of the same parameter name are included here.
   */
  List<String> getParameterNames() {
    return this.parameterNames;
  }

  /**
   * Return the parameter indexes for the specified parameter.
   *
   * @param parameterPosition the position of the parameter
   * (as index in the parameter names List)
   * @return the start index and end index, combined into
   * a int array of length 2
   */
  int[] getParameterIndexes(int parameterPosition) {
    return this.parameterIndexes.get(parameterPosition);
  }

  /**
   * Set the count of named parameters in the SQL statement.
   * Each parameter name counts once; repeated occurrences do not count here.
   */
  void setNamedParameterCount(int namedParameterCount) {
    this.namedParameterCount = namedParameterCount;
  }

  /**
   * Return the count of named parameters in the SQL statement.
   * Each parameter name counts once; repeated occurrences do not count here.
   */
  int getNamedParameterCount() {
    return this.namedParameterCount;
  }

  /**
   * Set the count of all of the unnamed parameters in the SQL statement.
   */
  void setUnnamedParameterCount(int unnamedParameterCount) {
    this.unnamedParameterCount = unnamedParameterCount;
  }

  /**
   * Return the count of all of the unnamed parameters in the SQL statement.
   */
  int getUnnamedParameterCount() {
    return this.unnamedParameterCount;
  }

  /**
   * Set the total count of all of the parameters in the SQL statement.
   * Repeated occurrences of the same parameter name do count here.
   */
  void setTotalParameterCount(int totalParameterCount) {
    this.totalParameterCount = totalParameterCount;
  }

  /**
   * Return the total count of all of the parameters in the SQL statement.
   * Repeated occurrences of the same parameter name do count here.
   */
  int getTotalParameterCount() {
    return this.totalParameterCount;
  }

  /**
   * Exposes the original SQL String.
   */
  @Override
  public String toString() {
    return this.originalSql;
  }

}
