package cn.taketoday.jdbc.parsing;

import java.util.List;
import java.util.Map;

/**
 * Created by lars on 11.04.14.
 */
public abstract class SqlParameterParser {

  public abstract String parse(String sqlToParse, Map<String, List<Integer>> mapToFill);
}
