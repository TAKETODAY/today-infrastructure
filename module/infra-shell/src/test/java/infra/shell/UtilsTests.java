/*
 * Copyright 2015-present the original author or authors.
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

import infra.shell.command.AbstractCommand;
import infra.shell.command.Command;
import infra.shell.command.CommandRegistry;
import infra.shell.utils.Utils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Utils}.
 *
 * @author Eric Bottard
 * @author Mahmoud Ben Hassine
 */
class UtilsTests {

  @Test
  void testSplitCamelCase() {
    assertThat(Utils.splitCamelCase("MyCommands")).isEqualTo("My Commands");
    assertThat(Utils.splitCamelCase("MyAppCommands")).isEqualTo("My App Commands");
  }

  @Test
  public void testUnCamelify() throws Exception {
    assertThat(Utils.unCamelify("HelloWorld")).isEqualTo("hello-world");
    assertThat(Utils.unCamelify("helloWorld")).isEqualTo("hello-world");
    assertThat(Utils.unCamelify("helloWorldHowAreYou")).isEqualTo("hello-world-how-are-you");
    assertThat(Utils.unCamelify("URL")).isEqualTo("url");
  }

  @Test
  void testFormatAvailableCommands() {
    CommandRegistry commandRegistry = new CommandRegistry();
    commandRegistry.registerCommand(Utils.QUIT_COMMAND);
    Command helloCommand = new Command.Builder().name("hello")
            .group("greetings")
            .description("Say hello")
            .aliases("hi", "hey")
            .execute(commandContext -> "hello");
    commandRegistry.registerCommand(helloCommand);
    AbstractCommand secretCommand = new Command.Builder().name("secret")
            .group("greetings")
            .description("A hidden command")
            .hidden(true)
            .execute(commandContext -> "secret");
    commandRegistry.registerCommand(secretCommand);
    String availableCommands = Utils.formatAvailableCommands(commandRegistry);
    String expected = "AVAILABLE COMMANDS" + System.lineSeparator() + System.lineSeparator() + "Built-In Commands"
            + System.lineSeparator() + "\tquit, exit: Exit the shell" + System.lineSeparator() + "greetings"
            + System.lineSeparator() + "\thello, hi, hey: Say hello" + System.lineSeparator();
    assertThat(availableCommands).isEqualTo(expected);
  }

  /**
   * Regression test for
   * <a href= "https://github.com/spring-projects/infra-shell/issues/1377">gh-1377</a>:
   * the quit command must not be forced into the available commands list when it was
   * not registered in the command registry.
   */
  @Test
  void testFormatAvailableCommandsWithoutQuitCommand() {
    CommandRegistry commandRegistry = new CommandRegistry();
    Command helloCommand = new Command.Builder().name("hello")
            .group("greetings")
            .description("Say hello")
            .execute(commandContext -> "hello");
    commandRegistry.registerCommand(helloCommand);
    String availableCommands = Utils.formatAvailableCommands(commandRegistry);
    assertThat(availableCommands).doesNotContain("quit").doesNotContain("Built-In Commands");
  }

}
