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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package cn.taketoday.persistence.sql;

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
