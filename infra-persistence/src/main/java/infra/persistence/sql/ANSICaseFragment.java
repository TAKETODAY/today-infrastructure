/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.persistence.sql;

/**
 * An ANSI SQL CASE expression : {@code case when ... then ... end as ..}
 *
 * @author Gavin King
 * @author Simon Harris
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ANSICaseFragment extends CaseFragment {

  @Override
  public String toFragmentString() {
    StringBuilder builder = new StringBuilder(cases.size() * 15 + 10)
            .append("case");

    for (var entry : cases.entrySet()) {
      builder.append(" when ")
              .append(entry.getKey())
              .append(" is not null then ")
              .append(entry.getValue());
    }

    builder.append(" end");
    if (returnColumnName != null) {
      builder.append(" as ")
              .append(returnColumnName);
    }

    return builder.toString();
  }

}
