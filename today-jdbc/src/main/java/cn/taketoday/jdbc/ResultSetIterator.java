/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Nullable;

/**
 * Iterator for a {@link ResultSet}. Tricky part here is getting
 * {@link #hasNext()} to work properly, meaning it can be called multiple times
 * without calling {@link #next()}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ResultSetIterator<T> implements Iterator<T>, Spliterator<T>, AutoCloseable {
  // fields needed to read result set
  protected final ResultSet resultSet;

  /**
   * Index of objects returned using next(), and as such, visible to users.
   */
  protected int iteratorIndex = -1;

  protected ResultSetIterator(ResultSet rs) {
    this.resultSet = rs;
  }

  // fields needed to properly implement
  @Nullable
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
    ResultSetValue<T> result = next;
    if (result == null) {
      if (resultSetFinished) {
        throw new NoSuchElementException();
      }
      result = safeReadNext();
      if (result == null) {
        resultSetFinished = true;
        throw new NoSuchElementException();
      }
    }
    else {
      next = null;
    }

    iteratorIndex++;
    return result.value;
  }

  @Override
  public void close() {
    JdbcUtils.closeQuietly(resultSet);
  }

  /**
   * Get the current item index. The first item has the index 0.
   *
   * @return -1 if the first item has not been retrieved. The index of the current item retrieved.
   */
  public int getCurrentIndex() {
    return iteratorIndex;
  }

  /**
   * If a remaining element exists, performs the given action on it,
   * returning {@code true}; else returns {@code false}.  If this
   * Spliterator is {@link #ORDERED} the action is performed on the
   * next element in encounter order.  Exceptions thrown by the
   * action are relayed to the caller.
   * <p>
   * Subsequent behavior of a spliterator is unspecified if the action throws
   * an exception.
   *
   * @param action The action
   * @return {@code false} if no remaining elements existed
   * upon entry to this method, else {@code true}.
   * @throws NullPointerException if the specified action is null
   */
  @Override
  public boolean tryAdvance(Consumer<? super T> action) {
    if (hasNext()) {
      action.accept(next());
      return true;
    }
    return false;
  }

  @Override
  @Nullable
  public Spliterator<T> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return Spliterator.ORDERED;
  }

  /**
   * Performs the given action for each remaining element until all elements
   * have been processed or the action throws an exception.  Actions are
   * performed in the order of iteration, if that order is specified.
   * Exceptions thrown by the action are relayed to the caller.
   * <p>
   * The behavior of an iterator is unspecified if the action modifies the
   * collection in any way (even by calling the {@link #remove remove} method
   * or other mutator methods of {@code Iterator} subtypes),
   * unless an overriding class has specified a concurrent modification policy.
   * <p>
   * Subsequent behavior of an iterator is unspecified if the action throws an
   * exception.
   *
   * @param action The action to be performed for each element
   * @throws NullPointerException if the specified action is null
   * @implSpec <p>The default implementation behaves as if:
   * <pre>{@code
   *     while (hasNext())
   *         action.accept(next());
   * }</pre>
   * @since 4.0
   */
  @Override
  public void forEachRemaining(Consumer<? super T> action) {
    while (hasNext())
      action.accept(next());
  }

  // -----------------------------------------------------------------------------------------------
  // Common methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns a sequential {@code Stream} with this iterator as its source.
   *
   * @return a sequential {@code Stream} over the elements in this iterator
   * @since 4.0
   */
  public Stream<T> stream() {
    return StreamSupport.stream(this, false)
            .onClose(this::close);
  }

  /**
   * Returns a possibly parallel {@code Stream} with this iterator as its
   * source. It is allowable for this method to return a sequential stream.
   *
   * @return a possibly parallel {@code Stream} over the elements in this
   * iterator
   * @since 4.0
   */
  public Stream<T> parallelStream() {
    return StreamSupport.stream(this, true)
            .onClose(this::close);
  }

  /**
   * @since 4.0
   */
  public ResultSetIterable<T> asIterable() {
    return new ResultSetIterable<>() {

      @Override
      public Iterator<T> iterator() {
        return ResultSetIterator.this;
      }

      @Override
      public Spliterator<T> spliterator() {
        return ResultSetIterator.this;
      }

      @Override
      public void close() {
        ResultSetIterator.this.close();
      }
    };
  }

  /**
   * consume elements
   *
   * @since 4.0
   */
  public void consume(Consumer<T> consumer) {
    try {
      while (hasNext()) {
        consumer.accept(next());
      }
    }
    finally {
      close();
    }
  }

  /**
   * Fetch unique element
   *
   * @since 4.0
   */
  @Nullable
  public T unique() {
    try {
      T returnValue = null;
      while (hasNext()) {
        if (returnValue != null) {
          throw new IncorrectResultSizeDataAccessException(1);
        }
        returnValue = next();
      }
      return returnValue;
    }
    finally {
      close();
    }
  }

  /**
   * Fetch first element
   *
   * @since 4.0
   */
  @Nullable
  public T first() {
    try {
      while (hasNext()) {
        T returnValue = next();
        if (returnValue != null) {
          return returnValue;
        }
      }
      return null;
    }
    finally {
      close();
    }
  }

  /**
   * Fetch list of elements
   *
   * @since 4.0
   */
  public List<T> list() {
    try {
      ArrayList<T> entities = new ArrayList<>();
      while (hasNext()) {
        entities.add(next());
      }
      return entities;
    }
    finally {
      close();
    }
  }

  private ResultSetValue<T> safeReadNext() {
    final ResultSet resultSet = this.resultSet;
    try {
      return resultSet.next() ? new ResultSetValue<>(readNext(resultSet)) : null;
    }
    catch (SQLException ex) {
      throw handleReadError(ex);
    }
  }

  protected RuntimeException handleReadError(SQLException ex) {
    return new PersistenceException("Database error: " + ex.getMessage(), ex);
  }

  protected abstract T readNext(ResultSet resultSet) throws SQLException;

  static final class ResultSetValue<T> {
    public final T value;

    private ResultSetValue(T value) {
      this.value = value;
    }
  }

}
