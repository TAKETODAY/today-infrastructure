package cn.taketoday.jdbc.utils;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.jdbc.parsing.QueryParameter;
import cn.taketoday.jdbc.parsing.SqlParameterParser;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * User: dimzon Date: 4/9/14 Time: 4:44 AM
 */
public class NamedParameterTest extends TestCase {

  private SqlParameterParser sqlParameterParsingStrategy = new SqlParameterParser();

  /*
   A type cast specifies a conversion from one data type to another.
   PostgreSQL accepts two equivalent syntaxes for type casts:
   CAST ( expression AS type )
   expression::type
   */
  public void testPostgresSqlCastSyntax() throws Exception {
    Map<String, QueryParameter> map = new HashMap<>();
    String preparedQuery = sqlParameterParsingStrategy.parse("select :foo", map);
    assertEquals("select ?", preparedQuery);
    assertThat(map.size(), is(equalTo(1)));
    final QueryParameter parameter = map.get("foo");

    List<Integer> integers = new ArrayList<>();
    parameter.getApplier().forEach(integers::add);
    assertThat(integers.size(), is(equalTo(1)));
    assertThat(integers.get(0), is(equalTo(1)));

    map.clear();
    preparedQuery = sqlParameterParsingStrategy.parse("select (:foo)::uuid", map);
    assertEquals("select (?)::uuid", preparedQuery);
  }

  public void testStringConstant() throws Exception {
    Map<String, QueryParameter> map = new HashMap<>();
    String preparedQuery = sqlParameterParsingStrategy.parse("select ':foo'", map);
    assertEquals("select ':foo'", preparedQuery);
  }
}
