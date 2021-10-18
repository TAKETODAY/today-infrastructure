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
