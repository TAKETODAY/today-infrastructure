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

package infra.lang;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/1 21:13
 */
class EnumerableTests {

  enum Gender implements Enumerable<Integer> {

    MALE(1, "男"),
    FEMALE(0, "女");

    private final int value;
    private final String desc;

    Gender(int value, String desc) {
      this.value = value;
      this.desc = desc;
    }

    @Override
    public Integer getValue() {
      return value;
    }

    @Override
    public String getDescription() {
      return desc;
    }

  }

  @Test
  void introspect() {
    assertThat(Enumerable.of(Gender.class, null)).isNull();

    assertThat(Enumerable.of(Gender.class, 2)).isNull();
    assertThat(Enumerable.of(Gender.class, 1)).isEqualTo(Gender.MALE);
    assertThat(Enumerable.of(Gender.class, 0)).isEqualTo(Gender.FEMALE);

    //
    assertThat(Enumerable.of(Gender.class, 2, Gender.MALE)).isEqualTo(Gender.MALE);
    assertThat(Enumerable.of(Gender.class, 2, (Supplier<Gender>) () -> Gender.MALE)).isEqualTo(Gender.MALE);

  }

  @Test
  void getValue() {
    assertThat(Enumerable.getValue(Gender.class, "MALE")).isEqualTo(1);
    assertThat(Enumerable.getValue(Gender.class, "FEMALE")).isEqualTo(0);
    assertThat(Enumerable.getValue(Gender.class, "FEMALE_")).isNull();

  }

  @Test
  void findReturnsEmptyForNullValue() {
    Optional<Gender> result = Enumerable.find(Gender.class, null);
    assertThat(result).isEmpty();
  }

  @Test
  void findReturnsEmptyForNonExistentValue() {
    Optional<Gender> result = Enumerable.find(Gender.class, 2);
    assertThat(result).isEmpty();
  }

  @Test
  void findReturnsCorrectEnumForExistingValue() {
    Optional<Gender> result = Enumerable.find(Gender.class, 1);
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(Gender.MALE);

    result = Enumerable.find(Gender.class, 0);
    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(Gender.FEMALE);
  }

  @Test
  void ofWithDefaultValueSupplierReturnsDefaultForNullValue() {
    Gender result = Enumerable.of(Gender.class, null, (Supplier) () -> Gender.MALE);
    assertThat(result).isEqualTo(Gender.MALE);
  }

  @Test
  void ofWithDefaultValueSupplierReturnsDefaultForNonExistentValue() {
    Gender result = Enumerable.of(Gender.class, 2, (Supplier) () -> Gender.FEMALE);
    assertThat(result).isEqualTo(Gender.FEMALE);
  }

  @Test
  void ofWithDefaultValueSupplierReturnsCorrectValueForExistingValue() {
    Gender result = Enumerable.of(Gender.class, 1, (Supplier) () -> Gender.FEMALE);
    assertThat(result).isEqualTo(Gender.MALE);
  }

  @Test
  void ofWithDefaultValueReturnsDefaultForNullValue() {
    Gender result = Enumerable.of(Gender.class, null, Gender.MALE);
    assertThat(result).isEqualTo(Gender.MALE);
  }

  @Test
  void ofWithDefaultValueReturnsDefaultForNonExistentValue() {
    Gender result = Enumerable.of(Gender.class, 2, Gender.FEMALE);
    assertThat(result).isEqualTo(Gender.FEMALE);
  }

  @Test
  void ofWithDefaultValueReturnsCorrectValueForExistingValue() {
    Gender result = Enumerable.of(Gender.class, 1, Gender.FEMALE);
    assertThat(result).isEqualTo(Gender.MALE);
  }

  @Test
  void getValueReturnsNullForNonExistentName() {
    Integer value = Enumerable.getValue(Gender.class, "UNKNOWN");
    assertThat(value).isNull();
  }

  @Test
  void getValueReturnsCorrectValueForExistingName() {
    Integer value = Enumerable.getValue(Gender.class, "MALE");
    assertThat(value).isEqualTo(1);

    value = Enumerable.getValue(Gender.class, "FEMALE");
    assertThat(value).isEqualTo(0);
  }

  @Test
  void getValueReturnsCorrectDescription() {
    assertThat(Gender.MALE.getDescription()).isEqualTo("男");
    assertThat(Gender.FEMALE.getDescription()).isEqualTo("女");
  }

  @Test
  void getValueReturnsNameAsDefault() {
    enum TestEnum implements Enumerable<String> {
      FIRST, SECOND
    }

    assertThat(TestEnum.FIRST.getValue()).isEqualTo("FIRST");
    assertThat(TestEnum.SECOND.getValue()).isEqualTo("SECOND");
  }

  @Test
  void getDescriptionReturnsNameAsDefault() {
    enum TestEnum implements Enumerable<Integer> {
      FIRST, SECOND
    }

    assertThat(TestEnum.FIRST.getDescription()).isEqualTo("FIRST");
    assertThat(TestEnum.SECOND.getDescription()).isEqualTo("SECOND");
  }

}