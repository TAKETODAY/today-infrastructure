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

package infra.web.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TimeoutUtilsTests {

  @Test
  void acceptsNullTimeout() {
    assertThatCode(() -> TimeoutUtils.validate(null))
            .doesNotThrowAnyException();
  }

  @Test
  void acceptsZeroTimeout() {
    assertThatCode(() -> TimeoutUtils.validate(Duration.ZERO))
            .doesNotThrowAnyException();
  }

  @Test
  void acceptsTimeoutOfAtLeastOneMillisecond() {
    assertThatCode(() -> TimeoutUtils.validate(Duration.ofMillis(1)))
            .doesNotThrowAnyException();
  }

  @Test
  void rejectsNegativeTimeout() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> TimeoutUtils.validate(Duration.ofMillis(-1)))
            .withMessage("Timeout must not be negative");
  }

  @Test
  void rejectsPositiveSubMillisecondTimeout() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> TimeoutUtils.validate(Duration.ofNanos(1)))
            .withMessage("Timeout must be zero or at least 1 millisecond");
  }

  @Test
  void rejectsTimeoutThatCannotBeRepresentedInMilliseconds() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> TimeoutUtils.validate(Duration.ofSeconds(Long.MAX_VALUE)))
            .withMessage("Timeout is too large")
            .withCauseInstanceOf(ArithmeticException.class);
  }

}
