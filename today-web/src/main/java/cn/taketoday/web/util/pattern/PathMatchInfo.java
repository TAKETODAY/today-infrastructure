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

package cn.taketoday.web.util.pattern;

import java.util.Collections;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;

/**
 * Holder for URI variables and path parameters (matrix variables) extracted
 * based on the pattern for a given matched path.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/2 17:59
 */
public class PathMatchInfo {
  public static final PathMatchInfo EMPTY = new PathMatchInfo(Collections.emptyMap(), Collections.emptyMap());

  private final Map<String, String> uriVariables;
  private final Map<String, MultiValueMap<String, String>> matrixVariables;

  public PathMatchInfo(Map<String, String> uriVars, @Nullable Map<String, MultiValueMap<String, String>> matrixVars) {
    this.uriVariables = Collections.unmodifiableMap(uriVars);
    this.matrixVariables = matrixVars != null
                           ? Collections.unmodifiableMap(matrixVars) : Collections.emptyMap();
  }

  /**
   * Return the extracted URI variables.
   */
  public Map<String, String> getUriVariables() {
    return this.uriVariables;
  }

  /**
   * Return the extracted URI variable with given name
   */
  @Nullable
  public String getUriVariable(String name) {
    return uriVariables.get(name);
  }

  /**
   * Return maps of matrix variables per path segment, keyed off by URI
   * variable name.
   */
  public Map<String, MultiValueMap<String, String>> getMatrixVariables() {
    return this.matrixVariables;
  }

  /**
   * Return matrix variables
   */
  public MultiValueMap<String, String> getMatrixVariable(String name) {
    return this.matrixVariables.get(name);
  }

  @Override
  public String toString() {
    return "PathMatchInfo[uriVariables=" + this.uriVariables + ", " +
            "matrixVariables=" + this.matrixVariables + "]";
  }
}
