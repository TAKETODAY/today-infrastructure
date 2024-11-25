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

package infra.dao.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Consumer;

import infra.dao.IncorrectResultSizeDataAccessException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.dao.TypeMismatchDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 20.10.2004
 */
public class DataAccessUtilsTests {

  @Test
  void withEmptyCollection() {
    Collection<String> col = new HashSet<>();

    assertThat(DataAccessUtils.uniqueResult(col)).isNull();

    assertThat(DataAccessUtils.singleResult(col)).isNull();
    assertThat(DataAccessUtils.singleResult(col.stream())).isNull();
    assertThat(DataAccessUtils.singleResult(col.iterator())).isNull();
    assertThat(DataAccessUtils.optionalResult(col)).isEmpty();
    assertThat(DataAccessUtils.optionalResult(col.stream())).isEmpty();
    assertThat(DataAccessUtils.optionalResult(col.iterator())).isEmpty();

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.requiredSingleResult(col))
            .satisfies(sizeRequirements(1, 0));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.requiredUniqueResult(col))
            .satisfies(sizeRequirements(1, 0));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.objectResult(col, String.class))
            .satisfies(sizeRequirements(1, 0));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.intResult(col))
            .satisfies(sizeRequirements(1, 0));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.longResult(col))
            .satisfies(sizeRequirements(1, 0));
  }

  @Test
  void withTooLargeCollection() {
    Collection<String> col = new HashSet<>(2);
    col.add("test1");
    col.add("test2");

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.uniqueResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.requiredUniqueResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.objectResult(col, String.class))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.intResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.longResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.requiredSingleResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.singleResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.singleResult(col.stream()))
            .satisfies(sizeRequirements(1));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.singleResult(col.iterator()))
            .satisfies(sizeRequirements(1));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.optionalResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.optionalResult(col.stream()))
            .satisfies(sizeRequirements(1));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.optionalResult(col.iterator()))
            .satisfies(sizeRequirements(1));
  }

  @Test
  void withNullValueInCollection() {
    Collection<String> col = new HashSet<>();
    col.add(null);

    assertThat(DataAccessUtils.uniqueResult(col)).isNull();

    assertThat(DataAccessUtils.singleResult(col)).isNull();
    assertThat(DataAccessUtils.singleResult(col.stream())).isNull();
    assertThat(DataAccessUtils.singleResult(col.iterator())).isNull();
    assertThat(DataAccessUtils.optionalResult(col)).isEmpty();
    assertThat(DataAccessUtils.optionalResult(col.stream())).isEmpty();
    assertThat(DataAccessUtils.optionalResult(col.iterator())).isEmpty();

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.requiredSingleResult(col));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.requiredUniqueResult(col));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.objectResult(col, String.class));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.intResult(col));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.longResult(col));
  }

  @Test
  void withInteger() {
    Collection<Integer> col = new HashSet<>(1);
    col.add(5);

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.objectResult(col, Integer.class)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
    assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
    assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
    assertThat(DataAccessUtils.requiredSingleResult(col)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.singleResult(col)).isEqualTo(5);
    assertThat(DataAccessUtils.singleResult(col.stream())).isEqualTo(5);
    assertThat(DataAccessUtils.singleResult(col.iterator())).isEqualTo(5);
    assertThat(DataAccessUtils.optionalResult(col)).isEqualTo(Optional.of(5));
    assertThat(DataAccessUtils.optionalResult(col.stream())).isEqualTo(Optional.of(5));
    assertThat(DataAccessUtils.optionalResult(col.iterator())).isEqualTo(Optional.of(5));
  }

  @Test
  void withSameIntegerInstanceTwice() {
    Integer i = 5;
    Collection<Integer> col = new ArrayList<>(1);
    col.add(i);
    col.add(i);

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.objectResult(col, Integer.class)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
    assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
    assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
  }

  @Test
  void withEquivalentIntegerInstanceTwice() {
    Collection<Integer> col = Arrays.asList(555, 555);

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.uniqueResult(col))
            .satisfies(sizeRequirements(1, 2));
  }

  @Test
  void withLong() {
    Collection<Long> col = new HashSet<>(1);
    col.add(5L);

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.objectResult(col, Long.class)).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
    assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
    assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
    assertThat(DataAccessUtils.requiredSingleResult(col)).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.singleResult(col)).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.singleResult(col.stream())).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.singleResult(col.iterator())).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.optionalResult(col)).isEqualTo(Optional.of(5L));
    assertThat(DataAccessUtils.optionalResult(col.stream())).isEqualTo(Optional.of(5L));
    assertThat(DataAccessUtils.optionalResult(col.iterator())).isEqualTo(Optional.of(5L));
  }

  @Test
  void withString() {
    Collection<String> col = new HashSet<>(1);
    col.add("test1");

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo("test1");
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo("test1");
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("test1");
    assertThat(DataAccessUtils.requiredSingleResult(col)).isEqualTo("test1");
    assertThat(DataAccessUtils.singleResult(col)).isEqualTo("test1");
    assertThat(DataAccessUtils.singleResult(col.stream())).isEqualTo("test1");
    assertThat(DataAccessUtils.singleResult(col.iterator())).isEqualTo("test1");
    assertThat(DataAccessUtils.optionalResult(col)).isEqualTo(Optional.of("test1"));
    assertThat(DataAccessUtils.optionalResult(col.stream())).isEqualTo(Optional.of("test1"));
    assertThat(DataAccessUtils.optionalResult(col.iterator())).isEqualTo(Optional.of("test1"));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.intResult(col));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.longResult(col));
  }

  @Test
  void withDate() {
    Date date = new Date();
    Collection<Date> col = new HashSet<>(1);
    col.add(date);

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(date);
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(date);
    assertThat(DataAccessUtils.objectResult(col, Date.class)).isEqualTo(date);
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo(date.toString());
    assertThat(DataAccessUtils.requiredSingleResult(col)).isEqualTo(date);
    assertThat(DataAccessUtils.singleResult(col)).isEqualTo(date);
    assertThat(DataAccessUtils.singleResult(col.stream())).isEqualTo(date);
    assertThat(DataAccessUtils.singleResult(col.iterator())).isEqualTo(date);
    assertThat(DataAccessUtils.optionalResult(col)).isEqualTo(Optional.of(date));
    assertThat(DataAccessUtils.optionalResult(col.stream())).isEqualTo(Optional.of(date));
    assertThat(DataAccessUtils.optionalResult(col.iterator())).isEqualTo(Optional.of(date));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.intResult(col));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.longResult(col));
  }

  @Test
  void exceptionTranslationWithNoTranslation() {
    MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
    RuntimeException in = new RuntimeException();
    assertThat(DataAccessUtils.translateIfNecessary(in, mpet)).isSameAs(in);
  }

  @Test
  void exceptionTranslationWithTranslation() {
    MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
    RuntimeException in = new RuntimeException("in");
    InvalidDataAccessApiUsageException out = new InvalidDataAccessApiUsageException("out");
    mpet.addTranslation(in, out);
    assertThat(DataAccessUtils.translateIfNecessary(in, mpet)).isSameAs(out);
  }

  private <E extends IncorrectResultSizeDataAccessException> Consumer<E> sizeRequirements(
          int expectedSize, int actualSize) {

    return ex -> {
      assertThat(ex.getExpectedSize()).as("expected size").isEqualTo(expectedSize);
      assertThat(ex.getActualSize()).as("actual size").isEqualTo(actualSize);
    };
  }

  private <E extends IncorrectResultSizeDataAccessException> Consumer<E> sizeRequirements(int expectedSize) {
    return ex -> assertThat(ex.getExpectedSize()).as("expected size").isEqualTo(expectedSize);
  }

}