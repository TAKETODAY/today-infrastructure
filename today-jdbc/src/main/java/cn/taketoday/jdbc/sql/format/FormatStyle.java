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

package cn.taketoday.jdbc.sql.format;

/**
 * Represents the the understood types or styles of formatting.
 *
 * @author Steve Ebersole
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/12 19:20
 */
public enum FormatStyle {
  /**
   * Formatting for SELECT, INSERT, UPDATE and DELETE statements
   */
  BASIC("basic", new BasicSQLFormatter()),
  /**
   * Formatting for DDL (CREATE, ALTER, DROP, etc) statements
   */
  DDL("ddl", DDLSQLFormatter.INSTANCE),
  /**
   * Syntax highlighting via ANSI escape codes
   */
  HIGHLIGHT("highlight", HighlightingSQLFormatter.INSTANCE),
  /**
   * No formatting
   */
  NONE("none", source -> source);

  public final String name;
  public final SQLFormatter formatter;

  FormatStyle(String name, SQLFormatter formatter) {
    this.name = name;
    this.formatter = formatter;
  }

}
