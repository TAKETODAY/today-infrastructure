/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.context;

import org.assertj.core.api.SoftAssertionsProvider.ThrowingRunnable;
import org.junit.jupiter.api.Test;

import infra.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ClearCachesStartupListener}.
 *
 * @author Phillip Webb
 */
class ClearCachesStartupListenerTests {

  @Test
  void readyWhenNoMethodDoesNothing() {
    TestClassLoader classLoader = new TestClassLoader(Thread.currentThread().getContextClassLoader());
    assertThatNoException().isThrownBy(() -> triggerReady(classLoader, () -> {
    }));
  }

  @Test
  void readyWhenDirectMethodInvokesMethod() throws Exception {
    ClearableClassLoader classLoader = new ClearableClassLoader(Thread.currentThread().getContextClassLoader());
    triggerReady(classLoader, () -> assertThat(classLoader.cleared).isTrue());
  }

  @Test
  void readyWhenInheritedMethodInvokesMethod() throws Exception {
    ExtendedClearableClassLoader classLoader = new ExtendedClearableClassLoader(
            Thread.currentThread().getContextClassLoader());
    triggerReady(classLoader, () -> assertThat(classLoader.cleared).isTrue());
  }

  @Test
  void readyWhenInParentInvokesMethod() throws Exception {
    ClearableClassLoader parentClassLoader = new ClearableClassLoader(
            Thread.currentThread().getContextClassLoader());
    TestClassLoader classLoader = new TestClassLoader(parentClassLoader);
    triggerReady(classLoader, () -> assertThat(parentClassLoader.cleared).isTrue());
  }

  private void triggerReady(ClassLoader classLoader, ThrowingRunnable action) throws Exception {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(classLoader);
      new ClearCachesStartupListener().ready(mock(ConfigurableApplicationContext.class), null);
      action.run();
    }
    finally {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
    }
  }

  public static class TestClassLoader extends ClassLoader {

    TestClassLoader(ClassLoader parent) {
      super(parent);
    }

  }

  public static class ClearableClassLoader extends TestClassLoader {

    public boolean cleared;

    ClearableClassLoader(ClassLoader parent) {
      super(parent);
    }

    public void clearCache() {
      this.cleared = true;
    }

  }

  public static class ExtendedClearableClassLoader extends ClearableClassLoader {

    ExtendedClearableClassLoader(ClassLoader parent) {
      super(parent);
    }

  }

}
