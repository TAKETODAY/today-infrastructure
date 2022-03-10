/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.dao.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;

import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.IncorrectResultSizeDataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.dao.TypeMismatchDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 20.10.2004
 */
public class DataAccessUtilsTests {

  @Test
  public void withEmptyCollection() {
    Collection<String> col = new HashSet<>();

    assertThat(DataAccessUtils.uniqueResult(col)).isNull();

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.requiredUniqueResult(col))
            .satisfies(sizeRequirements(1, 0));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.objectResult(col, String.class))
            .satisfies(sizeRequirements(1, 0));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.intResult(col))
            .satisfies(sizeRequirements(1, 0));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.longResult(col))
            .satisfies(sizeRequirements(1, 0));
  }

  @Test
  public void withTooLargeCollection() {
    Collection<String> col = new HashSet<>(2);
    col.add("test1");
    col.add("test2");

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.uniqueResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.requiredUniqueResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.objectResult(col, String.class))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.intResult(col))
            .satisfies(sizeRequirements(1, 2));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
                    DataAccessUtils.longResult(col))
            .satisfies(sizeRequirements(1, 2));
  }

  @Test
  public void withInteger() {
    Collection<Integer> col = new HashSet<>(1);
    col.add(5);

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.objectResult(col, Integer.class)).isEqualTo(Integer.valueOf(5));
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
    assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
    assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
  }

  @Test
  public void withSameIntegerInstanceTwice() {
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
  public void withEquivalentIntegerInstanceTwice() {
    Collection<Integer> col = Arrays.asList(Integer.valueOf(555), Integer.valueOf(555));

    assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
            .isThrownBy(() -> DataAccessUtils.uniqueResult(col))
            .satisfies(sizeRequirements(1, 2));
  }

  @Test
  public void withLong() {
    Collection<Long> col = new HashSet<>(1);
    col.add(5L);

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.objectResult(col, Long.class)).isEqualTo(Long.valueOf(5L));
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("5");
    assertThat(DataAccessUtils.intResult(col)).isEqualTo(5);
    assertThat(DataAccessUtils.longResult(col)).isEqualTo(5);
  }

  @Test
  public void withString() {
    Collection<String> col = new HashSet<>(1);
    col.add("test1");

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo("test1");
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo("test1");
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo("test1");

    assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
            DataAccessUtils.intResult(col));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
            DataAccessUtils.longResult(col));
  }

  @Test
  public void withDate() {
    Date date = new Date();
    Collection<Date> col = new HashSet<>(1);
    col.add(date);

    assertThat(DataAccessUtils.uniqueResult(col)).isEqualTo(date);
    assertThat(DataAccessUtils.requiredUniqueResult(col)).isEqualTo(date);
    assertThat(DataAccessUtils.objectResult(col, Date.class)).isEqualTo(date);
    assertThat(DataAccessUtils.objectResult(col, String.class)).isEqualTo(date.toString());

    assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
            DataAccessUtils.intResult(col));

    assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(() ->
            DataAccessUtils.longResult(col));
  }

  @Test
  public void exceptionTranslationWithNoTranslation() {
    MapPersistenceExceptionTranslator mpet = new MapPersistenceExceptionTranslator();
    RuntimeException in = new RuntimeException();
    assertThat(DataAccessUtils.translateIfNecessary(in, mpet)).isSameAs(in);
  }

  @Test
  public void exceptionTranslationWithTranslation() {
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

  public static class MapPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

    // in to out
    private final Map<RuntimeException, RuntimeException> translations = new HashMap<>();

    public void addTranslation(RuntimeException in, RuntimeException out) {
      this.translations.put(in, out);
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
      return (DataAccessException) translations.get(ex);
    }
  }

}
