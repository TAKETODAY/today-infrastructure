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

package cn.taketoday.context.annotation.config;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/9/21 23:47
 */
class AutoConfigurationReplacementsTests {

  private final AutoConfigurationReplacements replacements = AutoConfigurationReplacements
          .load(TestAutoConfigurationReplacements.class, null);

  @Test
  void replaceWhenMatchReplacesClassName() {
    assertThat(this.replacements.replace("com.example.A1")).isEqualTo("com.example.A2");
  }

  @Test
  void replaceWhenNoMatchReturnsOriginalClassName() {
    assertThat(this.replacements.replace("com.example.Z1")).isEqualTo("com.example.Z1");
  }

  @Test
  void replaceAllReplacesAllMatching() {
    Set<String> classNames = new LinkedHashSet<>(
            List.of("com.example.A1", "com.example.B1", "com.example.Y1", "com.example.Z1"));
    assertThat(this.replacements.replaceAll(classNames)).containsExactly("com.example.A2", "com.example.B2",
            "com.example.Y1", "com.example.Z1");
  }

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface TestAutoConfigurationReplacements {

  }

}