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

package infra.bytecode;

import org.junit.jupiter.api.Test;

import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/28 17:23
 */
class EnumValueHolderTests {

  @Test
  void testConstructorAndGetters() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "SOURCE");

    assertThat(holder.getName()).isEqualTo("SOURCE");
    assertThat(holder.getDescriptor()).isEqualTo("Ljava/lang/annotation/RetentionPolicy;");
  }

  @Test
  void testToString() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "SOURCE");
    String result = holder.toString();

    assertThat(result).contains("EnumValueHolder")
            .contains("SOURCE")
            .contains("Ljava/lang/annotation/RetentionPolicy;");
  }

  @Test
  void testGetInternalWithRealEnum() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "SOURCE");
    Object value = holder.read();

    assertThat(value)
            .isInstanceOf(RetentionPolicy.class)
            .isEqualTo(RetentionPolicy.SOURCE);
  }

  @Test
  void testCacheMechanism() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "SOURCE");

    Object firstRead = holder.read();
    Object secondRead = holder.read();

    assertThat(firstRead).isSameAs(secondRead);
  }

  @Test
  void testInvalidEnumValue() {
    EnumValueHolder holder = new EnumValueHolder("Ljava/lang/annotation/RetentionPolicy;", "INVALID_VALUE");

    assertThatThrownBy(holder::read)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("INVALID_VALUE");
  }

  @Test
  void testInvalidEnumClass() {
    EnumValueHolder holder = new EnumValueHolder("Linvalid/enum/Class;", "ANY");

    assertThatThrownBy(holder::read)
            .isInstanceOf(RuntimeException.class);
  }
}
