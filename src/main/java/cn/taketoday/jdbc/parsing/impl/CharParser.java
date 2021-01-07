package cn.taketoday.jdbc.parsing.impl;

/**
 * Created by lars on 22.09.2014.
 */
public interface CharParser {

  boolean canParse(char c, String sql, int idx);

  int parse(char c, int idx, StringBuilder parsedSql, String sql, int length);
}
