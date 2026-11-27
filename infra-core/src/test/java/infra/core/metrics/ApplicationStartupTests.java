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

package infra.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApplicationStartupTests {

  @Test
  void defaultStartupDoesNotRecordStepsOrEvaluateTagSuppliers() {
    StartupStep step = ApplicationStartup.DEFAULT.start("context.refresh");

    assertThat(step.getName()).isEqualTo("default");
    assertThat(step.getId()).isZero();
    assertThat(step.getParentId()).isNull();
    assertThat(step.tag("context", "test")).isSameAs(step);
    assertThat(step.tag("lazy", () -> {
      throw new AssertionError("A no-op startup step must not evaluate tag suppliers");
    })).isSameAs(step);
    step.close();

    StartupStep next = ApplicationStartup.DEFAULT.start("context.ready");
    assertThat(next.getTags()).isEmpty();
    assertThat(next.getId()).isZero();
  }

  @Test
  void closingAStartupStepEndsIt() {
    StartupStep step = mock(StartupStep.class, CALLS_REAL_METHODS);

    step.close();

    verify(step).end();
  }

}
