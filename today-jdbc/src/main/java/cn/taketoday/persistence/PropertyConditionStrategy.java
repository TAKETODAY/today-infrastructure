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

package cn.taketoday.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.persistence.sql.Restriction;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/2/24 23:58
 */
public interface PropertyConditionStrategy {

  @Nullable
  Condition resolve(EntityProperty entityProperty, Object propertyValue);

  class Condition implements Restriction {

    public final Object propertyValue;

    public final Restriction restriction;

    public final EntityProperty entityProperty;

    public Condition(Object propertyValue, Restriction restriction, EntityProperty entityProperty) {
      this.propertyValue = propertyValue;
      this.restriction = restriction;
      this.entityProperty = entityProperty;
    }

    /**
     * Render the restriction into the SQL buffer
     */
    @Override
    public void render(StringBuilder sqlBuffer) {
      restriction.render(sqlBuffer);
    }

    /**
     * <p>Sets the value of the designated parameter using the given object.
     *
     * @param parameterIndex the first parameter is 1, the second is 2, ...
     * @return Returns next parameterIndex
     * @throws SQLException if parameterIndex does not correspond to a parameter
     * marker in the SQL statement; if a database access error occurs;
     * this method is called on a closed {@code PreparedStatement}
     * or the type of the given object is ambiguous
     */
    public int setParameter(PreparedStatement ps, int parameterIndex) throws SQLException {
      entityProperty.setParameter(ps, parameterIndex++, propertyValue);
      return parameterIndex;
    }

  }
}
