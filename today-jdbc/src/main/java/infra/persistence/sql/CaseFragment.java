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

package infra.persistence.sql;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;

/**
 * Abstract SQL case fragment renderer
 *
 * @author Gavin King, Simon Harris
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("NullAway")
public abstract class CaseFragment {

  public abstract String toFragmentString();

  protected String returnColumnName;

  protected LinkedHashMap<String, String> cases = new LinkedHashMap<>();

  public CaseFragment setReturnColumnName(String returnColumnName) {
    this.returnColumnName = returnColumnName;
    return this;
  }

  public CaseFragment setReturnColumnName(String returnColumnName, String suffix) {
    return setReturnColumnName(new Alias(suffix).toAliasString(returnColumnName));
  }

  public CaseFragment addWhenColumnNotNull(String alias, String columnName, String value) {
    cases.put(qualify(alias, columnName), value);
    return this;
  }

  public static String qualify(@Nullable String prefix, @Nullable String name) {
    if (name == null || prefix == null) {
      throw new NullPointerException("prefix or name were null attempting to build qualified name");
    }
    return prefix + '.' + name;
  }
}
