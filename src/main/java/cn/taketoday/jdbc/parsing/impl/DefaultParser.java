package cn.taketoday.jdbc.parsing.impl;

/**
 * Created by lars on 22.09.2014.
 */
public class DefaultParser implements CharParser {

  @Override
  public boolean supports(char c, String sql, int idx) {
    return true;
  }

  @Override
  public int parse(char c, int idx, StringBuilder parsedSql, String sql, int length) {
    parsedSql.append(c);
    return idx;
  }
}
