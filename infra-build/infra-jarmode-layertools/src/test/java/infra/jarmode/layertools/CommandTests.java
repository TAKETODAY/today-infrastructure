/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.jarmode.layertools;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link Command}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class CommandTests {

  private static final Command.Option VERBOSE_FLAG = Command.Option.flag("verbose", "Verbose output");

  private static final Command.Option LOG_LEVEL_OPTION = Command.Option.of("log-level", "Logging level (debug or info)", "string");

  @Test
  void getNameReturnsName() {
    TestCommand command = new TestCommand("test");
    assertThat(command.getName()).isEqualTo("test");
  }

  @Test
  void getDescriptionReturnsDescription() {
    TestCommand command = new TestCommand("test", "Test description", Command.Options.none(), Command.Parameters.none());
    assertThat(command.getDescription()).isEqualTo("Test description");
  }

  @Test
  void getOptionsReturnsOptions() {
    Command.Options options = Command.Options.of(LOG_LEVEL_OPTION);
    TestCommand command = new TestCommand("test", "test", options, Command.Parameters.none());
    assertThat(command.getOptions()).isEqualTo(options);
  }

  @Test
  void getParametersReturnsParameters() {
    Command.Parameters parameters = Command.Parameters.of("[<param>]");
    TestCommand command = new TestCommand("test", "test", Command.Options.none(), parameters);
    assertThat(command.getParameters()).isEqualTo(parameters);
  }

  @Test
  void runWithOptionsAndParametersParsesOptionsAndParameters() {
    TestCommand command = new TestCommand("test", VERBOSE_FLAG, LOG_LEVEL_OPTION);
    run(command, "--verbose", "--log-level", "test1", "test2", "test3");
    Assertions.assertThat(command.getRunOptions()).containsEntry(VERBOSE_FLAG, null);
    Assertions.assertThat(command.getRunOptions()).containsEntry(LOG_LEVEL_OPTION, "test1");
    assertThat(command.getRunParameters()).containsExactly("test2", "test3");
  }

  @Test
  void runWithUnknownOptionThrowsException() {
    TestCommand command = new TestCommand("test", VERBOSE_FLAG, LOG_LEVEL_OPTION);
    assertThatExceptionOfType(UnknownOptionException.class).isThrownBy(() -> run(command, "--invalid"))
            .withMessage("--invalid");
  }

  @Test
  void runWithOptionMissingRequiredValueThrowsException() {
    TestCommand command = new TestCommand("test", VERBOSE_FLAG, LOG_LEVEL_OPTION);
    assertThatExceptionOfType(MissingValueException.class)
            .isThrownBy(() -> run(command, "--verbose", "--log-level"))
            .withMessage("--log-level");
  }

  @Test
  void findWhenNameMatchesReturnsCommand() {
    TestCommand test1 = new TestCommand("test1");
    TestCommand test2 = new TestCommand("test2");
    List<Command> commands = Arrays.asList(test1, test2);
    assertThat(Command.find(commands, "test1")).isEqualTo(test1);
    assertThat(Command.find(commands, "test2")).isEqualTo(test2);
  }

  @Test
  void findWhenNameDoesNotMatchReturnsNull() {
    TestCommand test1 = new TestCommand("test1");
    TestCommand test2 = new TestCommand("test2");
    List<Command> commands = Arrays.asList(test1, test2);
    assertThat(Command.find(commands, "test3")).isNull();
  }

  @Test
  void parametersOfCreatesParametersInstance() {
    Command.Parameters parameters = Command.Parameters.of("test1", "test2");
    assertThat(parameters.getDescriptions()).containsExactly("test1", "test2");
  }

  @Test
  void optionsNoneReturnsEmptyOptions() {
    Command.Options options = Command.Options.none();
    assertThat(options).extracting("values", as(InstanceOfAssertFactories.ARRAY)).isEmpty();
  }

  @Test
  void optionsOfReturnsOptions() {
    Command.Option option = Command.Option.of("test", "value description", "description");
    Command.Options options = Command.Options.of(option);
    assertThat(options).extracting("values", as(InstanceOfAssertFactories.ARRAY)).containsExactly(option);
  }

  @Test
  void optionFlagCreatesFlagOption() {
    Command.Option option = Command.Option.flag("test", "description");
    assertThat(option.getName()).isEqualTo("test");
    assertThat(option.getDescription()).isEqualTo("description");
    assertThat(option.getValueDescription()).isNull();
  }

  @Test
  void optionOfCreatesValueOption() {
    Command.Option option = Command.Option.of("test", "value description", "description");
    assertThat(option.getName()).isEqualTo("test");
    assertThat(option.getDescription()).isEqualTo("description");
    assertThat(option.getValueDescription()).isEqualTo("value description");
  }

  private void run(TestCommand command, String... args) {
    command.run(new ArrayDeque<>(Arrays.asList(args)));
  }

  static class TestCommand extends Command {

    private Map<Option, String> runOptions;

    private List<String> runParameters;

    TestCommand(String name, Option... options) {
      this(name, "test", Options.of(options), Parameters.none());
    }

    TestCommand(String name, String description, Options options, Parameters parameters) {
      super(name, description, options, parameters);
    }

    @Override
    protected void run(Map<Option, String> options, List<String> parameters) {
      this.runOptions = options;
      this.runParameters = parameters;
    }

    Map<Option, String> getRunOptions() {
      return this.runOptions;
    }

    List<String> getRunParameters() {
      return this.runParameters;
    }

  }

}
