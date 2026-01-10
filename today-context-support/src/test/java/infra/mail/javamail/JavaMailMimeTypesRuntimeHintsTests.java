/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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