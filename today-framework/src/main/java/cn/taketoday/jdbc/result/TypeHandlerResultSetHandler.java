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
package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.jdbc.type.TypeHandler;

/**
 * @author TODAY 2021/1/7 22:52
 */
public final class TypeHandlerResultSetHandler<T> extends ResultSetHandler<T> {
  final TypeHandler<T> typeHandler;

  public TypeHandlerResultSetHandler(TypeHandler<T> typeHandler) {
    this.typeHandler = typeHandler;
  }

  @Override
  public T handle(ResultSet resultSet) throws SQLException {
    return typeHandler.getResult(resultSet, 1);
  }
}
