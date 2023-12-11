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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

/**
 * SQL Server specific implementation for the {@link CallMetaDataProvider} interface.
 * This class is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
public class SqlServerCallMetaDataProvider extends GenericCallMetaDataProvider {

  private static final String REMOVABLE_COLUMN_PREFIX = "@";

  private static final String RETURN_VALUE_NAME = "@RETURN_VALUE";

  public SqlServerCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
  }

  @Override
  @Nullable
  public String parameterNameToUse(@Nullable String parameterName) {
    if (parameterName == null) {
      return null;
    }
    else if (parameterName.length() > 1 && parameterName.startsWith(REMOVABLE_COLUMN_PREFIX)) {
      return super.parameterNameToUse(parameterName.substring(1));
    }
    else {
      return super.parameterNameToUse(parameterName);
    }
  }

  @Override
  public String namedParameterBindingToUse(@Nullable String parameterName) {
    return parameterName + " = ?";
  }

  @Override
  public boolean byPassReturnParameter(String parameterName) {
    return RETURN_VALUE_NAME.equals(parameterName);
  }

}
