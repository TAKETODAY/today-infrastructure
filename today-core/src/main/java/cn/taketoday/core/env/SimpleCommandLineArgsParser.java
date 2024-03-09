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

package cn.taketoday.core.env;

/**
 * Parses a {@code String[]} of command line arguments in order to populate a
 * {@link CommandLineArgs} object.
 *
 * <h3>Working with option arguments</h3>
 * <p>Option arguments must adhere to the exact syntax:
 *
 * <pre class="code">--optName[=optValue]</pre>
 *
 * <p>That is, options must be prefixed with "{@code --}" and may or may not
 * specify a value. If a value is specified, the name and value must be separated
 * <em>without spaces</em> by an equals sign ("="). The value may optionally be
 * an empty string.
 *
 * <h4>Valid examples of option arguments</h4>
 * <pre class="code">
 * --foo
 * --foo=
 * --foo=""
 * --foo=bar
 * --foo="bar then baz"
 * --foo=bar,baz,biz</pre>
 *
 * <h4>Invalid examples of option arguments</h4>
 * <pre class="code">
 * -foo
 * --foo bar
 * --foo = bar
 * --foo=bar --foo=baz --foo=biz</pre>
 *
 * <h3>Working with non-option arguments</h3>
 * <p>Any and all arguments specified at the command line without the "{@code --}"
 * option prefix will be considered as "non-option arguments" and made available
 * through the {@link CommandLineArgs#getNonOptionArgs()} method.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class SimpleCommandLineArgsParser {

  /**
   * Parse the given {@code String} array based on the rules described {@linkplain
   * SimpleCommandLineArgsParser above}, returning a fully-populated
   * {@link CommandLineArgs} object.
   *
   * @param args command line arguments, typically from a {@code main()} method
   */
  public static CommandLineArgs parse(String... args) {
    CommandLineArgs commandLineArgs = new CommandLineArgs();
    boolean endOfOptions = false;
    for (String arg : args) {
      if (!endOfOptions && arg.startsWith("--")) {
        String optionText = arg.substring(2);
        int indexOfEqualsSign = optionText.indexOf('=');
        if (indexOfEqualsSign > -1) {
          String optionName = optionText.substring(0, indexOfEqualsSign);
          String optionValue = optionText.substring(indexOfEqualsSign + 1);
          if (optionName.isEmpty()) {
            throw new IllegalArgumentException("Invalid argument syntax: " + arg);
          }
          commandLineArgs.addOptionArg(optionName, optionValue);
        }
        else if (!optionText.isEmpty()) {
          commandLineArgs.addOptionArg(optionText, null);
        }
        else {
          // '--' End of options delimiter, all remaining args are non-option arguments
          endOfOptions = true;
        }
      }
      else {
        commandLineArgs.addNonOptionArg(arg);
      }
    }
    return commandLineArgs;
  }

}
