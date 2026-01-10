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

package infra.jdbc.parsing;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import infra.jdbc.ParameterBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 15:32
 */
class ParameterIndexHolderTests {

  @Test
  void shouldCreateDefaultParameterIndexHolder() {
    ParameterIndexHolder holder = ParameterIndexHolder.valueOf(1);
    assertThat(holder).isInstanceOf(DefaultParameterIndexHolder.class);
    DefaultParameterIndexHolder defaultHolder = (DefaultParameterIndexHolder) holder;
    assertThat(defaultHolder.getIndex()).isEqualTo(1);
  }

  @Test
  void shouldCreateListParameterIndexHolder() {
    ParameterIndexHolder holder = ParameterIndexHolder.valueOf(List.of(1, 2, 3));
    assertThat(holder).isInstanceOf(ListParameterIndexApplier.class);
  }

  @Test
  void shouldBindDefaultParameterIndexHolder() throws SQLException {
    ParameterIndexHolder holder = ParameterIndexHolder.valueOf(1);
    ParameterBinder binder = mock(ParameterBinder.class);
    PreparedStatement statement = mock(PreparedStatement.class);

    holder.bind(binder, statement);

    verify(binder).bind(statement, 1);
  }

  @Test
  void shouldBindListParameterIndexHolder() throws SQLException {
    ParameterIndexHolder holder = ParameterIndexHolder.valueOf(List.of(1, 2, 3));
    ParameterBinder binder = mock(ParameterBinder.class);
    PreparedStatement statement = mock(PreparedStatement.class);

    holder.bind(binder, statement);

    verify(binder).bind(statement, 1);
    verify(binder).bind(statement, 2);
    verify(binder).bind(statement, 3);
  }

  @Test
  void shouldIterateDefaultParameterIndexHolder() {
    ParameterIndexHolder holder = ParameterIndexHolder.valueOf(5);
    Iterator<Integer> iterator = holder.iterator();

    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo(5);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldIterateListParameterIndexHolder() {
    ParameterIndexHolder holder = ParameterIndexHolder.valueOf(List.of(1, 2, 3));
    Iterator<Integer> iterator = holder.iterator();

    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo(1);
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo(2);
    assertThat(iterator.hasNext()).isTrue();
    assertThat(iterator.next()).isEqualTo(3);
    assertThat(iterator.hasNext()).isFalse();
  }

  @Test
  void shouldAddIndexToListParameterIndexHolder() {
    ListParameterIndexApplier holder = new ListParameterIndexApplier(new ArrayList<>(List.of(1, 2)));
    holder.addIndex(3);

    Iterator<Integer> iterator = holder.iterator();
    assertThat(iterator.next()).isEqualTo(1);
    assertThat(iterator.next()).isEqualTo(2);
    assertThat(iterator.next()).isEqualTo(3);
  }

  @Test
  void shouldEqualsForDefaultParameterIndexHolder() {
    ParameterIndexHolder holder1 = ParameterIndexHolder.valueOf(1);
    ParameterIndexHolder holder2 = ParameterIndexHolder.valueOf(1);
    ParameterIndexHolder holder3 = ParameterIndexHolder.valueOf(2);

    assertThat(holder1).isEqualTo(holder2);
    assertThat(holder1).isNotEqualTo(holder3);
    assertThat(holder1).isNotEqualTo(null);
    assertThat(holder1).isNotEqualTo(new Object());
  }

  @Test
  void shouldEqualsForListParameterIndexHolder() {
    ParameterIndexHolder holder1 = ParameterIndexHolder.valueOf(List.of(1, 2, 3));
    ParameterIndexHolder holder2 = ParameterIndexHolder.valueOf(List.of(1, 2, 3));
    ParameterIndexHolder holder3 = ParameterIndexHolder.valueOf(List.of(1, 2, 4));

    assertThat(holder1).isEqualTo(holder2);
    assertThat(holder1).isNotEqualTo(holder3);
    assertThat(holder1).isNotEqualTo(null);
    assertThat(holder1).isNotEqualTo(new Object());
  }

  @Test
  void shouldHashCodeForDefaultParameterIndexHolder() {
    ParameterIndexHolder holder1 = ParameterIndexHolder.valueOf(1);
    ParameterIndexHolder holder2 = ParameterIndexHolder.valueOf(1);

    assertThat(holder1.hashCode()).isEqualTo(holder2.hashCode());
  }

  @Test
  void shouldHashCodeForListParameterIndexHolder() {
    ParameterIndexHolder holder1 = ParameterIndexHolder.valueOf(List.of(1, 2, 3));
    ParameterIndexHolder holder2 = ParameterIndexHolder.valueOf(List.of(1, 2, 3));

    assertThat(holder1.hashCode()).isEqualTo(holder2.hashCode());
  }

  @Test
  void shouldToStringForDefaultParameterIndexHolder() {
    ParameterIndexHolder holder = ParameterIndexHolder.valueOf(1);
    assertThat(holder.toString()).contains("index=1");
  }

  @Test
  void shouldToStringForListParameterIndexHolder() {
    ParameterIndexHolder holder = ParameterIndexHolder.valueOf(List.of(1, 2, 3));
    assertThat(holder.toString()).contains("indices=[1, 2, 3]");
  }

}