package cn.taketoday.jdbc.parsing;

import java.util.List;
import java.util.Map;

/**
 * Created by lars on 11.04.14.
 */
public interface SqlParameterParsingStrategy {

  String parseSql(String sqlToParse, Map<String, List<Integer>> mapToFill);
}
