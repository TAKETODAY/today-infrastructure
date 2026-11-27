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

package infra.shell.command;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import infra.shell.InputReader;
import infra.shell.command.availability.Availability;
import infra.shell.command.availability.AvailabilityProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for command availability.
 *
 * @author Mahmoud Ben Hassine
 */
public class CommandAvailabilityTests {

  @Test
  public void testCommandAvailability() throws Exception {
    // given
    Command command = new AbstractCommand("test", "A test command") {

      @Override
      public AvailabilityProvider getAvailabilityProvider() {
        return AvailabilityProvider.of(new Availability("you are not allowed to run this command"));
      }

      @Override
      public ExitStatus doExecute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    };
    StringWriter stringWriter = new StringWriter();
    CommandContext commandContext = new CommandContext(mock(ParsedInput.class), mock(CommandRegistry.class),
            new PrintWriter(stringWriter), mock(InputReader.class));

    // when
    ExitStatus exitStatus = command.execute(commandContext);

    // then
    assertThat(exitStatus).isEqualTo(ExitStatus.AVAILABILITY_ERROR);
    assertThat(stringWriter.toString().trim()).isEqualTo("Command 'test' exists but is not currently available because you are not allowed to run this command");
  }

}
