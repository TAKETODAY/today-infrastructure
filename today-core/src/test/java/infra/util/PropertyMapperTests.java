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

package infra.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 17:02
 */
class PropertyMapperTests {

  private final PropertyMapper map = PropertyMapper.get();

  @Test
  void fromNullValue() {
    ExampleDest dest = new ExampleDest();
    this.map.from((String) null).to(dest::setName);
    assertThat(dest.getName()).isNull();
  }

  @Test
  void fromValue() {
    ExampleDest dest = new ExampleDest();
    this.map.from("Hello World").to(dest::setName);
    assertThat(dest.getName()).isEqualTo("Hello World");
  }

  @Test
  void fromValueAsIntShouldAdaptValue() {
    Integer result = this.map.from("123").asInt(Long::valueOf).toInstance(Integer::valueOf);
    assertThat(result).isEqualTo(123);
  }

  @Test
  void fromValueAlwaysApplyingWhenNonNullShouldAlwaysApplyNonNullToSource() {
    this.map.alwaysApplyingWhenNonNull().from((String) null).toCall(Assertions::fail);
  }

  @Test
  void fromWhenSupplierIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.map.from((Supplier<?>) null))
            .withMessageContaining("Supplier is required");
  }

  @Test
  void toWhenConsumerIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.map.from(() -> "").to(null))
            .withMessageContaining("Consumer is required");
  }

  @Test
  void toShouldMapFromSupplier() {
    ExampleSource source = new ExampleSource("test");
    ExampleDest dest = new ExampleDest();
    this.map.from(source::getName).to(dest::setName);
    assertThat(dest.getName()).isEqualTo("test");
  }

  @Test
  void asIntShouldAdaptSupplier() {
    Integer result = this.map.from(() -> "123").asInt(Long::valueOf).toInstance(Integer::valueOf);
    assertThat(result).isEqualTo(123);
  }

  @Test
  void asWhenAdapterIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> this.map.from(() -> "").as(null))
            .withMessageContaining("Adapter is required");
  }

  @Test
  void asShouldAdaptSupplier() {
    ExampleDest dest = new ExampleDest();
    this.map.from(() -> 123).as(String::valueOf).to(dest::setName);
    assertThat(dest.getName()).isEqualTo("123");
  }

  @Test
  void whenNonNullWhenSuppliedNullShouldNotMap() {
    this.map.from(() -> null).whenNonNull().as(String::valueOf).toCall(Assertions::fail);
  }

  @Test
  void whenNonNullWhenSuppliedThrowsNullPointerExceptionShouldNotMap() {
    this.map.from(() -> {
      throw new NullPointerException();
    }).whenNonNull().as(String::valueOf).toCall(Assertions::fail);
  }

  @Test
  void whenTrueWhenValueIsTrueShouldMap() {
    Boolean result = this.map.from(true).whenTrue().toInstance(Boolean::valueOf);
    assertThat(result).isTrue();
  }

  @Test
  void whenTrueWhenValueIsFalseShouldNotMap() {
    this.map.from(false).whenTrue().toCall(Assertions::fail);
  }

  @Test
  void whenFalseWhenValueIsFalseShouldMap() {
    Boolean result = this.map.from(false).whenFalse().toInstance(Boolean::valueOf);
    assertThat(result).isFalse();
  }

  @Test
  void whenFalseWhenValueIsTrueShouldNotMap() {
    this.map.from(true).whenFalse().toCall(Assertions::fail);
  }

  @Test
  void whenHasTextWhenValueIsNullShouldNotMap() {
    this.map.from(() -> null).whenHasText().toCall(Assertions::fail);
  }

  @Test
  void whenHasTextWhenValueIsEmptyShouldNotMap() {
    this.map.from("").whenHasText().toCall(Assertions::fail);
  }

  @Test
  void whenHasTextWhenValueHasTextShouldMap() {
    Integer result = this.map.from(123).whenHasText().toInstance(Integer::valueOf);
    assertThat(result).isEqualTo(123);
  }

  @Test
  void whenEqualToWhenValueIsEqualShouldMatch() {
    String result = this.map.from("123").whenEqualTo("123").toInstance(String::new);
    assertThat(result).isEqualTo("123");
  }

  @Test
  void whenEqualToWhenValueIsNotEqualShouldNotMatch() {
    this.map.from("123").whenEqualTo("321").toCall(Assertions::fail);
  }

  @Test
  void whenInstanceOfWhenValueIsTargetTypeShouldMatch() {
    Long result = this.map.from(123L).whenInstanceOf(Long.class).toInstance((value) -> value + 1);
    assertThat(result).isEqualTo(124L);
  }

  @Test
  void whenInstanceOfWhenValueIsNotTargetTypeShouldNotMatch() {
    Supplier<Number> supplier = () -> 123L;
    this.map.from(supplier).whenInstanceOf(Double.class).toCall(Assertions::fail);
  }

  @Test
  void whenWhenValueMatchesShouldMap() {
    String result = this.map.from("123").when("123"::equals).toInstance(String::new);
    assertThat(result).isEqualTo("123");
  }

  @Test
  void whenWhenValueDoesNotMatchShouldNotMap() {
    this.map.from("123").when("321"::equals).toCall(Assertions::fail);
  }

  @Test
  void whenWhenCombinedWithAsUsesSourceValue() {
    Count<String> source = new Count<>(() -> "123");
    Long result = this.map.from(source).when("123"::equals).as(Integer::valueOf).when((v) -> v == 123)
            .as(Integer::longValue).toInstance(Long::valueOf);
    assertThat(result).isEqualTo(123);
    assertThat(source.getCount()).isOne();
  }

  @Test
  void alwaysApplyingWhenNonNullShouldAlwaysApplyNonNullToSource() {
    this.map.alwaysApplyingWhenNonNull().from(() -> null).toCall(Assertions::fail);
  }

  @Test
  void whenWhenValueNotMatchesShouldSupportChainedCalls() {
    this.map.from("123").when("456"::equals).when("123"::equals).toCall(Assertions::fail);
  }

  @Test
  void whenWhenValueMatchesShouldSupportChainedCalls() {
    String result = this.map.from("123").when((s) -> s.contains("2")).when("123"::equals).toInstance(String::new);
    assertThat(result).isEqualTo("123");
  }

  @Test
  void toImmutableReturnsNewInstance() {
    Immutable instance = this.map.from("Spring").toInstance(Immutable::of);
    instance = this.map.from("123").as(Integer::valueOf).to(instance, Immutable::withAge);
    assertThat(instance).hasToString("Spring 123");
  }

  @Test
  void toImmutableWhenFilteredReturnsOriginalInstance() {
    Immutable instance = this.map.from("Spring").toInstance(Immutable::of);
    instance = this.map.from("123").when("345"::equals).as(Integer::valueOf).to(instance, Immutable::withAge);
    assertThat(instance).hasToString("Spring null");
  }

  static class Count<T> implements Supplier<T> {

    private final Supplier<T> source;

    private int count;

    Count(Supplier<T> source) {
      this.source = source;
    }

    @Override
    public T get() {
      this.count++;
      return this.source.get();
    }

    int getCount() {
      return this.count;
    }

  }

  static class ExampleSource {

    private final String name;

    ExampleSource(String name) {
      this.name = name;
    }

    String getName() {
      return this.name;
    }

  }

  static class ExampleDest {

    private String name;

    void setName(String name) {
      this.name = name;
    }

    String getName() {
      return this.name;
    }

  }

  static class Immutable {

    private final String name;

    private final Integer age;

    Immutable(String name, Integer age) {
      this.name = name;
      this.age = age;
    }

    Immutable withAge(Integer age) {
      return new Immutable(this.name, age);
    }

    @Override
    public String toString() {
      return "%s %s".formatted(this.name, this.age);
    }

    static Immutable of(String name) {
      return new Immutable(name, null);
    }

  }

}
