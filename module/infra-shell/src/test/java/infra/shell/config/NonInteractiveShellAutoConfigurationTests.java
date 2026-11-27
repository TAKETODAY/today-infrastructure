/*
 * Copyright 2025-present the original author or authors.
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

import infra.app.InfraApplication;
import infra.context.annotation.config.AutoConfigurations;
import infra.shell.NonInteractiveShellRunner;
import infra.shell.ShellRunner;
import infra.shell.command.annotation.Command;
import infra.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class NonInteractiveShellAutoConfigurationTests {

  @Test
  void testNonInteractiveShellAutoConfiguration() {
    // given
    ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SpringShellApplication.class)
            .withConfiguration(AutoConfigurations.of(InfraShellAutoConfiguration.class))
            .withPropertyValues("shell.interactive.enabled=false");

    // when
    contextRunner.run(context -> {
      // then
      assertThat(context.getBeansOfType(ShellRunner.class)).hasSize(1);
      ShellRunner shellRunner = context.getBean(ShellRunner.class);
      assertThat(shellRunner).isInstanceOf(NonInteractiveShellRunner.class);
    });

  }

  @InfraApplication
  static class SpringShellApplication {

    @Command
    public void hi() {
      System.out.println("Hello world!");
    }

  }

}
