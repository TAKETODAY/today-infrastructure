package cn.taketoday.jdbc.parsing.impl;

import java.util.List;
import java.util.Map;

import cn.taketoday.jdbc.parsing.SqlParameterParser;

/**
 * Created by lars on 11.04.14.
 */
public class DefaultSqlParameterParser extends SqlParameterParser {

  public CharParser[] getCharParsers(Map<String, List<Integer>> paramMap) {
    return new CharParser[] {
            new QuoteParser(),
            new DoubleHyphensCommentParser(),
            new ForwardSlashCommentParser(),
            new ParameterParser(paramMap),
            new DefaultParser()
    };
  }

  @Override
  public String parse(String statement, Map<String, List<Integer>> paramMap) {
    final int length = statement.length();
    final StringBuilder parsedQuery = new StringBuilder(length);

    final CharParser[] charParsers = getCharParsers(paramMap);

    for (int idx = 0; idx < length; idx++) {
      for (CharParser parser : charParsers) {
        char c = statement.charAt(idx);
        if (parser.supports(c, statement, idx)) {
          idx = parser.parse(c, idx, parsedQuery, statement, length);
          break;
        }
      }
    }

    return parsedQuery.toString();
  }
}
