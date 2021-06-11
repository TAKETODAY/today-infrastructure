package cn.taketoday.jdbc.parsing;

import java.util.Map;

/**
 * Created by lars on 11.04.14.
 */
public abstract class SqlParameterParser {

  /**
   * @param statement
   *         sql to parse
   * @param paramMap
   *         ParameterApplier mapping
   *
   * @return parsed sql
   */
  public abstract String parse(String statement, Map<String, ParameterApplier> paramMap);

}
