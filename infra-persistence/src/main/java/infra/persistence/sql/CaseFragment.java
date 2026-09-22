/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
