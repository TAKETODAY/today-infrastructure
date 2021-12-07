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

package cn.taketoday.jdbc.datasource.embedded;

/**
 * A supported embedded database type.
 *
 * @author Keith Donald
 * @author Oliver Gierke
 * @since 4.0
 */
public enum EmbeddedDatabaseType {

  /** The <a href="http://hsqldb.org">Hypersonic</a> Embedded Java SQL Database. */
  HSQL,

  /** The <a href="https://h2database.com">H2</a> Embedded Java SQL Database Engine. */
  H2,

  /** The <a href="https://db.apache.org/derby">Apache Derby</a> Embedded SQL Database. */
  DERBY

}
