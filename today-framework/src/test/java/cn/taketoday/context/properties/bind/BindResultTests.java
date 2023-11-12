/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link BindResult}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class BindResultTests {

  @Mock
  private Consumer<String> consumer;

  @Mock
  private Function<String, String> mapper;

  @Mock
  private Supplier<String> supplier;

  @Test
  void getWhenHasValueShouldReturnValue() {
    BindResult<String> result = BindResult.of("foo");
    assertThat(result.get()).isEqualTo("foo");
  }

  @Test
  void getWhenHasNoValueShouldThrowException() {
    BindResult<String> result = BindResult.of(null);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(result::get)
            .withMessageContaining("No value bound");
  }

  @Test
  void isBoundWhenHasValueShouldReturnTrue() {
    BindResult<String> result = BindResult.of("foo");
    assertThat(result.isBound()).isTrue();
  }

  @Test
  void isBoundWhenHasNoValueShouldFalse() {
    BindResult<String> result = BindResult.of(null);
    assertThat(result.isBound()).isFalse();
  }

  @Test
  void ifBoundWhenConsumerIsNullShouldThrowException() {
    BindResult<String> result = BindResult.of("foo");
    assertThatIllegalArgumentException().isThrownBy(() -> result.ifBound(null))
            .withMessageContaining("Consumer is required");
  }

  @Test
  void ifBoundWhenHasValueShouldCallConsumer() {
    BindResult<String> result = BindResult.of("foo");
    result.ifBound(this.consumer);
    then(this.consumer).should().accept("foo");
  }

  @Test
  void ifBoundWhenHasNoValueShouldNotCallConsumer() {
    BindResult<String> result = BindResult.of(null);
    result.ifBound(this.consumer);
    then(this.consumer).shouldHaveNoInteractions();
  }

  @Test
  void mapWhenMapperIsNullShouldThrowException() {
    BindResult<String> result = BindResult.of("foo");
    assertThatIllegalArgumentException().isThrownBy(() -> result.map(null))
            .withMessageContaining("Mapper is required");
  }

  @Test
  void mapWhenHasValueShouldCallMapper() {
    BindResult<String> result = BindResult.of("foo");
    given(this.mapper.apply("foo")).willReturn("bar");
    assertThat(result.map(this.mapper).get()).isEqualTo("bar");
  }

  @Test
  void mapWhenHasNoValueShouldNotCallMapper() {
    BindResult<String> result = BindResult.of(null);
    result.map(this.mapper);
    then(this.mapper).shouldHaveNoInteractions();
  }

  @Test
  void orElseWhenHasValueShouldReturnValue() {
    BindResult<String> result = BindResult.of("foo");
    assertThat(result.orElse("bar")).isEqualTo("foo");
  }

  @Test
  void orElseWhenHasValueNoShouldReturnOther() {
    BindResult<String> result = BindResult.of(null);
    assertThat(result.orElse("bar")).isEqualTo("bar");
  }

  @Test
  void orElseGetWhenHasValueShouldReturnValue() {
    BindResult<String> result = BindResult.of("foo");
    assertThat(result.orElseGet(this.supplier)).isEqualTo("foo");
    then(this.supplier).shouldHaveNoInteractions();
  }

  @Test
  void orElseGetWhenHasValueNoShouldReturnOther() {
    BindResult<String> result = BindResult.of(null);
    given(this.supplier.get()).willReturn("bar");
    assertThat(result.orElseGet(this.supplier)).isEqualTo("bar");
  }

  @Test
  void orElseThrowWhenHasValueShouldReturnValue() throws Exception {
    BindResult<String> result = BindResult.of("foo");
    assertThat(result.orElseThrow(IOException::new)).isEqualTo("foo");
  }

  @Test
  void orElseThrowWhenHasNoValueShouldThrowException() {
    BindResult<String> result = BindResult.of(null);
    assertThatIOException().isThrownBy(() -> result.orElseThrow(IOException::new));
  }

  @Test
  void hashCodeAndEquals() {
    BindResult<?> result1 = BindResult.of("foo");
    BindResult<?> result2 = BindResult.of("foo");
    BindResult<?> result3 = BindResult.of("bar");
    BindResult<?> result4 = BindResult.of(null);
    assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    assertThat(result1).isEqualTo(result1).isEqualTo(result2).isNotEqualTo(result3).isNotEqualTo(result4);
  }

  @Test
  void ofWhenHasValueShouldReturnBoundResultOfValue() {
    BindResult<Object> result = BindResult.of("foo");
    assertThat(result.isBound()).isTrue();
    assertThat(result.get()).isEqualTo("foo");
  }

  @Test
  void ofWhenValueIsNullShouldReturnUnbound() {
    BindResult<Object> result = BindResult.of(null);
    assertThat(result.isBound()).isFalse();
    assertThat(result).isSameAs(BindResult.of(null));
  }

  static class ExampleBean {

    private final String value;

    ExampleBean() {
      this.value = "new";
    }

    ExampleBean(String value) {
      this.value = value;
    }

    String getValue() {
      return this.value;
    }

  }

}
