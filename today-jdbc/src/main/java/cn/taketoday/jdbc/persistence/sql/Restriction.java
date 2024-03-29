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

import java.util.Collection;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * A restriction (predicate) to be applied to a query
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface Restriction {

  /**
   * Render the restriction into the SQL buffer
   */
  void render(StringBuilder sqlBuffer);

  /**
   * Render the restriction into the SQL buffer
   */
  static void render(@Nullable Collection<Restriction> restrictions, StringBuilder buf) {
    if (CollectionUtils.isNotEmpty(restrictions)) {
      buf.append(" WHERE ");
      renderWhereClause(restrictions, buf);
    }
  }

  /**
   * Render the restriction into the SQL buffer
   */
  static void renderWhereClause(Collection<Restriction> restrictions, StringBuilder buf) {
    boolean appended = false;
    for (Restriction restriction : restrictions) {
      if (appended) {
        buf.append(" AND ");
      }
      else {
        appended = true;
      }
      restriction.render(buf);
    }
  }

}
