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

import infra.app.InfraApplication;
import infra.context.annotation.Profile;
import infra.context.annotation.config.AutoConfigurations;
import infra.shell.command.CommandRegistry;
import infra.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommandRegistryAutoConfiguration} profile filtering support on
 * {@code @Command} methods.
 *
 * @author David Pilar
 */
class CommandRegistryAutoConfigurationProfileTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(TestShellApplication.class)
          .withConfiguration(AutoConfigurations.of(InfraShellAutoConfiguration.class));

  @Test
  void commandWithoutProfileShouldAlwaysBeRegistered() {
    this.contextRunner.run(context -> {
      CommandRegistry registry = context.getBean(CommandRegistry.class);
      assertThat(hasCommand(registry, "always-available")).as("Command without @Profile should always be registered").isTrue();
    });
  }

  @Test
  void commandWithProfileShouldNotBeRegisteredWhenProfileIsNotActive() {
    this.contextRunner.run(context -> {
      CommandRegistry registry = context.getBean(CommandRegistry.class);
      assertThat(hasCommand(registry, "hello")).as("Command with @Profile('greetings') should not be registered when profile is not active").isFalse();
    });
  }

  @Test
  void commandWithProfileShouldBeRegisteredWhenProfileIsActive() {
    this.contextRunner.withPropertyValues("infra.profiles.active=greetings").run(context -> {
      CommandRegistry registry = context.getBean(CommandRegistry.class);
      assertThat(hasCommand(registry, "hello")).as("Command with @Profile('greetings') should be registered when profile is active").isTrue();
    });
  }

  @Test
  void commandWithNegatedProfileShouldBeRegisteredWhenProfileIsNotActive() {
    this.contextRunner.run(context -> {
      CommandRegistry registry = context.getBean(CommandRegistry.class);
      assertThat(hasCommand(registry, "debug-info")).as("Command with @Profile('!production') should be registered when 'production' is not active").isTrue();
    });
  }

  @Test
  void commandWithNegatedProfileShouldNotBeRegisteredWhenProfileIsActive() {
    this.contextRunner.withPropertyValues("infra.profiles.active=production").run(context -> {
      CommandRegistry registry = context.getBean(CommandRegistry.class);
      assertThat(hasCommand(registry, "debug-info")).as("Command with @Profile('!production') should not be registered when 'production' is active").isFalse();
    });
  }

  private boolean hasCommand(CommandRegistry registry, String commandName) {
    return registry.getCommands().stream().anyMatch(command -> command.getName().equals(commandName));
  }

  @InfraApplication
  static class TestShellApplication {

    @infra.shell.command.annotation.Command(name = "always-available",
            description = "Always available command")
    public void alwaysAvailable() {
    }

    @Profile("greetings")
    @infra.shell.command.annotation.Command(name = "hello", description = "Say hello")
    public void sayHello() {
    }

    @Profile("!production")
    @infra.shell.command.annotation.Command(name = "debug-info",
            description = "Debug information")
    public void debugInfo() {
    }

  }

}
