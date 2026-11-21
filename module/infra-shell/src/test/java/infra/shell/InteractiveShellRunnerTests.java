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

package infra.shell;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import infra.shell.command.AbstractCommand;
import infra.shell.command.Command;
import infra.shell.command.CommandContext;
import infra.shell.command.CommandParser;
import infra.shell.command.CommandRegistry;
import infra.shell.command.ExitStatus;
import infra.shell.command.ParsedInput;
import infra.shell.command.availability.Availability;
import infra.shell.command.availability.AvailabilityProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InteractiveShellRunner}.
 *
 * @author Mahmoud Ben Hassine
 */
class InteractiveShellRunnerTests {

  @Test
  void testUnavailableCommandDoesNotAlsoPrintGenericErrorMessage() throws Exception {
    // given
    Command command = new AbstractCommand("test", "A test command") {

      @Override
      public AvailabilityProvider getAvailabilityProvider() {
        return AvailabilityProvider.of(Availability.unavailable("you are not logged in"));
      }

      @Override
      public ExitStatus doExecute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    };
    CommandRegistry commandRegistry = new CommandRegistry(Set.of(command));
    CommandParser commandParser = input -> ParsedInput.builder().commandName(input).build();
    List<String> lines = new ArrayList<>(List.of("test"));
    InputProvider inputProvider = () -> lines.isEmpty() ? null : lines.remove(0);
    StringWriter outputWriter = new StringWriter();
    List<String> printedMessages = new ArrayList<>();
    InteractiveShellRunner shellRunner = new InteractiveShellRunner(inputProvider, commandParser, commandRegistry) {

      @Override
      public void print(String message) {
        printedMessages.add(message);
      }

      @Override
      public void flush() {
      }

      @Override
      public PrintWriter getWriter() {
        return new PrintWriter(outputWriter);
      }

      @Override
      public InputReader getReader() {
        return mock(InputReader.class);
      }
    };

    // when
    shellRunner.run(new String[0]);

    // then
    assertThat(outputWriter.toString().trim()).isEqualTo("Command 'test' exists but is not currently available because you are not logged in");
    assertThat(printedMessages.stream().anyMatch(message -> message.startsWith("Error while executing command"))).isFalse();
  }

}
