/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import infra.dao.IncorrectResultSizeDataAccessException;
import infra.jdbc.support.JdbcUtils;

/**
 * Iterator for a {@link ResultSet}. Tricky part here is getting
 * {@link #hasNext()} to work properly, meaning it can be called multiple times
 * without calling {@link #next()}.
 *
 * @param <T> the type of elements returned by this iterator
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ResultSetIterator<T> implements Iterator<T>, Spliterator<T>, AutoCloseable {
  // fields needed to read rows set
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

  private boolean resultSetFinished; // used to note when rows set exhausted

  @Override
  public boolean hasNext() {
    // check if we already fetched next item
    if (next != null) {
      return true;
    }
    // check if rows set already finished
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

  @Nullable
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
    T next = readNext();
    if (next != null) {
      action.accept(next);
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
    final ResultSet rows = this.resultSet;
    try {
      while (rows.next()) {
        action.accept(readNext(rows));
      }
    }
    catch (SQLException ex) {
      throw handleReadError(ex);
    }
    finally {
      close();
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Common methods
  // -----------------------------------------------------------------------------------------------

  /**
   * Returns a sequential {@code Stream} with this iterator as its source.
   *
   * @return a sequential {@code Stream} over the elements in this iterator
   * @apiNote This method must be used within a try-with-resources statement or similar
   * control structure to ensure that the stream's open connection is closed
   * promptly after the stream's operations have completed.
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
   * @apiNote This method must be used within a try-with-resources statement or similar
   * control structure to ensure that the stream's open connection is closed
   * promptly after the stream's operations have completed.
   * @since 4.0
   */
  public Stream<T> parallelStream() {
    return StreamSupport.stream(this, true)
            .onClose(this::close);
  }

  /**
   * Returns an {@link ResultSetIterable} instance that allows iteration over the
   * result set encapsulated by this iterator. The returned iterable can be used
   * in enhanced for-loops or other contexts where an {@link Iterable} is required.
   * <p>
   * The returned {@link ResultSetIterable} also implements {@link AutoCloseable},
   * ensuring that resources such as the underlying {@link ResultSet} and
   * database connection are properly closed after use. It is recommended to use
   * the returned iterable within a try-with-resources statement to ensure timely
   * resource cleanup.
   * <p>
   * Example usage:
   * <pre>{@code
   * try (ResultSetIterable<MyType> iterable = resultSetIterator.asIterable()) {
   *   for (MyType item : iterable) {
   *     System.out.println(item);
   *   }
   * }
   * }</pre>
   * <p>
   * Alternatively, you can use it with streams:
   * <pre>{@code
   * try (ResultSetIterable<MyType> iterable = resultSetIterator.asIterable()) {
   *   Stream<MyType> stream = iterable.stream();
   *   stream.forEach(System.out::println);
   * }
   * }</pre>
   *
   * @return an {@link ResultSetIterable} instance that provides access to the
   * elements of the result set encapsulated by this iterator
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
   * Performs the given action for each element of the result set until all elements
   * have been processed or the action throws an exception. The action is performed
   * in the order of iteration, as defined by the underlying {@link ResultSet}.
   * <p>
   * This method ensures that the underlying resources (e.g., the {@link ResultSet})
   * are properly closed after the operation completes, regardless of whether the
   * operation completes normally or due to an exception. It is recommended to use
   * this method when you need to process all elements in the result set without
   * manually managing resource cleanup.
   * <p>
   * Example usage:
   * <pre>{@code
   * resultSetIterator.consume(item -> {
   *   System.out.println("Processing item: " + item);
   * });
   * }</pre>
   * <p>
   * If an exception occurs during processing, it is wrapped in a runtime exception
   * and rethrown. The exact type of the runtime exception depends on the implementation
   * of the {@link #handleReadError(SQLException)} method.
   *
   * @param consumer the action to be performed for each element in the result set
   * @throws NullPointerException if the specified consumer is null
   * @since 4.0
   */
  public void consume(Consumer<T> consumer) {
    final ResultSet rows = this.resultSet;
    try {
      while (rows.next()) {
        consumer.accept(readNext(rows));
      }
    }
    catch (SQLException ex) {
      throw handleReadError(ex);
    }
    finally {
      close();
    }
  }

  /**
   * Retrieves the unique element from the result set encapsulated by this iterator.
   * If the result set contains more than one element, an exception is thrown to indicate
   * that the result size does not match the expected size of one.
   *
   * <p>This method ensures that the underlying resources (e.g., the {@link ResultSet}) are
   * properly closed after the operation completes, regardless of whether the operation
   * completes normally or due to an exception. It is recommended to use this method when
   * you expect exactly one element in the result set and want to retrieve it directly.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * try {
   *   MyType uniqueItem = resultSetIterator.unique();
   *   if (uniqueItem != null) {
   *     System.out.println("Unique item: " + uniqueItem);
   *   }
   *   else {
   *     System.out.println("No item found.");
   *   }
   * }
   * catch (IncorrectResultSizeDataAccessException e) {
   *   System.err.println("Expected one item but found more than one.");
   * }
   * }</pre>
   *
   * <p><b>Note:</b> If the result set contains no elements, this method returns {@code null}.
   * If more than one element exists, an {@link IncorrectResultSizeDataAccessException} is thrown.
   *
   * @return the unique element from the result set, or {@code null} if no element exists
   * @throws IncorrectResultSizeDataAccessException if more than one element exists in the result set
   */
  @Nullable
  public T unique() throws IncorrectResultSizeDataAccessException {
    try {
      T returnValue = readNext();
      if (returnValue != null && readNext() != null) {
        throw new IncorrectResultSizeDataAccessException(1);
      }
      return returnValue;
    }
    finally {
      close();
    }
  }

  /**
   * Returns the first element from the result set encapsulated by this iterator, if available.
   * If the result set is empty, this method returns {@code null}.
   * <p>
   * This method ensures that the underlying resources (e.g., the {@link ResultSet}) are
   * properly closed after the operation completes, regardless of whether the operation
   * completes normally or due to an exception. It is recommended to use this method when
   * you need to retrieve only the first element from the result set without manually
   * managing resource cleanup.
   * <p>
   * Example usage:
   * <pre>{@code
   * try {
   *   MyType firstItem = resultSetIterator.first();
   *   if (firstItem != null) {
   *     System.out.println("First item: " + firstItem);
   *   }
   *   else {
   *     System.out.println("No items found in the result set.");
   *   }
   * }
   * catch (RuntimeException e) {
   *   System.err.println("An error occurred while reading the first item: " + e.getMessage());
   * }
   * }</pre>
   * <p>
   * If an exception occurs during the retrieval process, it is wrapped in a runtime
   * exception and rethrown. The exact type of the runtime exception depends on the
   * implementation of the {@link #handleReadError(SQLException)} method.
   *
   * @return the first element from the result set, or {@code null} if the result set is empty
   */
  @Nullable
  public T first() {
    try {
      return readNext();
    }
    finally {
      close();
    }
  }

  /**
   * Returns a list of entities collected by the method.
   * This method initializes an empty list, populates it by calling
   * the {@code collect} method, and then returns the populated list.
   *
   * <p>Example usage:
   * <pre>{@code
   *   List<T> result = list();
   *
   *   // Iterate through the list
   *   for (T entity : result) {
   *     System.out.println(entity);
   *   }
   * }</pre>
   *
   * @return a {@code List<T>} containing the collected entities
   */
  public List<T> list() {
    ArrayList<T> entities = new ArrayList<>();
    collect(entities);
    return entities;
  }

  /**
   * Creates and returns a list of elements collected using the specified
   * initial capacity. The method initializes an ArrayList with the given
   * capacity, collects elements into it, and returns the populated list.
   *
   * <p>Example usage:
   * <pre>{@code
   *   List<String> result = list(10);
   *   System.out.println(result);
   * }</pre>
   *
   * @param initialCapacity the initial capacity of the list to be created;
   * this value should be non-negative
   * @return a list of elements collected by the {@code collect} method
   * @throws IllegalArgumentException if the initial capacity is negative
   */
  public List<T> list(int initialCapacity) {
    ArrayList<T> entities = new ArrayList<>(initialCapacity);
    collect(entities);
    return entities;
  }

  /**
   * Collects all remaining entities from the current {@code ResultSet}
   * and adds them to the provided collection. This method reads each row
   * from the {@code ResultSet}, converts it into an entity using the
   * {@code readNext} method, and adds the entity to the specified collection.
   * <p>
   * If an SQL exception occurs during the reading process, it is handled
   * by the {@code handleReadError} method, which may throw a runtime exception.
   * After processing, the {@code ResultSet} is closed automatically.
   *
   * <p>Example usage:
   * <pre>{@code
   *   List<MyEntity> entityList = new ArrayList<>();
   *   myDataCollector.collect(entityList);
   *
   *   // The entityList now contains all entities from the ResultSet
   * }</pre>
   *
   * @param entities the collection to which the entities will be added;
   * must not be null
   */
  @SuppressWarnings("NullAway")
  public void collect(Collection<T> entities) {
    final ResultSet resultSet = this.resultSet;
    try {
      while (resultSet.next()) {
        entities.add(readNext(resultSet));
      }
    }
    catch (SQLException ex) {
      throw handleReadError(ex);
    }
    finally {
      close();
    }
  }

  @Nullable
  private ResultSetValue<T> safeReadNext() {
    final ResultSet resultSet = this.resultSet;
    try {
      return resultSet.next() ? new ResultSetValue<>(readNext(resultSet)) : null;
    }
    catch (SQLException ex) {
      throw handleReadError(ex);
    }
  }

  @Nullable
  private T readNext() {
    final ResultSet resultSet = this.resultSet;
    try {
      return resultSet.next() ? readNext(resultSet) : null;
    }
    catch (SQLException ex) {
      throw handleReadError(ex);
    }
  }

  protected RuntimeException handleReadError(SQLException ex) {
    return new PersistenceException("Database read error: " + ex.getMessage(), ex);
  }

  @Nullable
  protected abstract T readNext(ResultSet resultSet) throws SQLException;

  static final class ResultSetValue<T> {

    @Nullable
    public final T value;

    private ResultSetValue(@Nullable T value) {
      this.value = value;
    }
  }

}
