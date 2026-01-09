/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
      catch (SQLException ignore) {
      }
    }
  }

}
