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

package infra.test.stdio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
@ResourceLock("SYSTEM_OUT")
@ResourceLock("SYSTEM_ERR")
class OutputCaptureExtensionTests {

  @BeforeEach
  void beforeEach(CapturedOutput output) {
    assertThat(output.getAll()).isEmpty();
    System.out.print("before:");
  }

  @Test
  void injectsCaptureIntoLifecycleAndTestMethods(CapturedOutput output) {
    System.err.print("error");
    assertThat(output.getOut()).isEqualTo("before:");
    assertThat(output.getErr()).isEqualTo("error");
    assertThat(output.getAll()).isEqualTo("before:error");
  }

  @Test
  void supportsOnlyCapturedOutput(CapturedOutput output) throws Exception {
    OutputCaptureExtension extension = new OutputCaptureExtension();
    ParameterContext parameter = mock(ParameterContext.class);
    ExtensionContext context = mock(ExtensionContext.class);
    given(parameter.getParameter()).willReturn(getClass()
            .getDeclaredMethod("supportsOnlyCapturedOutput", CapturedOutput.class).getParameters()[0]);
    assertThat(extension.supportsParameter(parameter, context)).isTrue();
    given(parameter.getParameter()).willReturn(String.class.getMethod("charAt", int.class).getParameters()[0]);
    assertThat(extension.supportsParameter(parameter, context)).isFalse();
    assertThat(output.getOut()).isEqualTo("before:");
  }

}
