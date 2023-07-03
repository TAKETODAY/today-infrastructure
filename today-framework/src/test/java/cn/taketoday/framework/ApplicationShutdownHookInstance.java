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

package cn.taketoday.framework;

import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AssertProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import cn.taketoday.context.ConfigurableApplicationContext;

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
