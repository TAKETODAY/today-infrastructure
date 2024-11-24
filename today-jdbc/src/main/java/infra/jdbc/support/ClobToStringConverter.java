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

package infra.jdbc.support;

import java.sql.Clob;
import java.sql.SQLException;

import infra.core.conversion.Converter;

/**
 * @author TODAY 2021/1/8 22:00
 */
public class ClobToStringConverter implements Converter<Clob, String> {

  @Override
  public String convert(final Clob source) {
    try {
      return source.getSubString(1, (int) source.length());
    }
    catch (SQLException e) {
      throw new IllegalArgumentException("error converting clob to String", e);
    }
    finally {
      try {
        source.free();
      }
      catch (SQLException ignore) { }
    }
  }

}
