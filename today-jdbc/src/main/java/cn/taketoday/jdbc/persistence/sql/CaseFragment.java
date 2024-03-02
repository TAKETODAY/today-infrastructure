/*
 * Copyright 2017 - 2024 the original author or authors.
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


package cn.taketoday.jdbc.persistence.sql;

import java.util.LinkedHashMap;

/**
 * Abstract SQL case fragment renderer
 *
 * @author Gavin King, Simon Harris
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
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

  public static String qualify(String prefix, String name) {
    if (name == null || prefix == null) {
      throw new NullPointerException("prefix or name were null attempting to build qualified name");
    }
    return prefix + '.' + name;
  }
}
