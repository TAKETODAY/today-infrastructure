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

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;

import infra.dao.IncorrectResultSizeDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 19:42
 */
class ResultSetIteratorTests {

  @Test
  void shouldIterateOverResultSet() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = new ArrayList<>();
    while (iterator.hasNext()) {
      results.add(iterator.next());
    }

    assertThat(results).containsExactly("value1", "value2");
  }

  @Test
  void shouldReturnCurrentIndex() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString(1)).thenReturn("value1");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThat(iterator.getCurrentIndex()).isEqualTo(-1);
    iterator.next();
    assertThat(iterator.getCurrentIndex()).isEqualTo(0);
  }

  @Test
  void shouldThrowExceptionWhenNextCalledOnEmptyResultSet() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.next())
            .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void shouldHandleHasNextCalledMultipleTimes() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString(1)).thenReturn("value1");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo("value1");
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldCloseResultSet() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    iterator.close();

    verify(mockResultSet).close();
  }

  @Test
  void shouldReturnStream() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.stream().toList();

    assertThat(results).containsExactly("value1", "value2");
  }

  @Test
  void shouldReturnParallelStream() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.parallelStream().toList();

    assertThat(results).containsExactly("value1", "value2");
  }

  @Test
  void shouldReturnAsIterable() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    ResultSetIterable<String> iterable = iterator.asIterable();

    List<String> results = new ArrayList<>();
    for (String value : iterable) {
      results.add(value);
    }

    assertThat(results).containsExactly("value1", "value2");
  }

  @Test
  void shouldConsumeAllElements() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> results = new ArrayList<>();

    iterator.consume(results::add);

    assertThat(results).containsExactly("value1", "value2");
  }

  @Test
  void shouldReturnUniqueElement() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString(1)).thenReturn("uniqueValue");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    String result = iterator.unique();

    assertThat(result).isEqualTo("uniqueValue");
  }

  @Test
  void shouldThrowExceptionWhenMultipleElementsInUnique() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    given(mockResultSet.next()).willReturn(true, true, false);
    given(mockResultSet.getString(1)).willReturn("1");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(iterator::unique)
            .isInstanceOf(IncorrectResultSizeDataAccessException.class);
  }

  @Test
  void shouldReturnFirstElement() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("firstValue", "secondValue");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    String result = iterator.first();

    assertThat(result).isEqualTo("firstValue");
  }

  @Test
  void shouldReturnNullWhenNoElementsInFirst() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    String result = iterator.first();

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnList() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.list();

    assertThat(results).containsExactly("value1", "value2");
  }

  @Test
  void shouldReturnListWithInitialCapacity() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.list(10);

    assertThat(results).containsExactly("value1", "value2");
    assertThat(results).hasSize(2);
  }

  @Test
  void shouldCollectIntoCollection() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> collection = new ArrayList<>();

    iterator.collect(collection);

    assertThat(collection).containsExactly("value1", "value2");
  }

  @Test
  void shouldHandleSQLExceptionInReadNext() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("Database error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.hasNext())
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldTryAdvanceWhenHasNext() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString(1)).thenReturn("value1");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> results = new ArrayList<>();

    boolean result = iterator.tryAdvance(results::add);

    assertThat(result).isTrue();
    assertThat(results).containsExactly("value1");
  }

  @Test
  void shouldNotTryAdvanceWhenNoNext() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> results = new ArrayList<>();

    boolean result = iterator.tryAdvance(results::add);

    assertThat(result).isFalse();
    assertThat(results).isEmpty();
  }

  @Test
  void shouldThrowExceptionWhenTryAdvanceWithNullAction() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    given(mockResultSet.next()).willReturn(true);
    given(mockResultSet.getString(1)).willReturn("1");
    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.tryAdvance(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void shouldTrySplitReturnNull() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    Spliterator<String> result = iterator.trySplit();

    assertThat(result).isNull();
  }

  @Test
  void shouldEstimateSizeReturnMaxValue() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    long size = iterator.estimateSize();

    assertThat(size).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  void shouldReturnOrderedCharacteristics() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    int characteristics = iterator.characteristics();

    assertThat(characteristics).isEqualTo(Spliterator.ORDERED);
  }

  @Test
  void shouldForEachRemainingProcessAllElements() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> results = new ArrayList<>();

    iterator.forEachRemaining(results::add);

    assertThat(results).containsExactly("value1", "value2");
    verify(mockResultSet).close();
  }

  @Test
  void shouldForEachRemainingHandleSQLException() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("Database error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> results = new ArrayList<>();

    assertThatThrownBy(() -> iterator.forEachRemaining(results::add))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");

    verify(mockResultSet).close();
  }

  @Test
  void shouldUniqueReturnNullWhenNoElements() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    String result = iterator.unique();

    assertThat(result).isNull();
  }

  @Test
  void shouldUniqueThrowExceptionWhenMoreThanOneElement() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);

    given(mockResultSet.next()).willReturn(true, true, true);
    given(mockResultSet.getString(1)).willReturn("1");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.unique())
            .isInstanceOf(IncorrectResultSizeDataAccessException.class);
    verify(mockResultSet).close();
  }

  @Test
  void shouldUniqueReturnElementWhenExactlyOne() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString(1)).thenReturn("uniqueValue");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    String result = iterator.unique();

    assertThat(result).isEqualTo("uniqueValue");
    verify(mockResultSet).close();
  }

  @Test
  void shouldListWithZeroInitialCapacity() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.list(0);

    assertThat(results).containsExactly("value1", "value2");
  }

  @Test
  void shouldThrowExceptionWhenListWithNegativeCapacity() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    // ArrayList itself will throw IllegalArgumentException for negative capacity
    assertThatThrownBy(() -> iterator.list(-1))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCollectHandleSQLException() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("Database error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> collection = new ArrayList<>();

    assertThatThrownBy(() -> iterator.collect(collection))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");

    verify(mockResultSet).close();
  }

  @Test
  void shouldHandleNextCalledMultipleTimesWithoutHasNext() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString(1)).thenReturn("value1");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    String result1 = iterator.next();
    assertThat(result1).isEqualTo("value1");

    assertThatThrownBy(() -> iterator.next())
            .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void shouldHandleHasNextAfterNext() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString(1)).thenReturn("value1");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    String result = iterator.next();
    boolean hasMore = iterator.hasNext();

    assertThat(result).isEqualTo("value1");
    assertThat(hasMore).isFalse();
  }

  @Test
  void shouldHandleExceptionInReadNextDuringNext() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString(1)).thenThrow(new SQLException("Read error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(iterator::next)
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldHandleExceptionInReadNextDuringHasNext() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("Read error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(iterator::hasNext)
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldCloseBeIdempotent() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    iterator.close();

    verify(mockResultSet).close();
  }

  @Test
  void shouldGetCurrentIndexAfterMultipleOperations() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(true, true, true, false);
    when(mockResultSet.getString(1)).thenReturn("value1", "value2", "value3");

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThat(iterator.getCurrentIndex()).isEqualTo(-1);

    iterator.next();
    assertThat(iterator.getCurrentIndex()).isEqualTo(0);

    iterator.next();
    assertThat(iterator.getCurrentIndex()).isEqualTo(1);

    iterator.hasNext(); // Should not increment index
    assertThat(iterator.getCurrentIndex()).isEqualTo(1);

    iterator.next();
    assertThat(iterator.getCurrentIndex()).isEqualTo(2);
  }

  @Test
  void shouldHandleEmptyResultSetInStream() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.stream().toList();

    assertThat(results).isEmpty();
  }

  @Test
  void shouldHandleExceptionInStream() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("Stream error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.stream().toList())
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldHandleEmptyResultSetInParallelStream() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.parallelStream().toList();

    assertThat(results).isEmpty();
  }

  @Test
  void shouldHandleExceptionInParallelStream() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("Stream error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.parallelStream().toList())
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldHandleEmptyResultSetInAsIterable() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    ResultSetIterable<String> iterable = iterator.asIterable();

    List<String> results = new ArrayList<>();
    for (String value : iterable) {
      results.add(value);
    }

    assertThat(results).isEmpty();
  }

  @Test
  void shouldHandleExceptionInAsIterable() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("Iterable error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    ResultSetIterable<String> iterable = iterator.asIterable();

    List<String> results = new ArrayList<>();
    assertThatThrownBy(() -> {
      for (String value : iterable) {
        results.add(value);
      }
    }).isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldHandleEmptyResultSetInConsume() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> results = new ArrayList<>();

    iterator.consume(results::add);

    assertThat(results).isEmpty();
  }

  @Test
  void shouldHandleExceptionInConsume() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("Consume error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> results = new ArrayList<>();

    assertThatThrownBy(() -> iterator.consume(results::add))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldHandleEmptyResultSetInFirst() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    String result = iterator.first();

    assertThat(result).isNull();
  }

  @Test
  void shouldHandleExceptionInFirst() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("First error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.first())
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldHandleEmptyResultSetInList() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.list();

    assertThat(results).isEmpty();
  }

  @Test
  void shouldHandleExceptionInList() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("List error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.list())
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldHandleEmptyResultSetInListWithCapacity() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    List<String> results = iterator.list(5);

    assertThat(results).isEmpty();
  }

  @Test
  void shouldHandleExceptionInListWithCapacity() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenThrow(new SQLException("List error"));

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);

    assertThatThrownBy(() -> iterator.list(5))
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("Database read error");
  }

  @Test
  void shouldHandleEmptyResultSetInCollect() throws SQLException {
    ResultSet mockResultSet = mock(ResultSet.class);
    when(mockResultSet.next()).thenReturn(false);

    ResultSetIterator<String> iterator = new TestResultSetIterator(mockResultSet);
    List<String> collection = new ArrayList<>();

    iterator.collect(collection);

    assertThat(collection).isEmpty();
  }

  private static class TestResultSetIterator extends ResultSetIterator<String> {
    public TestResultSetIterator(ResultSet rs) {
      super(rs);
    }

    @Override
    protected String readNext(ResultSet resultSet) throws SQLException {
      return resultSet.getString(1);
    }
  }

}