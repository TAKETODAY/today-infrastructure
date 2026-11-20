/*
 * Copyright 2021-present the original author or authors.
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

import infra.app.ApplicationRunner;
import infra.app.ExitCodeGenerator;
import infra.context.annotation.Bean;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnMissingClass;
import infra.context.condition.ConditionalOnProperty;
import infra.core.env.Environment;
import infra.shell.ConsoleInputProvider;
import infra.shell.NonInteractiveShellRunner;
import infra.shell.ShellRunner;
import infra.shell.SystemShellRunner;
import infra.shell.command.CommandExecutionException;
import infra.shell.command.CommandParser;
import infra.shell.command.CommandRegistry;
import infra.shell.command.DefaultCommandParser;

@AutoConfiguration
public class ShellRunnerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "infraShellApplicationRunner")
  public ApplicationRunner infraShellApplicationRunner(ShellRunner shellRunner) {
    return args -> {
      try {
        shellRunner.run(args.getSourceArgs());
      }
      catch (CommandExecutionException ex) {
        throw new ShellApplicationException(ex);
      }
    };
  }

  @Bean
  @ConditionalOnMissingClass("infra.shell.jline.config.JLineShellAutoConfiguration")
  @ConditionalOnProperty(prefix = "shell.interactive", name = "enabled", havingValue = "true",
          matchIfMissing = true)
  public ShellRunner systemShellRunner(ConsoleInputProvider consoleInputProvider, CommandParser commandParser,
          CommandRegistry commandRegistry, Environment environment) {
    SystemShellRunner systemShellRunner = new SystemShellRunner(consoleInputProvider, commandParser,
            commandRegistry);
    Boolean debugMode = environment.getProperty("shell.debug.enabled", Boolean.class, false);
    systemShellRunner.setDebugMode(debugMode);
    return systemShellRunner;
  }

  @Bean
  @ConditionalOnMissingClass("infra.shell.jline.config.JLineShellAutoConfiguration")
  @ConditionalOnProperty(prefix = "shell.interactive", name = "enabled", havingValue = "false")
  public ShellRunner nonInteractiveShellRunner(CommandParser commandParser, CommandRegistry commandRegistry) {
    return new NonInteractiveShellRunner(commandParser, commandRegistry);
  }

  @Bean
  @ConditionalOnMissingBean
  public ConsoleInputProvider consoleInputProvider() {
    return new ConsoleInputProvider();
  }

  @Bean
  @ConditionalOnMissingBean
  public CommandParser commandParser(CommandRegistry commandRegistry) {
    return new DefaultCommandParser(commandRegistry);
  }

  private static final class ShellApplicationException extends RuntimeException implements ExitCodeGenerator {

    private final int exitCode;

    ShellApplicationException(CommandExecutionException cause) {
      super(cause.getMessage(), cause);
      this.exitCode = cause.getExitCode();
    }

    @Override
    public int getExitCode() {
      return this.exitCode;
    }
  }

}
