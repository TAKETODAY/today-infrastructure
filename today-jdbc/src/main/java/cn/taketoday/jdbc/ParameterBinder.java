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

package cn.taketoday.jdbc;

import java.io.InputStream;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import cn.taketoday.jdbc.type.TypeHandler;

/**
 * Parameter Setter
 *
 * @author TODAY 2021/1/7 15:09
 */
public abstract class ParameterBinder {

  /**
   * Bind a value to statement
   *
   * @param statement statement
   * @param paramIdx parameter index
   * @throws SQLException parameter set error
   */
  public abstract void bind(PreparedStatement statement, int paramIdx)
          throws SQLException;

  /**
   * null setter
   */
  public static final ParameterBinder null_binder = new ParameterBinder() {
    @Override
    public void bind(final PreparedStatement statement, final int paramIdx) throws SQLException {
      statement.setObject(paramIdx, null);
    }
  };

  /**
   * Bind int to {@link PreparedStatement}
   *
   * @param value int value
   * @return Int ParameterBinder
   * @see PreparedStatement#setInt(int, int)
   */
  public static ParameterBinder forInt(int value) {
    final class IntegerParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setInt(paramIdx, value);
      }

    }
    return new IntegerParameterBinder();
  }

  /**
   * Bind long to {@link PreparedStatement}
   *
   * @param value long value
   * @return Long ParameterBinder
   * @see PreparedStatement#setLong(int, long)
   */
  public static ParameterBinder forLong(long value) {
    final class LongParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setLong(paramIdx, value);
      }

    }
    return new LongParameterBinder();
  }

  /**
   * Bind String to {@link PreparedStatement}
   *
   * @param value String value
   * @return String ParameterBinder
   * @see PreparedStatement#setString(int, String)
   */
  public static ParameterBinder forString(String value) {
    final class StringParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setString(paramIdx, value);
      }

    }
    return new StringParameterBinder();
  }

  /**
   * Bind Timestamp to {@link PreparedStatement}
   *
   * @param value Timestamp value
   * @return Timestamp ParameterBinder
   * @see PreparedStatement#setTimestamp(int, Timestamp)
   */
  public static ParameterBinder forTimestamp(Timestamp value) {
    final class TimestampParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setTimestamp(paramIdx, value);
      }

    }
    return new TimestampParameterBinder();
  }

  /**
   * Bind Time to {@link PreparedStatement}
   *
   * @param value Time value
   * @return Time ParameterBinder
   * @see PreparedStatement#setTime(int, Time)
   */
  public static ParameterBinder forTime(Time value) {
    final class TimeParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setTime(paramIdx, value);
      }

    }
    return new TimeParameterBinder();
  }

  public static ParameterBinder forDate(Date value) {
    final class DateParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setDate(paramIdx, value);
      }
    }
    return new DateParameterBinder();
  }

  /**
   * Bind Boolean to {@link PreparedStatement}
   *
   * @param value Boolean value
   * @return Boolean ParameterBinder
   * @see PreparedStatement#setBoolean(int, boolean)
   */
  public static ParameterBinder forBoolean(boolean value) {
    final class BooleanParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setBoolean(paramIdx, value);
      }

    }
    return new BooleanParameterBinder();
  }

  /**
   * Bind InputStream to {@link PreparedStatement}
   *
   * @param value InputStream value
   * @return InputStream ParameterBinder
   * @see PreparedStatement#setBinaryStream(int, InputStream)
   */
  public static ParameterBinder forBinaryStream(InputStream value) {
    final class BinaryStreamParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setBinaryStream(paramIdx, value);
      }
    }
    return new BinaryStreamParameterBinder();
  }

  /**
   * Bind Object to {@link PreparedStatement} using TypeHandler
   *
   * @param value Object value
   * @return InputStream ParameterBinder
   * @see PreparedStatement#setBinaryStream(int, InputStream)
   */
  public static <T> ParameterBinder forTypeHandler(TypeHandler<T> typeHandler, T value) {
    final class TypeHandlerParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        typeHandler.setParameter(statement, paramIdx, value);
      }
    }
    return new TypeHandlerParameterBinder();
  }
}
