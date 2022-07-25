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

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLSyntaxErrorException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;

/**
 * Class to generate Java 6 SQLException subclasses for testing purposes.
 *
 * @author Thomas Risberg
 */
public class SQLExceptionSubclassFactory {

  public static SQLException newSQLDataException(String reason, String SQLState, int vendorCode) {
    return new SQLDataException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLFeatureNotSupportedException(String reason, String SQLState, int vendorCode) {
    return new SQLFeatureNotSupportedException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLIntegrityConstraintViolationException(String reason, String SQLState, int vendorCode) {
    return new SQLIntegrityConstraintViolationException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLInvalidAuthorizationSpecException(String reason, String SQLState, int vendorCode) {
    return new SQLInvalidAuthorizationSpecException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLNonTransientConnectionException(String reason, String SQLState, int vendorCode) {
    return new SQLNonTransientConnectionException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLSyntaxErrorException(String reason, String SQLState, int vendorCode) {
    return new SQLSyntaxErrorException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLTransactionRollbackException(String reason, String SQLState, int vendorCode) {
    return new SQLTransactionRollbackException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLTransientConnectionException(String reason, String SQLState, int vendorCode) {
    return new SQLTransientConnectionException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLTimeoutException(String reason, String SQLState, int vendorCode) {
    return new SQLTimeoutException(reason, SQLState, vendorCode);
  }

  public static SQLException newSQLRecoverableException(String reason, String SQLState, int vendorCode) {
    return new SQLRecoverableException(reason, SQLState, vendorCode);
  }

}
