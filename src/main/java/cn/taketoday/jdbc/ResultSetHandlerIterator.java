package cn.taketoday.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Iterator for a {@link ResultSet}. Tricky part here is getting
 * {@link #hasNext()} to work properly, meaning it can be called multiple times
 * without calling {@link #next()}.
 *
 * @author aldenquimby@gmail.com
 */
public class ResultSetHandlerIterator<T> extends AbstractResultSetIterator<T> {

  private final ResultSetHandler<T> handler;

  public ResultSetHandlerIterator(ResultSet rs, ResultSetHandler<T> handler) {
    super(rs);
    this.handler = handler;
  }

  public ResultSetHandlerIterator(ResultSet rs, ResultSetHandlerFactory<T> factory) {
    super(rs);
    try {
      this.handler = factory.newResultSetHandler(rs.getMetaData());
    }
    catch (SQLException e) {
      throw new Sql2oException("Database error: " + e.getMessage(), e);
    }
  }

  @Override
  protected T readNext() throws SQLException {
    return handler.handle(rs);
  }

}
