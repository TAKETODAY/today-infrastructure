/*
 * Copyright 2012 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jarmode.layertools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * A command that can be launched from the layertools jarmode.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class Command {

  private final String name;

  private final String description;

  private final Options options;

  private final Parameters parameters;

  /**
   * Create a new {@link Command} instance.
   *
   * @param name the name of the command
   * @param description a description of the command
   * @param options the command options
   * @param parameters the command parameters
   */
  Command(String name, String description, Options options, Parameters parameters) {
    this.name = name;
    this.description = description;
    this.options = options;
    this.parameters = parameters;
  }

  /**
   * Return the name of this command.
   *
   * @return the command name
   */
  String getName() {
    return this.name;
  }

  /**
   * Return the description of this command.
   *
   * @return the command description
   */
  String getDescription() {
    return this.description;
  }

  /**
   * Return options that this command accepts.
   *
   * @return the command options
   */
  Options getOptions() {
    return this.options;
  }

  /**
   * Return parameters that this command accepts.
   *
   * @return the command parameters
   */
  Parameters getParameters() {
    return this.parameters;
  }

  /**
   * Run the command by processing the remaining arguments.
   *
   * @param args a mutable deque of the remaining arguments
   */
  final void run(Deque<String> args) {
    List<String> parameters = new ArrayList<>();
    Map<Option, String> options = new HashMap<>();
    while (!args.isEmpty()) {
      String arg = args.removeFirst();
      Option option = this.options.find(arg);
      if (option != null) {
        options.put(option, option.claimArg(args));
      }
      else {
        parameters.add(arg);
      }
    }
    run(options, parameters);
  }

  /**
   * Run the actual command.
   *
   * @param options any options extracted from the arguments
   * @param parameters any parameters extracted from the arguments
   */
  protected abstract void run(Map<Option, String> options, List<String> parameters);

  /**
   * Static method that can be used to find a single command from a collection.
   *
   * @param commands the commands to search
   * @param name the name of the command to find
   * @return a {@link Command} instance or {@code null}.
   */
  static Command find(Collection<? extends Command> commands, String name) {
    for (Command command : commands) {
      if (command.getName().equals(name)) {
        return command;
      }
    }
    return null;
  }

  /**
   * Parameters that the command accepts.
   */
  protected static final class Parameters {

    private final List<String> descriptions;

    private Parameters(String[] descriptions) {
      this.descriptions = Collections.unmodifiableList(Arrays.asList(descriptions));
    }

    /**
     * Return the parameter descriptions.
     *
     * @return the descriptions
     */
    List<String> getDescriptions() {
      return this.descriptions;
    }

    @Override
    public String toString() {
      return this.descriptions.toString();
    }

    /**
     * Factory method used if there are no expected parameters.
     *
     * @return a new {@link Parameters} instance
     */
    protected static Parameters none() {
      return of();
    }

    /**
     * Factory method used to create a new {@link Parameters} instance with specific
     * descriptions.
     *
     * @param descriptions the parameter descriptions
     * @return a new {@link Parameters} instance with the given descriptions
     */
    protected static Parameters of(String... descriptions) {
      return new Parameters(descriptions);
    }

  }

  /**
   * Options that the command accepts.
   */
  protected static final class Options {

    private final Option[] values;

    private Options(Option[] values) {
      this.values = values;
    }

    private Option find(String arg) {
      if (arg.startsWith("--")) {
        String name = arg.substring(2);
        for (Option candidate : this.values) {
          if (candidate.getName().equals(name)) {
            return candidate;
          }
        }
        throw new UnknownOptionException(name);
      }
      return null;
    }

    /**
     * Return if this options collection is empty.
     *
     * @return if there are no options
     */
    boolean isEmpty() {
      return this.values.length == 0;
    }

    /**
     * Return a stream of each option.
     *
     * @return a stream of the options
     */
    Stream<Option> stream() {
      return Arrays.stream(this.values);
    }

    /**
     * Factory method used if there are no expected options.
     *
     * @return a new {@link Options} instance
     */
    protected static Options none() {
      return of();
    }

    /**
     * Factory method used to create a new {@link Options} instance with specific
     * values.
     *
     * @param values the option values
     * @return a new {@link Options} instance with the given values
     */
    protected static Options of(Option... values) {
      return new Options(values);
    }

  }

  /**
   * An individual option that the command can accepts. Can either be an option with a
   * value (e.g. {@literal --log debug}) or a flag (e.g. {@literal
   * --verbose}).
   */
  protected static final class Option {

    private final String name;

    private final String valueDescription;

    private final String description;

    private Option(String name, String valueDescription, String description) {
      this.name = name;
      this.description = description;
      this.valueDescription = valueDescription;
    }

    /**
     * Return the name of the option.
     *
     * @return the options name
     */
    String getName() {
      return this.name;
    }

    /**
     * Return the description of the expected argument value or {@code null} if this
     * option is a flag/switch.
     *
     * @return the option value description
     */
    String getValueDescription() {
      return this.valueDescription;
    }

    /**
     * Return the name and the value description combined.
     *
     * @return the name and value description
     */
    String getNameAndValueDescription() {
      return this.name + ((this.valueDescription != null) ? " " + this.valueDescription : "");
    }

    /**
     * Return a description of the option.
     *
     * @return the option description
     */
    String getDescription() {
      return this.description;
    }

    private String claimArg(Deque<String> args) {
      if (this.valueDescription != null) {
        if (args.isEmpty()) {
          throw new MissingValueException(this.name);
        }
        return args.removeFirst();
      }
      return null;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      return this.name.equals(((Option) obj).name);
    }

    @Override
    public int hashCode() {
      return this.name.hashCode();
    }

    @Override
    public String toString() {
      return this.name;
    }

    /**
     * Factory method to create a flag/switch option.
     *
     * @param name the name of the option
     * @param description a description of the option
     * @return a new {@link Option} instance
     */
    protected static Option flag(String name, String description) {
      return new Option(name, null, description);
    }

    /**
     * Factory method to create value option.
     *
     * @param name the name of the option
     * @param valueDescription a description of the expected value
     * @param description a description of the option
     * @return a new {@link Option} instance
     */
    protected static Option of(String name, String valueDescription, String description) {
      return new Option(name, valueDescription, description);
    }

  }

}
