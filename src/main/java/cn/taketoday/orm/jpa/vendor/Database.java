/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.orm.jpa.vendor;

/**
 * Enumeration for common database platforms. Allows strong typing of database type
 * and portable configuration between JpaVendorDialect implementations.
 *
 * <p>If a given PersistenceProvider supports a database not listed here,
 * the strategy class can still be specified using the fully-qualified class name.
 * This enumeration is merely a convenience. The database products listed here
 * are the same as those explicitly supported for Framework JDBC exception translation
 * in {@code sql-error-codes.xml}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AbstractJpaVendorAdapter#setDatabase
 * @since 4.0
 */
public enum Database {

  DEFAULT,

  DB2,

  DERBY,

  H2,

  HANA,

  HSQL,

  INFORMIX,

  MYSQL,

  ORACLE,

  POSTGRESQL,

  SQL_SERVER,

  SYBASE

}
