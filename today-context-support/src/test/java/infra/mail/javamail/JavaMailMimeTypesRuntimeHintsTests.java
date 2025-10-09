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

package infra.mail.javamail;

import org.junit.jupiter.api.Test;

import infra.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 14:33
 */
class JavaMailMimeTypesRuntimeHintsTests {

  @Test
  void registerHintsWithNullHintsThrowsException() {
    var registrar = new JavaMailMimeTypesRuntimeHints();

    assertThatNullPointerException()
            .isThrownBy(() -> registrar.registerHints(null, null));
  }

  @Test
  void resourcePatternHintsContainsRegisteredMimeTypesPattern() {
    var hints = new RuntimeHints();
    var registrar = new JavaMailMimeTypesRuntimeHints();
    registrar.registerHints(hints, null);

    assertThat(hints.resources().resourcePatternHints())
            .hasSize(1)
            .first()
            .satisfies(pattern -> assertThat(pattern.getIncludes())
                    .anyMatch(hint -> hint.getPattern().equals("infra/mail/javamail/mime.types")));
  }

  @Test
  void resourcePatternHintsWithDifferentClassLoadersYieldsSamePattern() {
    var hints = new RuntimeHints();
    var registrar = new JavaMailMimeTypesRuntimeHints();
    var classLoader1 = getClass().getClassLoader();
    var classLoader2 = ClassLoader.getSystemClassLoader();

    registrar.registerHints(hints, classLoader1);
    registrar.registerHints(hints, classLoader2);

    assertThat(hints.resources().resourcePatternHints())
            .hasSize(2)
            .first()
            .satisfies(pattern -> assertThat(pattern.getIncludes())
                    .anyMatch(hint -> hint.getPattern().equals("infra/mail/javamail/mime.types")));
  }

  @Test
  void registeredPatternHintHasCorrectFilePath() {
    var hints = new RuntimeHints();
    var registrar = new JavaMailMimeTypesRuntimeHints();
    registrar.registerHints(hints, null);

    assertThat(hints.resources().resourcePatternHints())
            .first()
            .satisfies(pattern -> assertThat(pattern.getIncludes())
                    .anyMatch(hint -> hint.getPattern().startsWith("infra/mail/javamail/")
                            && hint.getPattern().endsWith("mime.types")));
  }

}