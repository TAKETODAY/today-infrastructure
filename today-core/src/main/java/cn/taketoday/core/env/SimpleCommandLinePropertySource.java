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

import java.util.List;

import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link CommandLinePropertySource} implementation backed by a simple String array.
 *
 * <h3>Purpose</h3>
 * <p>This {@code CommandLinePropertySource} implementation aims to provide the simplest
 * possible approach to parsing command line arguments. As with all {@code
 * CommandLinePropertySource} implementations, command line arguments are broken into two
 * distinct groups: <em>option arguments</em> and <em>non-option arguments</em>, as
 * described below <em>(some sections copied from Javadoc for
 * {@link SimpleCommandLineArgsParser})</em>:
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
 * <pre>{@code
 * --foo
 * --foo=
 * --foo=""
 * --foo=bar
 * --foo="bar then baz"
 * --foo=bar,baz,biz
 * }</pre>
 *
 * <h4>Invalid examples of option arguments</h4>
 * <pre>{@code
 * -foo
 * --foo bar
 * --foo = bar
 * --foo=bar --foo=baz --foo=biz
 * }</pre>
 *
 * <h3>End of option arguments</h3>
 * <p>The underlying parser supports the POSIX "end of options" delimiter, meaning
 * that any {@code "--"} (empty option name) in the command line signals that all
 * remaining arguments are non-option arguments. For example, {@code "--opt1=ignored"},
 * {@code "--opt2"}, and {@code "filename"} in the following command line are
 * considered non-option arguments.
 * <pre class="code">
 * --foo=bar -- --opt1=ignored -opt2 filename</pre>
 *
 * <h3>Working with non-option arguments</h3>
 * <p>Any arguments following the "end of options" delimiter ({@code --}) or
 * specified without the "{@code --}" option prefix will be considered as
 * "non-option arguments" and made available through the
 * {@link CommandLineArgs#getNonOptionArgs()} method.
 *
 * <h3>Typical usage</h3>
 * <pre>{@code
 * public static void main(String[] args) {
 *     PropertySource<?> ps = new SimpleCommandLinePropertySource(args);
 *     // ...
 * }
 * }</pre>
 *
 * See {@link CommandLinePropertySource} for complete general usage examples.
 *
 * <h3>Beyond the basics</h3>
 *
 * <p>When more fully-featured command line parsing is necessary, consider
 * implementing your own {@code CommandLinePropertySource} against the command line
 * parsing library of your choice.
 *
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see CommandLinePropertySource
 * @see JOptCommandLinePropertySource
 * @since 4.0
 */
public class SimpleCommandLinePropertySource extends CommandLinePropertySource<CommandLineArgs> {

  /**
   * Create a new {@code SimpleCommandLinePropertySource} having the default name
   * and backed by the given {@code String[]} of command line arguments.
   *
   * @see CommandLinePropertySource#COMMAND_LINE_PROPERTY_SOURCE_NAME
   * @see CommandLinePropertySource#CommandLinePropertySource(Object)
   */
  public SimpleCommandLinePropertySource(String... args) {
    super(SimpleCommandLineArgsParser.parse(args));
  }

  /**
   * Create a new {@code SimpleCommandLinePropertySource} having the given name
   * and backed by the given {@code String[]} of command line arguments.
   */
  public SimpleCommandLinePropertySource(String name, String[] args) {
    super(name, SimpleCommandLineArgsParser.parse(args));
  }

  /**
   * Get the property names for the option arguments.
   */
  @NonNull
  @Override
  public String[] getPropertyNames() {
    return StringUtils.toStringArray(this.source.getOptionNames());
  }

  @Override
  protected boolean containsOption(String name) {
    return this.source.containsOption(name);
  }

  @Override
  @Nullable
  protected List<String> getOptionValues(String name) {
    return this.source.getOptionValues(name);
  }

  @Override
  protected List<String> getNonOptionArgs() {
    return this.source.getNonOptionArgs();
  }

}
