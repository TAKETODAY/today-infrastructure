package cn.taketoday.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator for a {@link ResultSet}. Tricky part here is getting
 * {@link #hasNext()} to work properly, meaning it can be called multiple times
 * without calling {@link #next()}.
 *
 * @author TODAY
 */
public abstract class AbstractResultSetIterator<T> implements Iterator<T> {
  // fields needed to read result set
  protected ResultSet rs;

  protected AbstractResultSetIterator(ResultSet rs) {
    this.rs = rs;
  }

  // fields needed to properly implement
  private ResultSetValue<T> next; // keep track of next item in case hasNext() is called multiple times
  private boolean resultSetFinished; // used to note when result set exhausted

  @Override
  public boolean hasNext() {
    // check if we already fetched next item
    if (next != null) {
      return true;
    }
    // check if result set already finished
    if (resultSetFinished) {
      return false;
    }
    // now fetch next item
    next = safeReadNext();
    // check if we got something
    if (next != null) {
      return true;
    }
    // no more items
    resultSetFinished = true;
    return false;
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    final ResultSetValue<T> result = next;
    next = null;
    return result.value;
  }

  private ResultSetValue<T> safeReadNext() {
    try {
      return rs.next() ? new ResultSetValue<>(readNext()) : null;
    }
    catch (SQLException ex) {
      throw new Sql2oException("Database error: " + ex.getMessage(), ex);
    }
  }

  protected abstract T readNext() throws SQLException;

  static final class ResultSetValue<T> {
    public final T value;

    public ResultSetValue(T value) {
      this.value = value;
    }
  }

}
