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

package infra.shell.utils;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import infra.context.annotation.Profile;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.env.Environment;
import infra.core.env.Profiles;
import infra.shell.command.AbstractCommand;
import infra.shell.command.Command;
import infra.shell.command.CommandContext;
import infra.shell.command.CommandRegistry;
import infra.shell.command.ExitStatus;
import infra.util.Assert;
import infra.util.StringUtils;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Some text utilities.
 *
 * @author Eric Bottard
 * @author Mahmoud Ben Hassine
 */
public final class Utils {

  private Utils() {
  }

  /**
   * Check if a method is annotated with an active {@link Profile} in the given
   * environment.
   *
   * @param method the method to check
   * @param environment the environment
   * @return true if the method has an active profile, false otherwise
   * @since 4.0.2
   */
  public static boolean isProfileActive(Method method, Environment environment) {
    Profile profile = AnnotatedElementUtils.findMergedAnnotation(method, Profile.class);
    return profile == null || environment.acceptsProfiles(Profiles.parse(profile.value()));
  }

  /**
   * Split a CamelCase class name into separate words. Used to derive command group
   * names. For example, "MyCommands" becomes "My Commands".
   *
   * @param className the simple class name
   * @return the split string
   */
  public static String splitCamelCase(String className) {
    String[] tokens = className.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
    return StringUtils.arrayToDelimitedString(tokens, " ");
  }

  /**
   * Turn CamelCaseText into gnu-style-lowercase.
   */
  public static String unCamelify(CharSequence original) {
    StringBuilder result = new StringBuilder(original.length());
    boolean wasLowercase = false;
    for (int i = 0; i < original.length(); i++) {
      char ch = original.charAt(i);
      if (Character.isUpperCase(ch) && wasLowercase) {
        result.append('-');
      }
      wasLowercase = Character.isLowerCase(ch);
      result.append(Character.toLowerCase(ch));
    }
    return result.toString();
  }

  /**
   * Get a formatted string of available non-hidden commands from the command registry.
   *
   * @param commandRegistry the command registry
   * @return a string of available commands with their descriptions
   * @since 5.0
   */
  public static String formatAvailableCommands(CommandRegistry commandRegistry) {
    StringBuilder stringBuilder = new StringBuilder("AVAILABLE COMMANDS");
    stringBuilder.append(System.lineSeparator()).append(System.lineSeparator());
    Set<Command> commands = commandRegistry.getCommands();
    List<String> groups = commands.stream()
            .filter(command -> !command.isHidden())
            .map(Command::getGroup)
            .distinct()
            .sorted()
            .toList();
    for (String group : groups) {
      stringBuilder.append(group).append(System.lineSeparator());
      for (Command command : commands.stream()
              .filter(c -> !c.isHidden())
              .filter(c -> c.getGroup().equals(group))
              .sorted(Comparator.comparing(Command::getName))
              .toList()) {
        stringBuilder.append("\t")
                .append(command.getName())
                .append(command.getAliases().isEmpty() ? "" : ", " + String.join(", ", command.getAliases()))
                .append(": ")
                .append(command.getDescription())
                .append(System.lineSeparator());
      }
    }
    return stringBuilder.toString();
  }

  public static Object getDefaultValueForPrimitiveType(Class<?> type) {
    Assert.isTrue(type.isPrimitive(), "Type must be a primitive type");
    if (type == boolean.class) {
      return false;
    }
    else if (type == char.class) {
      return '\u0000';
    }
    else if (type == byte.class || type == short.class || type == int.class || type == long.class) {
      return 0;
    }
    else if (type == float.class) {
      return 0f;
    }
    else {
      return 0d;
    }
  }

  // Dummy exit command to show in available commands
  public static final Command QUIT_COMMAND = new AbstractCommand("quit", "Exit the shell", "Built-In Commands") {
    @Override
    public List<String> getAliases() {
      return List.of("exit");
    }

    @Override
    public ExitStatus doExecute(CommandContext commandContext) {
      return ExitStatus.OK;
    }
  };

  private static final ValidatorFactory DEFAULT_VALIDATOR_FACTORY;

  private static final Validator DEFAULT_VALIDATOR;

  static {
    DEFAULT_VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    DEFAULT_VALIDATOR = DEFAULT_VALIDATOR_FACTORY.getValidator();
  }

  /**
   * Gets a default shared validation factory.
   *
   * @return default validation factory
   */
  public static ValidatorFactory defaultValidatorFactory() {
    return DEFAULT_VALIDATOR_FACTORY;
  }

  /**
   * Gets a default shared validator.
   *
   * @return default validator
   */
  public static Validator defaultValidator() {
    return DEFAULT_VALIDATOR;
  }

}
