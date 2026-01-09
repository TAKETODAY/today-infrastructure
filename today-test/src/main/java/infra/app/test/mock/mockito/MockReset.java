/*
 * Copyright 2002-present the original author or authors.
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

package infra.app.test.mock.mockito;

import org.mockito.MockSettings;
import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.mockito.listeners.InvocationListener;
import org.mockito.listeners.MethodInvocationReport;
import org.mockito.mock.MockCreationSettings;

import java.util.List;

import infra.lang.Assert;

/**
 * Reset strategy used on a mock bean. Usually applied to a mock via the
 * {@link MockBean @MockBean} annotation but can also be directly applied to any mock in
 * the {@code ApplicationContext} using the static methods.
 *
 * @author Phillip Webb
 * @see ResetMocksTestExecutionListener
 * @since 4.0
 */
public enum MockReset {

  /**
   * Reset the mock before the test method runs.
   */
  BEFORE,

  /**
   * Reset the mock after the test method runs.
   */
  AFTER,

  /**
   * Don't reset the mock.
   */
  NONE;

  /**
   * Create {@link MockSettings settings} to be used with mocks where reset should occur
   * before each test method runs.
   *
   * @return mock settings
   */
  public static MockSettings before() {
    return withSettings(BEFORE);
  }

  /**
   * Create {@link MockSettings settings} to be used with mocks where reset should occur
   * after each test method runs.
   *
   * @return mock settings
   */
  public static MockSettings after() {
    return withSettings(AFTER);
  }

  /**
   * Create {@link MockSettings settings} to be used with mocks where a specific reset
   * should occur.
   *
   * @param reset the reset type
   * @return mock settings
   */
  public static MockSettings withSettings(MockReset reset) {
    return apply(reset, Mockito.withSettings());
  }

  /**
   * Apply {@link MockReset} to existing {@link MockSettings settings}.
   *
   * @param reset the reset type
   * @param settings the settings
   * @return the configured settings
   */
  public static MockSettings apply(MockReset reset, MockSettings settings) {
    Assert.notNull(settings, "Settings is required");
    if (reset != null && reset != NONE) {
      settings.invocationListeners(new ResetInvocationListener(reset));
    }
    return settings;
  }

  /**
   * Get the {@link MockReset} associated with the given mock.
   *
   * @param mock the source mock
   * @return the reset type (never {@code null})
   */
  static MockReset get(Object mock) {
    MockReset reset = MockReset.NONE;
    MockingDetails mockingDetails = Mockito.mockingDetails(mock);
    if (mockingDetails.isMock()) {
      MockCreationSettings<?> settings = mockingDetails.getMockCreationSettings();
      List<InvocationListener> listeners = settings.getInvocationListeners();
      for (Object listener : listeners) {
        if (listener instanceof ResetInvocationListener resetInvocationListener) {
          reset = resetInvocationListener.getReset();
        }
      }
    }
    return reset;
  }

  /**
   * Dummy {@link InvocationListener} used to hold the {@link MockReset} value.
   */
  private static class ResetInvocationListener implements InvocationListener {

    private final MockReset reset;

    ResetInvocationListener(MockReset reset) {
      this.reset = reset;
    }

    MockReset getReset() {
      return this.reset;
    }

    @Override
    public void reportInvocation(MethodInvocationReport methodInvocationReport) {
    }

  }

}
