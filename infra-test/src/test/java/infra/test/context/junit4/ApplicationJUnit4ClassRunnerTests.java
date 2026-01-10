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

package infra.test.context.junit4;

import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.core.annotation.AliasFor;
import infra.test.annotation.Timed;
import infra.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link JUnit4ClassRunner}.
 *
 * @author Sam Brannen
 * @author Rick Evans
 * @since 4.0
 */
public class ApplicationJUnit4ClassRunnerTests {

  @Test
  public void checkThatExceptionsAreNotSilentlySwallowed() throws Exception {
    JUnit4ClassRunner runner = new JUnit4ClassRunner(getClass()) {

      @Override
      protected TestContextManager createTestContextManager(Class<?> clazz) {
        return new TestContextManager(clazz) {

          @Override
          public void prepareTestInstance(Object testInstance) {
            throw new RuntimeException(
                    "This RuntimeException should be caught and wrapped in an Exception.");
          }
        };
      }
    };
    assertThatExceptionOfType(Exception.class).isThrownBy(
            runner::createTest);
  }

  @Test
  public void infraTimeoutViaMetaAnnotation() throws Exception {
    JUnit4ClassRunner runner = new JUnit4ClassRunner(getClass());
    long timeout = runner.getInfraTimeout(new FrameworkMethod(getClass().getDeclaredMethod(
            "infraTimeoutWithMetaAnnotation")));
    assertThat(timeout).isEqualTo(10);
  }

  @Test
  public void infraTimeoutViaMetaAnnotationWithOverride() throws Exception {
    JUnit4ClassRunner runner = new JUnit4ClassRunner(getClass());
    long timeout = runner.getInfraTimeout(new FrameworkMethod(getClass().getDeclaredMethod(
            "infraTimeoutWithMetaAnnotationAndOverride")));
    assertThat(timeout).isEqualTo(42);
  }

  // -------------------------------------------------------------------------

  @MetaTimed
  void infraTimeoutWithMetaAnnotation() {
    /* no-op */
  }

  @MetaTimedWithOverride(millis = 42)
  void infraTimeoutWithMetaAnnotationAndOverride() {
    /* no-op */
  }

  @Timed(millis = 10)
  @Retention(RetentionPolicy.RUNTIME)
  private static @interface MetaTimed {
  }

  @Timed(millis = 1000)
  @Retention(RetentionPolicy.RUNTIME)
  private static @interface MetaTimedWithOverride {

    @AliasFor(annotation = Timed.class)
    long millis() default 1000;
  }

}
