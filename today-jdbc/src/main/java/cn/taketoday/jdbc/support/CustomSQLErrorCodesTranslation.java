/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.support;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * JavaBean for holding custom JDBC error codes translation for a particular
 * database. The "exceptionClass" property defines which exception will be
 * thrown for the list of error codes specified in the errorCodes property.
 *
 * @author Thomas Risberg
 * @see SQLErrorCodeSQLExceptionTranslator
 * @since 4.0
 */
public class CustomSQLErrorCodesTranslation {

  private String[] errorCodes = new String[0];

  @Nullable
  private Class<?> exceptionClass;

  /**
   * Set the SQL error codes to match.
   */
  public void setErrorCodes(String... errorCodes) {
    this.errorCodes = StringUtils.sortArray(errorCodes);
  }

  /**
   * Return the SQL error codes to match.
   */
  public String[] getErrorCodes() {
    return this.errorCodes;
  }

  /**
   * Set the exception class for the specified error codes.
   */
  public void setExceptionClass(@Nullable Class<?> exceptionClass) {
    if (exceptionClass != null && !DataAccessException.class.isAssignableFrom(exceptionClass)) {
      throw new IllegalArgumentException("Invalid exception class [" + exceptionClass +
              "]: needs to be a subclass of [cn.taketoday.dao.DataAccessException]");
    }
    this.exceptionClass = exceptionClass;
  }

  /**
   * Return the exception class for the specified error codes.
   */
  @Nullable
  public Class<?> getExceptionClass() {
    return this.exceptionClass;
  }

}
