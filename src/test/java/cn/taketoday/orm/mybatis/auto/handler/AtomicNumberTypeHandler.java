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
package cn.taketoday.orm.mybatis.auto.handler;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@MappedTypes({ AtomicInteger.class, AtomicLong.class })
public class AtomicNumberTypeHandler implements TypeHandler<Number> {

  private final Class<? extends Number> type;

  public AtomicNumberTypeHandler(Class<? extends Number> type) {
    this.type = type;
  }

  public void setParameter(PreparedStatement ps, int i, Number parameter, JdbcType jdbcType) throws SQLException {
  }

  public Number getResult(ResultSet rs, String columnName) throws SQLException {
    return null;
  }

  public Number getResult(CallableStatement cs, int columnIndex) throws SQLException {
    return null;
  }

  public Number getResult(ResultSet rs, int columnIndex) throws SQLException {
    return null;
  }

  @Override
  public String toString() {
    return "type=" + type;
  }

}
