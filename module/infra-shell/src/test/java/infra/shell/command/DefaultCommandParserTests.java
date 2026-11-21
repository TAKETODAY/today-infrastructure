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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DefaultCommandParserTests {

  private CommandParser parser;

  private CommandRegistry commandRegistry;

  @BeforeEach
  void setUp() {
    this.commandRegistry = new CommandRegistry();
    this.parser = new DefaultCommandParser(this.commandRegistry);
  }

  @Test
  void testParseLongOptionWithoutValue() {
    assertThatIllegalArgumentException().isThrownBy(() -> parser.parse("mycommand --optionA -b=value2 arg1"));
  }

  @Test
  void testParseShortOptionWithoutValue() {
    assertThatIllegalArgumentException().isThrownBy(() -> parser.parse("mycommand -a --optionB=value2 arg1"));
  }

  @Test
  void testParseCommandWithoutSubCommand() {
    ParsedInput parsedInput = parser
            .parse("mycommand --optionA=value1 arg1 -b=value2 arg2 --optionC value3 -d value4");
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.options().size()).isEqualTo(4);
    assertThat(parsedInput.arguments().size()).isEqualTo(2);

    CommandOption optionA = parsedInput.options().get(0);
    assertThat(optionA.shortName()).isEqualTo(' ');
    assertThat(optionA.longName()).isEqualTo("optionA");
    assertThat(optionA.value()).isEqualTo("value1");

    CommandOption optionB = parsedInput.options().get(1);
    assertThat(optionB.shortName()).isEqualTo('b');
    assertThat(optionB.longName()).isEqualTo("");
    assertThat(optionB.value()).isEqualTo("value2");

    CommandOption optionC = parsedInput.options().get(2);
    assertThat(optionC.shortName()).isEqualTo(' ');
    assertThat(optionC.longName()).isEqualTo("optionC");
    assertThat(optionC.value()).isEqualTo("value3");

    CommandOption optionD = parsedInput.options().get(3);
    assertThat(optionD.shortName()).isEqualTo('d');
    assertThat(optionD.longName()).isEqualTo("");
    assertThat(optionD.value()).isEqualTo("value4");

    CommandArgument argument1 = parsedInput.arguments().get(0);
    assertThat(argument1.index()).isEqualTo(0);
    assertThat(argument1.value()).isEqualTo("arg1");

    CommandArgument argument2 = parsedInput.arguments().get(1);
    assertThat(argument2.index()).isEqualTo(1);
    assertThat(argument2.value()).isEqualTo("arg2");
  }

  @Test
  void testParseCommandWithSubCommand() {
    commandRegistry.registerCommand(createCommand("mycommand mysubcommand", "My test command"));
    ParsedInput parsedInput = parser.parse("mycommand mysubcommand --optionA=value1 arg1 -b=value2 arg2");
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.subCommands().size()).isEqualTo(1);
    assertThat(parsedInput.subCommands().get(0)).isEqualTo("mysubcommand");
    assertThat(parsedInput.options().size()).isEqualTo(2);
    assertThat(parsedInput.arguments().size()).isEqualTo(2);

    CommandOption optionA = parsedInput.options().get(0);
    assertThat(optionA.shortName()).isEqualTo(' ');
    assertThat(optionA.longName()).isEqualTo("optionA");
    assertThat(optionA.value()).isEqualTo("value1");

    CommandOption optionB = parsedInput.options().get(1);
    assertThat(optionB.shortName()).isEqualTo('b');
    assertThat(optionB.longName()).isEqualTo("");
    assertThat(optionB.value()).isEqualTo("value2");

    CommandArgument argument1 = parsedInput.arguments().get(0);
    assertThat(argument1.index()).isEqualTo(0);
    assertThat(argument1.value()).isEqualTo("arg1");

    CommandArgument argument2 = parsedInput.arguments().get(1);
    assertThat(argument2.index()).isEqualTo(1);
    assertThat(argument2.value()).isEqualTo("arg2");
  }

  @Test
  void testParseWithMultipleSubCommands() {
    commandRegistry.registerCommand(createCommand("mycommand mysubcommand1 mysubcommand2", "My test command"));
    ParsedInput parsedInput = parser
            .parse("mycommand mysubcommand1 mysubcommand2 --optionA=value1 arg1 -b=value2 arg2");
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.subCommands().size()).isEqualTo(2);
    assertThat(parsedInput.subCommands().get(0)).isEqualTo("mysubcommand1");
    assertThat(parsedInput.subCommands().get(1)).isEqualTo("mysubcommand2");
    assertThat(parsedInput.options().size()).isEqualTo(2);
    assertThat(parsedInput.arguments().size()).isEqualTo(2);

    CommandOption optionA = parsedInput.options().get(0);
    assertThat(optionA.shortName()).isEqualTo(' ');
    assertThat(optionA.longName()).isEqualTo("optionA");
    assertThat(optionA.value()).isEqualTo("value1");

    CommandOption optionB = parsedInput.options().get(1);
    assertThat(optionB.shortName()).isEqualTo('b');
    assertThat(optionB.longName()).isEqualTo("");
    assertThat(optionB.value()).isEqualTo("value2");

    CommandArgument argument1 = parsedInput.arguments().get(0);
    assertThat(argument1.index()).isEqualTo(0);
    assertThat(argument1.value()).isEqualTo("arg1");

    CommandArgument argument2 = parsedInput.arguments().get(1);
    assertThat(argument2.index()).isEqualTo(1);
    assertThat(argument2.value()).isEqualTo("arg2");
  }

  @Test
  void testParseWithSubCommandWithoutOptions() {
    commandRegistry.registerCommand(createCommand("mycommand mysubcommand", "My test command"));
    ParsedInput parsedInput = parser.parse("mycommand mysubcommand -- arg1 arg2");
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.subCommands().size()).isEqualTo(1);
    assertThat(parsedInput.subCommands().get(0)).isEqualTo("mysubcommand");
    assertThat(parsedInput.options().size()).isEqualTo(0);
    assertThat(parsedInput.arguments().size()).isEqualTo(2);

    CommandArgument argument1 = parsedInput.arguments().get(0);
    assertThat(argument1.index()).isEqualTo(0);
    assertThat(argument1.value()).isEqualTo("arg1");

    CommandArgument argument2 = parsedInput.arguments().get(1);
    assertThat(argument2.index()).isEqualTo(1);
    assertThat(argument2.value()).isEqualTo("arg2");
  }

  // In this case, subcommands should still be parsed, and the rest considered as
  // arguments
  @Test
  void testParseWithSubCommandWithoutOptionsAndWithoutSeparator() {
    commandRegistry.registerCommand(createCommand("mycommand mysubcommand", "My test command"));
    ParsedInput parsedInput = parser.parse("mycommand mysubcommand arg1 arg2");
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.subCommands().size()).isEqualTo(1);
    assertThat(parsedInput.subCommands().get(0)).isEqualTo("mysubcommand");
    assertThat(parsedInput.arguments().get(0).value()).isEqualTo("arg1");
    assertThat(parsedInput.arguments().get(1).value()).isEqualTo("arg2");
  }

  @ParameterizedTest
  @MethodSource("parseWithQuotedOptionData")
  void testParseWithQuotedOption(String input, String longName, char shortName, String expectedValue) {
    // when
    ParsedInput parsedInput = parser.parse(input);

    // then
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.options().size()).isEqualTo(1);
    assertThat(parsedInput.options().get(0).longName()).isEqualTo(longName);
    assertThat(parsedInput.options().get(0).shortName()).isEqualTo(shortName);
    assertThat(parsedInput.options().get(0).value()).isEqualTo(expectedValue);
  }

  static Stream<Arguments> parseWithQuotedOptionData() {
    return Stream.of(Arguments.of("mycommand --option=value", "option", ' ', "value"),
            Arguments.of("mycommand --option=\\\"value\\\"", "option", ' ', "\\\"value\\\""),
            Arguments.of("mycommand --option=\"value\"", "option", ' ', "value"),
            Arguments.of("mycommand --option=\"value1 value2\"", "option", ' ', "value1 value2"),
            Arguments.of("mycommand --option=\"value1=value2\"", "option", ' ', "value1=value2"),
            Arguments.of("mycommand --option=value1\"inside\"value2", "option", ' ', "value1\"inside\"value2"),
            Arguments.of("mycommand --option=\"value1 \\\"inside\\\" value2\"", "option", ' ',
                    "value1 \"inside\" value2"),
            Arguments.of("mycommand --option=value1'inside'value2", "option", ' ', "value1'inside'value2"),
            Arguments.of("mycommand --option=\"value1 'inside' value2\"", "option", ' ', "value1 'inside' value2"),
            Arguments.of("mycommand   --option=value", "option", ' ', "value"),
            Arguments.of("mycommand   --option=\"value\"", "option", ' ', "value"),

            Arguments.of("mycommand --option value", "option", ' ', "value"),
            Arguments.of("mycommand --option \\\"value\\\"", "option", ' ', "\\\"value\\\""),
            Arguments.of("mycommand --option \"value\"", "option", ' ', "value"),
            Arguments.of("mycommand --option \"value1 value2\"", "option", ' ', "value1 value2"),
            Arguments.of("mycommand --option \"value1=value2\"", "option", ' ', "value1=value2"),
            Arguments.of("mycommand --option value1\"inside\"value2", "option", ' ', "value1\"inside\"value2"),
            Arguments.of("mycommand --option \"value1 \\\"inside\\\" value2\"", "option", ' ',
                    "value1 \"inside\" value2"),
            Arguments.of("mycommand --option value1'inside'value2", "option", ' ', "value1'inside'value2"),
            Arguments.of("mycommand --option \"value1 'inside' value2\"", "option", ' ', "value1 'inside' value2"),
            Arguments.of("mycommand   --option   value", "option", ' ', "value"),
            Arguments.of("mycommand   --option   \"value\"", "option", ' ', "value"),

            Arguments.of("mycommand -o=value", "", 'o', "value"),
            Arguments.of("mycommand -o=\\\"value\\\"", "", 'o', "\\\"value\\\""),
            Arguments.of("mycommand -o=\"value\"", "", 'o', "value"),
            Arguments.of("mycommand -o=\"value1 value2\"", "", 'o', "value1 value2"),
            Arguments.of("mycommand -o=\"value1=value2\"", "", 'o', "value1=value2"),
            Arguments.of("mycommand -o=value1\"inside\"value2", "", 'o', "value1\"inside\"value2"),
            Arguments.of("mycommand -o=\"value1 \\\"inside\\\" value2\"", "", 'o', "value1 \"inside\" value2"),
            Arguments.of("mycommand -o=value1'inside'value2", "", 'o', "value1'inside'value2"),
            Arguments.of("mycommand -o=\"value1 'inside' value2\"", "", 'o', "value1 'inside' value2"),
            Arguments.of("mycommand   -o=value", "", 'o', "value"),
            Arguments.of("mycommand   -o=\"value\"", "", 'o', "value"),

            Arguments.of("mycommand -o value", "", 'o', "value"),
            Arguments.of("mycommand -o \\\"value\\\"", "", 'o', "\\\"value\\\""),
            Arguments.of("mycommand -o \"value\"", "", 'o', "value"),
            Arguments.of("mycommand -o \"value1 value2\"", "", 'o', "value1 value2"),
            Arguments.of("mycommand -o \"value1=value2\"", "", 'o', "value1=value2"),
            Arguments.of("mycommand -o value1\"inside\"value2", "", 'o', "value1\"inside\"value2"),
            Arguments.of("mycommand -o \"value1 \\\"inside\\\" value2\"", "", 'o', "value1 \"inside\" value2"),
            Arguments.of("mycommand -o value1'inside'value2", "", 'o', "value1'inside'value2"),
            Arguments.of("mycommand -o \"value1 'inside' value2\"", "", 'o', "value1 'inside' value2"),
            Arguments.of("mycommand   -o   \"value\"", "", 'o', "value"));
  }

  @ParameterizedTest
  @MethodSource("parseWithQuotedArgumentData")
  void testParseWithQuotedArgument(String input, String expectedValue) {
    // when
    ParsedInput parsedInput = parser.parse(input);

    // then
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.arguments().get(0).value()).isEqualTo(expectedValue);
    assertThat(parsedInput.arguments().size()).isEqualTo(1);
  }

  static Stream<Arguments> parseWithQuotedArgumentData() {
    return Stream.of(Arguments.of("mycommand -- value", "value"),
            Arguments.of("mycommand -- \\\"value\\\"", "\\\"value\\\""),
            Arguments.of("mycommand -- \"value\"", "value"),
            Arguments.of("mycommand -- \"value1 value2\"", "value1 value2"),
            Arguments.of("mycommand -- \"value1=value2\"", "value1=value2"),
            Arguments.of("mycommand -- value1\"inside\"value2", "value1\"inside\"value2"),
            Arguments.of("mycommand -- \"value1 \\\"inside\\\" value2\"", "value1 \"inside\" value2"),
            Arguments.of("mycommand -- value1'inside'value2", "value1'inside'value2"),
            Arguments.of("mycommand -- \"value1 'inside' value2\"", "value1 'inside' value2"),
            Arguments.of("mycommand  --  value", "value"), Arguments.of("mycommand  --  \"value\"", "value"));
  }

  @ParameterizedTest
  @MethodSource("parseWithEscapedQuotedArgumentData")
  void testParseWithEscapedQuotedArgument(String input, String expectedValue) {
    // when
    ParsedInput parsedInput = parser.parse(input);

    // then
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.arguments().size()).isEqualTo(1);
    assertThat(parsedInput.arguments().get(0).value()).isEqualTo(expectedValue);
  }

  static Stream<Arguments> parseWithEscapedQuotedArgumentData() {
    return Stream.of(
            // escaped quote inside a double quoted argument (odd number of escaped
            // quotes)
            Arguments.of("mycommand \"she said \\\" and left\"", "she said \" and left"),
            Arguments.of("mycommand \"a \\\" b \\\" c \\\" d\"", "a \" b \" c \" d"),
            Arguments.of("mycommand \"it's here\"", "it's here"),
            // single quoted arguments group words
            Arguments.of("mycommand 'value1 value2'", "value1 value2"),
            Arguments.of("mycommand 'she said \" and left'", "she said \" and left"),
            Arguments.of("mycommand 'don\\'t stop'", "don't stop"),
            // escaped backslash inside a quoted argument
            Arguments.of("mycommand \"a\\\\b\"", "a\\b"),
            // empty quoted argument
            Arguments.of("mycommand \"\"", ""), Arguments.of("mycommand ''", ""),
            // quoted words are not treated as options
            Arguments.of("mycommand \"--option=value\"", "--option=value"),
            // trailing backslash is kept as-is
            Arguments.of("mycommand arg\\", "arg\\"),
            // any whitespace separates words
            Arguments.of("mycommand\t\"value1 value2\"", "value1 value2"));
  }

  @Test
  void testParseWithMultipleQuotedArguments() {
    // when
    ParsedInput parsedInput = parser.parse("mycommand \"value1 value2\" 'value3 value4'");

    // then
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.arguments().size()).isEqualTo(2);
    assertThat(parsedInput.arguments().get(0).value()).isEqualTo("value1 value2");
    assertThat(parsedInput.arguments().get(1).value()).isEqualTo("value3 value4");
  }

  @Test
  void testParseWithQuotedOptionValueFollowedByArgument() {
    // when
    ParsedInput parsedInput = parser.parse("mycommand --option \"value1 value2\" arg1");

    // then
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.options().size()).isEqualTo(1);
    assertThat(parsedInput.options().get(0).value()).isEqualTo("value1 value2");
    assertThat(parsedInput.arguments().size()).isEqualTo(1);
    assertThat(parsedInput.arguments().get(0).value()).isEqualTo("arg1");
  }

  @ParameterizedTest
  @ValueSource(strings = { "", "   " })
  void testParseEmptyInput(String input) {
    // when
    ParsedInput parsedInput = parser.parse(input);

    // then
    assertThat(parsedInput.commandName()).isEqualTo("");
    assertThat(parsedInput.options().size()).isEqualTo(0);
    assertThat(parsedInput.arguments().size()).isEqualTo(0);
  }

  @ParameterizedTest
  @MethodSource("parseWithEscapedQuotedOptionData")
  void testParseWithEscapedQuotedOption(String input, String expectedValue) {
    // when
    ParsedInput parsedInput = parser.parse(input);

    // then
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.options().size()).isEqualTo(1);
    assertThat(parsedInput.options().get(0).longName()).isEqualTo("option");
    assertThat(parsedInput.options().get(0).value()).isEqualTo(expectedValue);
  }

  static Stream<Arguments> parseWithEscapedQuotedOptionData() {
    return Stream.of(Arguments.of("mycommand --option=\"she said \\\" and left\"", "she said \" and left"),
            Arguments.of("mycommand --option \"she said \\\" and left\"", "she said \" and left"),
            Arguments.of("mycommand --option='value1 value2'", "value1 value2"),
            Arguments.of("mycommand --option 'value1 value2'", "value1 value2"),
            Arguments.of("mycommand --option=\"\"", ""), Arguments.of("mycommand --option=''", ""));
  }

  @ParameterizedTest
  @ValueSource(strings = { "mycommand \"unbalanced value", "mycommand 'unbalanced value",
          "mycommand --option=\"unbalanced value", "mycommand don't" })
  void testParseWithUnbalancedQuotes(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> parser.parse(input)).withMessageContaining("quote");
  }

  @ParameterizedTest
  @MethodSource("parseWithBooleanOptionData")
  void testParseWithBooleanOption(String input, String commandName, String longName, char shortName, Class<?> type,
          String expectedValue) {
    // given
    Command command = createCommand(commandName, "My test command");
    command.getOptions().add(CommandOption.with().longName(longName).shortName(shortName).type(type).build());
    commandRegistry.registerCommand(command);

    // when
    ParsedInput parsedInput = parser.parse(input);
    String commandWithSubcommands = String
            .join(" ", parsedInput.commandName(), String.join(" ", parsedInput.subCommands()))
            .trim();

    // then
    assertThat(commandWithSubcommands).isEqualTo(commandName);
    assertThat(parsedInput.options().size()).isEqualTo(1);
    assertThat(parsedInput.options().get(0).longName()).isEqualTo(longName);
    assertThat(parsedInput.options().get(0).shortName()).isEqualTo(shortName);
    assertThat(parsedInput.options().get(0).value()).isEqualTo(expectedValue);
  }

  static Stream<Arguments> parseWithBooleanOptionData() {
    return Stream.of(Arguments.of("mycommand --option=true", "mycommand", "option", ' ', boolean.class, "true"),
            Arguments.of("mycommand --option=false", "mycommand", "option", ' ', boolean.class, "false"),
            Arguments.of("mycommand --option true", "mycommand", "option", ' ', boolean.class, "true"),
            Arguments.of("mycommand --option false", "mycommand", "option", ' ', boolean.class, "false"),
            Arguments.of("mycommand --option", "mycommand", "option", ' ', boolean.class, "true"),

            Arguments.of("mycommand -on=true", "mycommand", "", 'o', boolean.class, "true"),
            Arguments.of("mycommand -o=false", "mycommand", "", 'o', boolean.class, "false"),
            Arguments.of("mycommand -o true", "mycommand", "", 'o', boolean.class, "true"),
            Arguments.of("mycommand -o false", "mycommand", "", 'o', boolean.class, "false"),
            Arguments.of("mycommand -o", "mycommand", "", 'o', boolean.class, "true"),

            Arguments.of("mycommand --option=true", "mycommand", "option", ' ', Boolean.class, "true"),
            Arguments.of("mycommand --option=false", "mycommand", "option", ' ', Boolean.class, "false"),
            Arguments.of("mycommand --option true", "mycommand", "option", ' ', Boolean.class, "true"),
            Arguments.of("mycommand --option false", "mycommand", "option", ' ', Boolean.class, "false"),
            Arguments.of("mycommand --option", "mycommand", "option", ' ', Boolean.class, "true"),

            Arguments.of("mycommand -on=true", "mycommand", "", 'o', Boolean.class, "true"),
            Arguments.of("mycommand -o=false", "mycommand", "", 'o', Boolean.class, "false"),
            Arguments.of("mycommand -o true", "mycommand", "", 'o', Boolean.class, "true"),
            Arguments.of("mycommand -o false", "mycommand", "", 'o', Boolean.class, "false"),
            Arguments.of("mycommand -o", "mycommand", "", 'o', Boolean.class, "true"),

            Arguments.of("thecommand mysubcommand --option=true", "thecommand mysubcommand", "option", ' ',
                    boolean.class, "true"),
            Arguments.of("thecommand mysubcommand --option=false", "thecommand mysubcommand", "option", ' ',
                    boolean.class, "false"),
            Arguments.of("thecommand mysubcommand --option true", "thecommand mysubcommand", "option", ' ',
                    boolean.class, "true"),
            Arguments.of("thecommand mysubcommand --option false", "thecommand mysubcommand", "option", ' ',
                    boolean.class, "false"),
            Arguments.of("thecommand mysubcommand --option", "thecommand mysubcommand", "option", ' ',
                    boolean.class, "true"),

            Arguments.of("thecommand mysubcommand -on=true", "thecommand mysubcommand", "", 'o', boolean.class,
                    "true"),
            Arguments.of("thecommand mysubcommand -o=false", "thecommand mysubcommand", "", 'o', boolean.class,
                    "false"),
            Arguments.of("thecommand mysubcommand -o true", "thecommand mysubcommand", "", 'o', boolean.class,
                    "true"),
            Arguments.of("thecommand mysubcommand -o false", "thecommand mysubcommand", "", 'o', boolean.class,
                    "false"),
            Arguments.of("thecommand mysubcommand -o", "thecommand mysubcommand", "", 'o', boolean.class, "true"),

            Arguments.of("thecommand mysubcommand --option=true", "thecommand mysubcommand", "option", ' ',
                    Boolean.class, "true"),
            Arguments.of("thecommand mysubcommand --option=false", "thecommand mysubcommand", "option", ' ',
                    Boolean.class, "false"),
            Arguments.of("thecommand mysubcommand --option true", "thecommand mysubcommand", "option", ' ',
                    Boolean.class, "true"),
            Arguments.of("thecommand mysubcommand --option false", "thecommand mysubcommand", "option", ' ',
                    Boolean.class, "false"),
            Arguments.of("thecommand mysubcommand --option", "thecommand mysubcommand", "option", ' ',
                    Boolean.class, "true"),

            Arguments.of("thecommand mysubcommand -on=true", "thecommand mysubcommand", "", 'o', Boolean.class,
                    "true"),
            Arguments.of("thecommand mysubcommand -o=false", "thecommand mysubcommand", "", 'o', Boolean.class,
                    "false"),
            Arguments.of("thecommand mysubcommand -o true", "thecommand mysubcommand", "", 'o', Boolean.class,
                    "true"),
            Arguments.of("thecommand mysubcommand -o false", "thecommand mysubcommand", "", 'o', Boolean.class,
                    "false"),
            Arguments.of("thecommand mysubcommand -o", "thecommand mysubcommand", "", 'o', Boolean.class, "true"));
  }

  @ParameterizedTest
  @MethodSource("parseWithBooleanOptionAndArgumentData")
  void testParseWithBooleanOptionAndArgument(String input, String commandName, String expectedValue,
          int expectedArguments, String expectedArgumentValue) {
    // given
    Command command = createCommand(commandName, "My test command");
    command.getOptions().add(CommandOption.with().longName("option").type(boolean.class).build());
    commandRegistry.registerCommand(command);

    // when
    ParsedInput parsedInput = parser.parse(input);
    String commandWithSubcommands = String
            .join(" ", parsedInput.commandName(), String.join(" ", parsedInput.subCommands()))
            .trim();

    // then
    assertThat(commandWithSubcommands).isEqualTo(commandName);
    assertThat(parsedInput.options().size()).isEqualTo(1);
    assertThat(parsedInput.options().get(0).value()).isEqualTo(expectedValue);
    assertThat(parsedInput.arguments().size()).isEqualTo(expectedArguments);
    if (expectedArguments > 0) {
      assertThat(parsedInput.arguments().get(0).value()).isEqualTo(expectedArgumentValue);
    }
  }

  static Stream<Arguments> parseWithBooleanOptionAndArgumentData() {
    return Stream.of(Arguments.of("mycommand --option=false", "mycommand", "false", 0, null),
            Arguments.of("mycommand --option=true", "mycommand", "true", 0, null),
            Arguments.of("mycommand --option false", "mycommand", "false", 0, null),
            Arguments.of("mycommand --option true", "mycommand", "true", 0, null),
            Arguments.of("mycommand --option", "mycommand", "true", 0, null),

            Arguments.of("mycommand --option=false argument", "mycommand", "false", 1, "argument"),
            Arguments.of("mycommand --option=true argument", "mycommand", "true", 1, "argument"),
            Arguments.of("mycommand --option false argument", "mycommand", "false", 1, "argument"),
            Arguments.of("mycommand --option true argument", "mycommand", "true", 1, "argument"),
            Arguments.of("mycommand --option argument", "mycommand", "true", 1, "argument"),

            Arguments.of("mycommand argument --option=false", "mycommand", "false", 1, "argument"),
            Arguments.of("mycommand argument --option=true", "mycommand", "true", 1, "argument"),
            Arguments.of("mycommand argument --option false", "mycommand", "false", 1, "argument"),
            Arguments.of("mycommand argument --option true", "mycommand", "true", 1, "argument"),
            Arguments.of("mycommand argument --option", "mycommand", "true", 1, "argument"),

            Arguments.of("mycommand --option=false true", "mycommand", "false", 1, "true"),
            Arguments.of("mycommand --option=true true", "mycommand", "true", 1, "true"),
            Arguments.of("mycommand --option false false", "mycommand", "false", 1, "false"),
            Arguments.of("mycommand --option true false", "mycommand", "true", 1, "false"),

            Arguments.of("mycommand false --option=false", "mycommand", "false", 1, "false"),
            Arguments.of("mycommand false --option=true", "mycommand", "true", 1, "false"),
            Arguments.of("mycommand true --option false", "mycommand", "false", 1, "true"),
            Arguments.of("mycommand true --option true", "mycommand", "true", 1, "true"),
            Arguments.of("mycommand true --option", "mycommand", "true", 1, "true"),

            Arguments.of("thecommand mysubcommand --option=false", "thecommand mysubcommand", "false", 0, null),
            Arguments.of("thecommand mysubcommand --option=true", "thecommand mysubcommand", "true", 0, null),
            Arguments.of("thecommand mysubcommand --option false", "thecommand mysubcommand", "false", 0, null),
            Arguments.of("thecommand mysubcommand --option true", "thecommand mysubcommand", "true", 0, null),
            Arguments.of("thecommand mysubcommand --option", "thecommand mysubcommand", "true", 0, null),

            Arguments.of("thecommand mysubcommand --option=false argument", "thecommand mysubcommand", "false", 1,
                    "argument"),
            Arguments.of("thecommand mysubcommand --option=true argument", "thecommand mysubcommand", "true", 1,
                    "argument"),
            Arguments.of("thecommand mysubcommand --option false argument", "thecommand mysubcommand", "false", 1,
                    "argument"),
            Arguments.of("thecommand mysubcommand --option true argument", "thecommand mysubcommand", "true", 1,
                    "argument"),
            Arguments.of("thecommand mysubcommand --option argument", "thecommand mysubcommand", "true", 1,
                    "argument"),

            Arguments.of("thecommand mysubcommand argument --option=false", "thecommand mysubcommand", "false", 1,
                    "argument"),
            Arguments.of("thecommand mysubcommand argument --option=true", "thecommand mysubcommand", "true", 1,
                    "argument"),
            Arguments.of("thecommand mysubcommand argument --option false", "thecommand mysubcommand", "false", 1,
                    "argument"),
            Arguments.of("thecommand mysubcommand argument --option true", "thecommand mysubcommand", "true", 1,
                    "argument"),
            Arguments.of("thecommand mysubcommand argument --option", "thecommand mysubcommand", "true", 1,
                    "argument"),

            Arguments.of("thecommand mysubcommand --option=false true", "thecommand mysubcommand", "false", 1,
                    "true"),
            Arguments.of("thecommand mysubcommand --option=true true", "thecommand mysubcommand", "true", 1,
                    "true"),
            Arguments.of("thecommand mysubcommand --option false false", "thecommand mysubcommand", "false", 1,
                    "false"),
            Arguments.of("thecommand mysubcommand --option true false", "thecommand mysubcommand", "true", 1,
                    "false"),

            Arguments.of("thecommand mysubcommand false --option=false", "thecommand mysubcommand", "false", 1,
                    "false"),
            Arguments.of("thecommand mysubcommand false --option=true", "thecommand mysubcommand", "true", 1,
                    "false"),
            Arguments.of("thecommand mysubcommand true --option false", "thecommand mysubcommand", "false", 1,
                    "true"),
            Arguments.of("thecommand mysubcommand true --option true", "thecommand mysubcommand", "true", 1,
                    "true"),
            Arguments.of("thecommand mysubcommand true --option", "thecommand mysubcommand", "true", 1, "true"));
  }

  @ParameterizedTest
  @ValueSource(strings = { "mycommand --help", "mycommand -h" })
  void testParseWithHelpOption(String input) {
    // given
    Command command = createCommand("mycommand", "My test command");
    command.getOptions().add(CommandOption.with().longName("option").shortName('o').build());
    commandRegistry.registerCommand(command);
    // when
    ParsedInput parsedInput = parser.parse(input);

    // then
    assertThat(parsedInput.commandName()).isEqualTo("mycommand");
    assertThat(parsedInput.options().size()).isEqualTo(1);
    assertThat(parsedInput.options().get(0).longName()).isEqualTo("help");
    assertThat(parsedInput.options().get(0).shortName()).isEqualTo('h');
    assertThat(parsedInput.options().get(0).value()).isEqualTo("true");
  }

  @ParameterizedTest
  @ValueSource(strings = { "mycommand mysubcommand --help", "mycommand mysubcommand -h",
          "mycommand --help arg1 --option value1", "mycommand -h arg1" })
  void testParseWithImplicitHelpOption(String input) {
    // given
    Command command = createCommand("mycommand", "My test command");
    command.getOptions().add(CommandOption.with().longName("option").shortName('o').build());
    commandRegistry.registerCommand(command);
    Command subCommand = createCommand("mycommand mysubcommand", "My test sub command");
    subCommand.getOptions().add(CommandOption.with().longName("option").shortName('o').build());
    commandRegistry.registerCommand(subCommand);

    // when
    ParsedInput parsedInput = parser.parse(input);

    // then
    assertThat(parsedInput.options().get(0).value()).isEqualTo("true");
  }

  private static Command createCommand(String name, String description) {
    return new AbstractCommand(name, description) {
      @Override
      public ExitStatus doExecute(CommandContext commandContext) {
        return ExitStatus.OK;
      }
    };
  }

}
