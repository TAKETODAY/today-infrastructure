/*
 * Copyright 2026-present the original author or authors.
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

package infra.shell.config;

import org.junit.jupiter.api.Test;

import infra.app.ApplicationArguments;
import infra.app.ApplicationRunner;
import infra.app.InfraApplication;
import infra.context.annotation.config.AutoConfigurations;
import infra.shell.command.annotation.Command;
import infra.stereotype.Component;
import infra.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ShellRunnerAutoConfiguration}.
 *
 * @author David Pilar
 */
class ShellRunnerAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(InfraShellAutoConfiguration.class))
          .withPropertyValues("shell.interactive.enabled=false");

  @Test
  void infraShellApplicationRunnerShouldBeRegistered() {
    this.contextRunner.withUserConfiguration(TestShellApplication.class).run(context -> {
      ApplicationRunner shellRunner = context.getBean("infraShellApplicationRunner", ApplicationRunner.class);
      assertThat(shellRunner).as("infraShellApplicationRunner should be registered").isNotNull();
    });
  }

  @Test
  void infraShellApplicationRunnerShouldBeRegisteredAlongsideUserApplicationRunner() {
    this.contextRunner.withUserConfiguration(TestShellApplicationWithRunner.class).run(context -> {
      ApplicationRunner shellRunner = context.getBean("infraShellApplicationRunner", ApplicationRunner.class);
      assertThat(shellRunner).as("infraShellApplicationRunner should be registered even when another ApplicationRunner exists").isNotNull();

      assertThat(context.containsBean("demoRunner")).as("User-defined ApplicationRunner should also be registered").isTrue();
    });
  }

  @InfraApplication
  static class TestShellApplication {

    @Command
    public void hi() {
      System.out.println("Hello!");
    }

  }

  @InfraApplication
  static class TestShellApplicationWithRunner {

    @Command
    public void hi() {
      System.out.println("Hello!");
    }

    @Component("demoRunner")
    static class DemoRunner implements ApplicationRunner {

      @Override
      public void run(ApplicationArguments args) {
        System.out.println("DemoRunner executed!");
      }

    }

  }

}
