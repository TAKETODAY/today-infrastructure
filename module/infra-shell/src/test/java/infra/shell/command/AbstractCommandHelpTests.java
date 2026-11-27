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

package infra.shell.command;

import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import infra.shell.InputReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for how {@link AbstractCommand} recognises a request for the implicit help
 * option.
 *
 * @author David Pilar
 */
class AbstractCommandHelpTests {

  private static final String HELP_TEXT = "The help of 'mycommand'";

  @Test
  void testHelpIsPrintedForImplicitLongOption() throws Exception {
    // given
    TestCommand command = new TestCommand();

    // when
    String output = execute(command, option(' ', "help", "true"));

    // then
    assertThat(command.executed).isFalse();
    assertThat(output.contains(HELP_TEXT)).isTrue();
  }

  @Test
  void testHelpIsPrintedForImplicitShortOption() throws Exception {
    // given
    TestCommand command = new TestCommand();

    // when
    String output = execute(command, option('h', "", "true"));

    // then
    assertThat(command.executed).isFalse();
    assertThat(output.contains(HELP_TEXT)).isTrue();
  }

  @Test
  void testHelpIsPrintedWhenGivenAlongsideOtherOptions() throws Exception {
    // given
    TestCommand command = new TestCommand();
    command.getOptions().add(declaredOption(' ', "name", String.class));

    // when
    String output = execute(command, option(' ', "help", "true"), option(' ', "name", "x"));

    // then
    assertThat(command.executed).isFalse();
    assertThat(output.contains(HELP_TEXT)).isTrue();
  }

  @Test
  void testDeclaredShortOptionIsNotTreatedAsHelp() throws Exception {
    // given
    TestCommand command = new TestCommand();
    command.getOptions().add(declaredOption('h', "host", String.class));

    // when
    String output = execute(command, option('h', "", "myhost"));

    // then
    assertThat(command.executed).isTrue();
    assertThat(output).isEqualTo("");
  }

  @Test
  void testDeclaredShortOptionIsNotTreatedAsHelpAlongsideOtherOptions() throws Exception {
    // given
    TestCommand command = new TestCommand();
    command.getOptions().add(declaredOption('h', "host", String.class));
    command.getOptions().add(declaredOption(' ', "port", String.class));

    // when
    String output = execute(command, option('h', "", "myhost"), option(' ', "port", "22"));

    // then
    assertThat(command.executed).isTrue();
    assertThat(output).isEqualTo("");
  }

  @Test
  void testDeclaredLongOptionIsNotTreatedAsHelp() throws Exception {
    // given
    TestCommand command = new TestCommand();
    command.getOptions().add(declaredOption(' ', "help", String.class));

    // when
    String output = execute(command, option(' ', "help", "topic"));

    // then
    assertThat(command.executed).isTrue();
    assertThat(output).isEqualTo("");
  }

  @Test
  void testImplicitLongOptionStillWorksWhenShortOptionIsDeclared() throws Exception {
    // given
    TestCommand command = new TestCommand();
    command.getOptions().add(declaredOption('h', "host", String.class));

    // when
    String output = execute(command, option(' ', "help", "true"));

    // then
    assertThat(command.executed).isFalse();
    assertThat(output.contains(HELP_TEXT)).isTrue();
  }

  @Test
  void testImplicitShortOptionStillWorksWhenLongOptionIsDeclared() throws Exception {
    // given
    TestCommand command = new TestCommand();
    command.getOptions().add(declaredOption(' ', "help", String.class));

    // when
    String output = execute(command, option('h', "", "true"));

    // then
    assertThat(command.executed).isFalse();
    assertThat(output.contains(HELP_TEXT)).isTrue();
  }

  @Test
  void testDifferentlyCasedLongOptionIsNotTreatedAsHelp() throws Exception {
    // given
    TestCommand command = new TestCommand();

    // when
    String output = execute(command, option(' ', "HELP", "true"));

    // then
    assertThat(command.executed).isTrue();
    assertThat(output).isEqualTo("");
  }

  @Test
  void testDeclaredHelpOptionIsNotHijackedByDifferentlyCasedInput() throws Exception {
    // given
    TestCommand command = new TestCommand();
    command.getOptions().add(declaredOption(' ', "help", String.class));

    // when
    String output = execute(command, option(' ', "HELP", "topic"));

    // then
    assertThat(command.executed).isTrue();
    assertThat(output).isEqualTo("");
  }

  private static String execute(Command command, CommandOption... options) throws Exception {
    ParsedInput.Builder parsedInputBuilder = ParsedInput.builder().commandName(command.getName());
    for (CommandOption option : options) {
      parsedInputBuilder.addOption(option);
    }
    StringWriter outputWriter = new StringWriter();
    CommandRegistry commandRegistry = new CommandRegistry();
    commandRegistry.registerCommand(command);
    CommandContext commandContext = new CommandContext(parsedInputBuilder.build(), commandRegistry,
            new PrintWriter(outputWriter), mock(InputReader.class));
    ExitStatus exitStatus = command.execute(commandContext);
    assertThat(exitStatus).isEqualTo(ExitStatus.OK);
    return outputWriter.toString().trim();
  }

  private static CommandOption option(char shortName, String longName, String value) {
    return CommandOption.with().shortName(shortName).longName(longName).value(value).build();
  }

  private static CommandOption declaredOption(char shortName, String longName, Class<?> type) {
    return CommandOption.with().shortName(shortName).longName(longName).type(type).build();
  }

  private static class TestCommand extends AbstractCommand {

    private boolean executed;

    TestCommand() {
      super("mycommand", "My test command", "My test group", HELP_TEXT);
    }

    @Override
    public ExitStatus doExecute(CommandContext commandContext) {
      this.executed = true;
      return ExitStatus.OK;
    }

  }

}
