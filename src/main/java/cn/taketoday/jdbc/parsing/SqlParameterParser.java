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
package cn.taketoday.jdbc.parsing;

import java.util.Map;

/**
 * Created by lars on 11.04.14.
 */
public class SqlParameterParser {

  public static CharParser[] getCharParsers(Map<String, QueryParameter> paramMap) {
    return new CharParser[] {
            new QuoteParser(),
            new DoubleHyphensCommentParser(),
            new ForwardSlashCommentParser(),
            new ParameterParser(paramMap),
            new DefaultParser()
    };
  }

  /**
   * @param statement sql to parse
   * @param paramMap QueryParameter mapping
   * @return parsed sql
   */
  public String parse(final String statement, final Map<String, QueryParameter> paramMap) {
    final int length = statement.length();
    final StringBuilder parsedQuery = new StringBuilder(length);
    final CharParser[] charParsers = getCharParsers(paramMap);

    for (int idx = 0; idx < length; idx++) {
      for (final CharParser parser : charParsers) {
        final char c = statement.charAt(idx);
        if (parser.supports(c, statement, idx)) {
          idx = parser.parse(c, idx, parsedQuery, statement, length);
          break;
        }
      }
    }

    return parsedQuery.toString();
  }

}
