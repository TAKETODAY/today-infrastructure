/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.bind.resolver;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.util.pattern.PathMatchInfo;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/28 22:11
 */
public class MockPathMatchInfo extends PathMatchInfo {

  private final Map<String, String> uriVariables;
  private final Map<String, MultiValueMap<String, String>> matrixVariables;

  public MockPathMatchInfo() {
    this(new HashMap<>(), new HashMap<>());
  }

  public MockPathMatchInfo(Map<String, String> uriVars, Map<String, MultiValueMap<String, String>> matrixVars) {
    super(uriVars, matrixVars);
    uriVariables = uriVars;
    matrixVariables = matrixVars;
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

}
