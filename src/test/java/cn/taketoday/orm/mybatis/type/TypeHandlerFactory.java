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
package cn.taketoday.orm.mybatis.type;

import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public interface TypeHandlerFactory {

  static TypeHandler<String> handler1() {
    return new TypeHandler<String>() {
      @Override
      public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) {

      }

      @Override
      public String getResult(ResultSet rs, String columnName) {
        return null;
      }

      @Override
      public String getResult(ResultSet rs, int columnIndex) {
        return null;
      }

      @Override
      public String getResult(CallableStatement cs, int columnIndex) {
        return null;
      }
    };
  }

  static TypeHandler<UUID> handler2() {
    return new InnerTypeHandler();
  }

  @MappedTypes({ UUID.class })
  class InnerTypeHandler implements TypeHandler<UUID> {

    @Override
    public void setParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType) {
    }

    @Override
    public UUID getResult(ResultSet rs, String columnName) {
      return null;
    }

    @Override
    public UUID getResult(ResultSet rs, int columnIndex) {
      return null;
    }

    @Override
    public UUID getResult(CallableStatement cs, int columnIndex) {
      return null;
    }

  }

}
