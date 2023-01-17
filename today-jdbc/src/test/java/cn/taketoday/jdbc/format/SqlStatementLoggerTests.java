/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.format;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/9/12 19:38
 */
class SqlStatementLoggerTests {
  SqlStatementLogger logger = new SqlStatementLogger(true, true, true, 20);

  @Test
  void log() {

    logger.logStatement("SELECT * FROM t_user where id = ?");
    logger.logStatement("SELECT * FROM t_user where id = ?", DDLSQLFormatter.INSTANCE);
    logger.logSlowQuery("SELECT * FROM t_user where id = ?", System.nanoTime() - TimeUnit.MINUTES.toNanos(2));

    logger.logStatement(
            "create table issue5table(id int identity primary key, val integer)", DDLSQLFormatter.INSTANCE);

  }

}