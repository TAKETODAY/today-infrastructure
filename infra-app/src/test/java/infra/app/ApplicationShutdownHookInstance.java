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

package infra.app;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import infra.context.ConfigurableApplicationContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 17:25
 */
public class ApplicationShutdownHookInstance implements AssertProvider<ApplicationShutdownHookInstance.Assert> {

  private final ApplicationShutdownHook shutdownHook;

  private ApplicationShutdownHookInstance(ApplicationShutdownHook shutdownHook) {
    this.shutdownHook = shutdownHook;
  }

  ApplicationShutdownHook getShutdownHook() {
    return this.shutdownHook;
  }

  @Override
  public Assert assertThat() {
    return new Assert(this.shutdownHook);
  }

  public static void reset() {
    get().getShutdownHook().reset();
  }

  public static ApplicationShutdownHookInstance get() {
    return new ApplicationShutdownHookInstance(Application.shutdownHook);
  }

  /**
   * Assertions that can be performed on the {@link ApplicationShutdownHook}.
   */
  public static class Assert extends ObjectAssert<ApplicationShutdownHook> {

    Assert(ApplicationShutdownHook actual) {
      super(actual);
    }

    public Assert registeredApplicationContext(ConfigurableApplicationContext context) {
      assertThatIsApplicationContextRegistered(context).isTrue();
      return this;
    }

    public Assert didNotRegisterApplicationContext(ConfigurableApplicationContext context) {
      assertThatIsApplicationContextRegistered(context).isFalse();
      return this;
    }

    private AbstractBooleanAssert<?> assertThatIsApplicationContextRegistered(
            ConfigurableApplicationContext context) {
      return Assertions.assertThat(this.actual.isApplicationContextRegistered(context))
              .as("ApplicationContext registered with shutdown hook");
    }

  }

}
