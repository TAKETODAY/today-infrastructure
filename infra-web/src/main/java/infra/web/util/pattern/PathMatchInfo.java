/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.util.pattern;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import infra.util.MultiValueMap;

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

  @Override
  public String toString() {
    return "PathMatchInfo[uriVariables=%s, matrixVariables=%s]".formatted(this.uriVariables, this.matrixVariables);
  }
}
