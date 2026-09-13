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

package infra.jdbc.format;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;
import java.util.TreeMap;

/**
 * JDK dynamic-proxy wrapper around a {@link PreparedStatement} that collects the
 * bound parameter values and logs them (MyBatis style) around execution, along with
 * the update count or the number of rows read.
 *
 * <p>The wrapper is only installed when SQL logging is enabled; otherwise
 * {@link #wrap} returns the original statement so the normal path has no overhead.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public final class LoggingPreparedStatement implements InvocationHandler {

  private final PreparedStatement delegate;

  private final SqlStatementLogger logger;

  private final Map<Integer, @Nullable Object> parameters = new TreeMap<>();

  private LoggingPreparedStatement(PreparedStatement delegate, SqlStatementLogger logger) {
    this.delegate = delegate;
    this.logger = logger;
  }

  /**
   * Wrap the given statement when SQL logging is enabled; otherwise return it as-is.
   *
   * @param statement the statement to wrap
   * @param logger the SQL logger to report parameters and results to
   * @return a logging proxy, or the original statement when logging is disabled
   */
  public static PreparedStatement wrap(PreparedStatement statement, SqlStatementLogger logger) {
    if (!logger.isDebugEnabled()) {
      return statement;
    }
    return (PreparedStatement) Proxy.newProxyInstance(
            LoggingPreparedStatement.class.getClassLoader(),
            new Class<?>[] { PreparedStatement.class },
            new LoggingPreparedStatement(statement, logger));
  }

  @Override
  public Object invoke(Object proxy, Method method, @Nullable Object @Nullable [] args) throws Throwable {
    String name = method.getName();
    if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer index) {
      parameters.put(index, "setNull".equals(name) ? null : args[1]);
    }
    else if ("clearParameters".equals(name)) {
      parameters.clear();
    }
    else if (isExecution(name)) {
      logger.logParameters(parameters.values().toArray());
    }

    Object result = method.invoke(delegate, args);

    if (result instanceof ResultSet resultSet && "executeQuery".equals(name)) {
      return wrapResultSet(resultSet);
    }
    if (result instanceof Integer count
            && ("executeUpdate".equals(name) || "executeLargeUpdate".equals(name))) {
      logger.logResult("Updates", count);
    }
    return result;
  }

  private ResultSet wrapResultSet(ResultSet resultSet) {
    int[] total = { 0 };
    return (ResultSet) Proxy.newProxyInstance(
            LoggingPreparedStatement.class.getClassLoader(),
            new Class<?>[] { ResultSet.class },
            (proxy, method, args) -> {
              String name = method.getName();
              if ("next".equals(name)) {
                boolean next = (boolean) method.invoke(resultSet, args);
                if (next) {
                  if (total[0] == 0) {
                    logger.logOutcome("<==    Columns: " + columnLabels(resultSet));
                  }
                  total[0]++;
                  logger.logOutcome("<==        Row: " + rowValues(resultSet));
                }
                return next;
              }
              if ("close".equals(name)) {
                logger.logResult("Total", total[0]);
              }
              return method.invoke(resultSet, args);
            });
  }

  private static String columnLabels(ResultSet resultSet) throws Exception {
    ResultSetMetaData metaData = resultSet.getMetaData();
    int count = metaData.getColumnCount();
    String[] labels = new String[count];
    for (int i = 1; i <= count; i++) {
      labels[i - 1] = metaData.getColumnLabel(i);
    }
    return String.join(", ", labels);
  }

  private String rowValues(ResultSet resultSet) throws Exception {
    ResultSetMetaData metaData = resultSet.getMetaData();
    int count = metaData.getColumnCount();
    Object[] values = new Object[count];
    for (int i = 1; i <= count; i++) {
      values[i - 1] = resultSet.getObject(i);
    }
    return logger.formatParameters(values);
  }

  private static boolean isExecution(String name) {
    return switch (name) {
      case "execute", "executeUpdate", "executeLargeUpdate", "executeQuery",
           "executeBatch", "executeLargeBatch", "addBatch" -> true;
      default -> false;
    };
  }

}
