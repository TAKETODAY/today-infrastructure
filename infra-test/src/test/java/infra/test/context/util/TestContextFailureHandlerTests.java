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

package infra.test.context.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import infra.core.NativeDetector;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

class TestContextFailureHandlerTests {

  private final TestContextFailureHandler handler = new TestContextFailureHandler();

  @Test
  void missingAndIncompatibleDependenciesAreSkipped() {
    for (Throwable failure : new Throwable[] { new ClassNotFoundException("missing"), new NoClassDefFoundError("missing"),
            new UnsupportedClassVersionError("version"), new InvocationTargetException(new NoClassDefFoundError("wrapped")) }) {
      assertThatNoException().isThrownBy(() -> handler.handleFailure(Runnable.class, "example.Factory", failure));
    }
  }

  @Test
  void runtimeExceptionsAndErrorsAreRethrownWithoutWrapping() {
    IllegalArgumentException runtime = new IllegalArgumentException("failure");
    AssertionError error = new AssertionError("failure");
    assertThatThrownBy(() -> handler.handleFailure(Runnable.class, "factory", runtime)).isSameAs(runtime);
    assertThatThrownBy(() -> handler.handleFailure(Runnable.class, "factory", new InvocationTargetException(error)))
            .isSameAs(error);
  }

  @Test
  void checkedExceptionsAreWrappedWithFactoryInformation() {
    Exception failure = new Exception("failure");
    assertThatIllegalStateException().isThrownBy(() -> handler.handleFailure(Runnable.class, "example.Factory", failure))
            .withMessage("Failed to load Runnable [example.Factory]").withCause(failure);
  }

  @Test
  void nativeImageSkipsIllegalStateButStillPropagatesOtherFailures() {
    try (var nativeDetector = mockStatic(NativeDetector.class)) {
      nativeDetector.when(NativeDetector::inNativeImage).thenReturn(true);
      assertThatNoException().isThrownBy(() -> handler.handleFailure(Runnable.class, "factory", new IllegalStateException("native")));
      IllegalArgumentException failure = new IllegalArgumentException("invalid");
      assertThatThrownBy(() -> handler.handleFailure(Runnable.class, "factory", failure)).isSameAs(failure);
    }
  }

}
