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

package cn.taketoday.jdbc.persistence.dialect;

/**
 * @author TODAY 2021/10/10 13:13
 * @since 4.0
 */
public class PostgreSQLPlatform extends Platform {

  @Override
  public String pagination(SQLParams sqlParams) {
    PageRow pageRow = sqlParams.getPageRow();
    int limit = pageRow.getPageSize();
    int offset = limit * (pageRow.getPageNum() - 1);
    String limitSQL = " LIMIT " + limit + " OFFSET " + offset;

    return select(sqlParams) + limitSQL;
  }
}
