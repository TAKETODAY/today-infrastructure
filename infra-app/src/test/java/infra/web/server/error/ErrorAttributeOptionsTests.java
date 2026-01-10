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

package infra.web.server.error;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static infra.web.server.error.ErrorAttributeOptions.Include;
import static infra.web.server.error.ErrorAttributeOptions.of;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:13
 */
class ErrorAttributeOptionsTests {

  @Test
  void includingFromEmptyAttributesReturnAddedEntry() {
    ErrorAttributeOptions options = of(EnumSet.noneOf(Include.class));
    assertThat(options.including(Include.EXCEPTION).getIncludes()).containsOnly(Include.EXCEPTION);
  }

  @Test
  void includingFromMatchingAttributesDoesNotModifyOptions() {
    ErrorAttributeOptions options = of(EnumSet.of(Include.EXCEPTION, Include.STACK_TRACE));
    assertThat(options.including(Include.EXCEPTION).getIncludes()).containsOnly(Include.EXCEPTION,
            Include.STACK_TRACE);
  }

  @Test
  void excludingFromEmptyAttributesReturnEmptyList() {
    ErrorAttributeOptions options = of(EnumSet.noneOf(Include.class));
    assertThat(options.excluding(Include.EXCEPTION).getIncludes()).isEmpty();
  }

  @Test
  void excludingFromMatchingAttributesRemoveMatch() {
    ErrorAttributeOptions options = of(EnumSet.of(Include.EXCEPTION, Include.STACK_TRACE));
    assertThat(options.excluding(Include.EXCEPTION).getIncludes()).containsOnly(Include.STACK_TRACE);
  }

}
