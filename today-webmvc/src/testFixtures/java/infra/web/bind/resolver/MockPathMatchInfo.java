/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.bind.resolver;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import infra.util.MultiValueMap;
import infra.web.util.pattern.PathMatchInfo;

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

}
