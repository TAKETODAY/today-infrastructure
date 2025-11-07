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