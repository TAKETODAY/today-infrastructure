package cn.taketoday.jdbc.parsing;

import java.util.Map;

/**
 * Created by lars on 11.04.14.
 */
public abstract class SqlParameterParser {

  public abstract String parse(String sqlToParse, Map<String, ParameterApplier> mapToFill);
}
