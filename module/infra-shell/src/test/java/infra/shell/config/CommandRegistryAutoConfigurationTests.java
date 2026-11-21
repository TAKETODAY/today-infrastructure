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

import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;
import infra.context.annotation.config.AutoConfigurations;
import infra.shell.command.CommandRegistry;
import infra.shell.command.annotation.Command;
import infra.stereotype.Component;
import infra.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommandRegistryAutoConfiguration} command discovery.
 *
 * @author David Pilar
 * @author Mahmoud Ben Hassine
 */
class CommandRegistryAutoConfigurationTests {

  @Test
  void requestScopedBeanDoesNotPreventCommandRegistration() {
    new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class, RequestScopedBean.class)
            .withConfiguration(AutoConfigurations.of(InfraShellAutoConfiguration.class))
            .run(context -> {
              assertThat(context.getStartupFailure()).as("Context should start without errors").isNull();
              CommandRegistry registry = context.getBean(CommandRegistry.class);
              assertThat(registry.getCommands().stream().anyMatch(c -> c.getName().equals("greet"))).as("Annotated command should be registered").isTrue();
            });
  }

  @Test
  void quitCommandCanBeDisabledInNonInteractiveMode() {
    new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class)
            .withConfiguration(AutoConfigurations.of(InfraShellAutoConfiguration.class))
            .withPropertyValues("shell.interactive.enabled=false", "shell.command.quit.enabled=false")
            .run(context -> {
              CommandRegistry registry = context.getBean(CommandRegistry.class);
              assertThat(registry.getCommands().stream().anyMatch(c -> c.getName().equals("quit"))).as("Quit command should not be registered when disabled in non-interactive mode").isFalse();
            });
  }

  @Test
  void quitCommandCannotBeDisabledInInteractiveMode() {
    new ApplicationContextRunner().withUserConfiguration(TestConfiguration.class)
            .withConfiguration(AutoConfigurations.of(InfraShellAutoConfiguration.class))
            .withPropertyValues("shell.command.quit.enabled=false")
            .run(context -> {
              CommandRegistry registry = context.getBean(CommandRegistry.class);
              assertThat(registry.getCommands().stream().anyMatch(c -> c.getName().equals("quit"))).as("Quit command should stay registered in interactive mode even when disabled").isTrue();
            });
  }

  @Configuration
  static class TestConfiguration {

    @Command(name = "greet", description = "Say hello")
    public void sayHello() {
    }

  }

  @Component
  @Scope("request")
  static class RequestScopedBean {

  }

}
