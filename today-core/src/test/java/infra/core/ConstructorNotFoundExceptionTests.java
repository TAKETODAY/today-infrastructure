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

package infra.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/19 22:19
 */
class ConstructorNotFoundExceptionTests {

  @Test
  void constructorWithOnlyType() {
    Class<?> type = String.class;
    ConstructorNotFoundException exception = new ConstructorNotFoundException(type);

    assertThat(exception.getType()).isEqualTo(type);
    assertThat(exception.getParameterTypes()).isNull();
    assertThat(exception.getMessage()).contains("No suitable constructor in class " + type.getName());
    assertThat(exception.getCause()).isNull();
  }

  @Test
  void constructorWithTypeAndMessage() {
    Class<?> type = Integer.class;
    String message = "Custom message for constructor not found";
    ConstructorNotFoundException exception = new ConstructorNotFoundException(type, message);

    assertThat(exception.getType()).isEqualTo(type);
    assertThat(exception.getParameterTypes()).isNull();
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isNull();
  }

  @Test
  void constructorWithTypeParameterTypesAndCause() {
    Class<?> type = Double.class;
    Class<?>[] parameterTypes = { String.class, int.class };
    Throwable cause = new RuntimeException("Root cause");
    ConstructorNotFoundException exception = new ConstructorNotFoundException(type, parameterTypes, cause);

    assertThat(exception.getType()).isEqualTo(type);
    assertThat(exception.getParameterTypes()).isEqualTo(parameterTypes);
    assertThat(exception.getMessage()).contains("No suitable constructor in class " + type.getName());
    assertThat(exception.getCause()).isEqualTo(cause);
  }

  @Test
  void constructorWithAllParameters() {
    Class<?> type = Boolean.class;
    String message = "Complete custom message";
    Class<?>[] parameterTypes = { int.class, String.class, double.class };
    Throwable cause = new Exception("Another cause");
    ConstructorNotFoundException exception = new ConstructorNotFoundException(type, message, parameterTypes, cause);

    assertThat(exception.getType()).isEqualTo(type);
    assertThat(exception.getParameterTypes()).isEqualTo(parameterTypes);
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }

  @Test
  void getTypeReturnsCorrectClass() {
    Class<?> type = Object.class;
    ConstructorNotFoundException exception = new ConstructorNotFoundException(type);

    assertThat(exception.getType()).isEqualTo(type);
  }

  @Test
  void getParameterTypesReturnsNullWhenNotProvided() {
    Class<?> type = Object.class;
    ConstructorNotFoundException exception = new ConstructorNotFoundException(type);

    assertThat(exception.getParameterTypes()).isNull();
  }

  @Test
  void getParameterTypesReturnsProvidedArray() {
    Class<?> type = Object.class;
    Class<?>[] parameterTypes = { String.class, Integer.class };
    ConstructorNotFoundException exception = new ConstructorNotFoundException(type, "msg", parameterTypes, null);

    assertThat(exception.getParameterTypes()).isEqualTo(parameterTypes);
  }

  @Test
  void exceptionExtendsNestedRuntimeException() {
    ConstructorNotFoundException exception = new ConstructorNotFoundException(String.class);

    assertThat(exception).isInstanceOf(NestedRuntimeException.class);
  }

  @Test
  void exceptionHasProperToStringRepresentation() {
    Class<?> type = String.class;
    ConstructorNotFoundException exception = new ConstructorNotFoundException(type);

    assertThat(exception.toString()).contains(type.getName());
  }

}