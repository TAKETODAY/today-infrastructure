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

package infra.shell.config;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.shell.NonInteractiveShellRunner;
import infra.shell.command.CommandRegistry;
import infra.shell.command.DefaultCommandParser;
import infra.shell.command.ExitStatus;
import infra.shell.command.annotation.Command;
import infra.shell.command.annotation.Option;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@JUnitConfig({ CommandRegistryAutoConfiguration.class, ShellCommandIntegrationTests.Commands.class })
class ShellCommandIntegrationTests {

  @Autowired
  CommandRegistry registry;

  @Test
  void bindsFileAndPrintsResult() {
    StringWriter output = new StringWriter();
    var runner = new NonInteractiveShellRunner(new DefaultCommandParser(registry), registry, new PrintWriter(output));

    assertThat(runner.execute("data", "import", "--file", "users.csv")).isEqualTo(ExitStatus.OK);
    assertThat(output.toString()).contains("Imported users.csv (1 batch)");
  }

  @Test
  void preservesSpacesInAnArgument() {
    StringWriter output = new StringWriter();
    var runner = new NonInteractiveShellRunner(new DefaultCommandParser(registry), registry, new PrintWriter(output));

    assertThat(runner.execute("data", "import", "--file=first users.csv").code()).isZero();
    assertThat(output.toString()).contains("Imported first users.csv");
  }

  @Test
  void displaysHelpWithoutInvokingTheCommand() {
    assertOutcome(0, "--file", "data", "import", "--help");
  }

  @Test
  void reportsAnUnknownCommand() {
    assertOutcome(-2, "Unknown command: missing", "missing");
  }

  @Test
  void reportsAMissingRequiredOption() {
    assertOutcome(-2, "Required option '--file'", "data", "import");
  }

  @Test
  void reportsAnOptionWithoutAValue() {
    assertOutcome(-2, "requires a value", "data", "import", "--file");
  }

  @Test
  void reportsAnInvalidOptionValue() {
    assertOutcome(-2, "not-a-number", "data", "import", "--file", "users.csv", "--batches", "not-a-number");
  }

  @Test
  void reportsAnExecutionFailure() {
    assertOutcome(-1, "Unable to execute command data import", "data", "import", "--file", "unreadable.csv");
  }

  private void assertOutcome(int code, String message, String... args) {
    StringWriter output = new StringWriter();
    var runner = new NonInteractiveShellRunner(new DefaultCommandParser(registry), registry, new PrintWriter(output));

    assertThat(runner.execute(args).code()).isEqualTo(code);
    assertThat(output.toString()).contains(message).doesNotContain("Imported");
  }

  @Configuration(proxyBeanMethods = false)
  static class Commands {

    @Bean
    ImportCommands importCommands() {
      return new ImportCommands();
    }
  }

  static class ImportCommands {

    @Command(name = "data import", description = "Import a CSV file")
    public String importData(@Option(longName = "file", required = true) Path file,
            @Option(longName = "batches", defaultValue = "1") int batches) {
      if (file.getFileName().toString().equals("unreadable.csv")) {
        throw new IllegalStateException("Unable to read the input file");
      }
      return "Imported " + file.getFileName() + " (" + batches + " batch)";
    }
  }
}
