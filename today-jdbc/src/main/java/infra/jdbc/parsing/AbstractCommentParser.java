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

package infra.jdbc.parsing;

/**
 * Created by lars on 22.09.2014.
 */
public abstract class AbstractCommentParser extends CharParser {

  protected void startParsing() { }

  @Override
  public int parse(char c, int idx, StringBuilder parsedSql, String sql, int length) {
    startParsing();
    do {
      parsedSql.append(c);
      if (++idx == length) {
        return idx;
      }
      c = sql.charAt(idx);
    }
    while (!isEndComment(c));
    parsedSql.append(c);
    return idx;
  }

  public abstract boolean isEndComment(char c);
}
