/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.junit4;

import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.test.annotation.Timed;
import cn.taketoday.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link ApplicationJUnit4ClassRunner}.
 *
 * @author Sam Brannen
 * @author Rick Evans
 * @since 2.5
 */
public class ApplicationJUnit4ClassRunnerTests {

  @Test
  public void checkThatExceptionsAreNotSilentlySwallowed() throws Exception {
    ApplicationJUnit4ClassRunner runner = new ApplicationJUnit4ClassRunner(getClass()) {

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
  public void getSpringTimeoutViaMetaAnnotation() throws Exception {
    ApplicationJUnit4ClassRunner runner = new ApplicationJUnit4ClassRunner(getClass());
    long timeout = runner.getSpringTimeout(new FrameworkMethod(getClass().getDeclaredMethod(
            "springTimeoutWithMetaAnnotation")));
    assertThat(timeout).isEqualTo(10);
  }

  @Test
  public void getSpringTimeoutViaMetaAnnotationWithOverride() throws Exception {
    ApplicationJUnit4ClassRunner runner = new ApplicationJUnit4ClassRunner(getClass());
    long timeout = runner.getSpringTimeout(new FrameworkMethod(getClass().getDeclaredMethod(
            "springTimeoutWithMetaAnnotationAndOverride")));
    assertThat(timeout).isEqualTo(42);
  }

  // -------------------------------------------------------------------------

  @MetaTimed
  void springTimeoutWithMetaAnnotation() {
    /* no-op */
  }

  @MetaTimedWithOverride(millis = 42)
  void springTimeoutWithMetaAnnotationAndOverride() {
    /* no-op */
  }

  @Timed(millis = 10)
  @Retention(RetentionPolicy.RUNTIME)
  private static @interface MetaTimed {
  }

  @Timed(millis = 1000)
  @Retention(RetentionPolicy.RUNTIME)
  private static @interface MetaTimedWithOverride {

    long millis() default 1000;
  }

}
