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

package infra.jdbc.parsing;

/**
 * Created by lars on 22.09.2014.
 */
public class QuoteParser extends CharParser {

  @Override
  public boolean supports(char c, String sql, int idx) {
    return c == '\'' || c == '"';
  }

  @Override
  public int parse(char c, int idx, StringBuilder parsedSql, String sql, int length) {
    char quoteChar = c;

    do {
      parsedSql.append(c);
      if (++idx == length)
        return idx;
      c = sql.charAt(idx);
    }
    while (c != quoteChar);
    parsedSql.append(c);
    return idx;
  }
}
