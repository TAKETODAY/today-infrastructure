/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.core.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

/**
 * {@link CommandLinePropertySource} implementation backed by a JOpt {@link OptionSet}.
 *
 * <h2>Typical usage</h2>
 *
 * Configure and execute an {@code OptionParser} against the {@code String[]} of arguments
 * supplied to the {@code main} method, and create a {@link JOptCommandLinePropertySource}
 * using the resulting {@code OptionSet} object:
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *     OptionParser parser = new OptionParser();
 *     parser.accepts("option1");
 *     parser.accepts("option2").withRequiredArg();
 *     OptionSet options = parser.parse(args);
 *     PropertySource<?> ps = new JOptCommandLinePropertySource(options);
 *     // ...
 * }</pre>
 *
 * See {@link CommandLinePropertySource} for complete general usage examples.
 *
 * <p>Requires JOpt Simple version 4.3 or higher. Tested against JOpt up until 5.0.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Dave Syer
 * @see CommandLinePropertySource
 * @see joptsimple.OptionParser
 * @see joptsimple.OptionSet
 * @since 4.0
 */
public class JOptCommandLinePropertySource extends CommandLinePropertySource<OptionSet> {

  /**
   * Create a new {@code JOptCommandLinePropertySource} having the default name
   * and backed by the given {@code OptionSet}.
   *
   * @see CommandLinePropertySource#COMMAND_LINE_PROPERTY_SOURCE_NAME
   * @see CommandLinePropertySource#CommandLinePropertySource(Object)
   */
  public JOptCommandLinePropertySource(OptionSet options) {
    super(options);
  }

  /**
   * Create a new {@code JOptCommandLinePropertySource} having the given name
   * and backed by the given {@code OptionSet}.
   */
  public JOptCommandLinePropertySource(String name, OptionSet options) {
    super(name, options);
  }

  @Override
  protected boolean containsOption(String name) {
    return this.source.has(name);
  }

  @NonNull
  @Override
  public List<String> getPropertyNames() {
    ArrayList<String> names = new ArrayList<>();
    for (OptionSpec<?> spec : this.source.specs()) {
      String lastOption = CollectionUtils.lastElement(spec.options());
      if (lastOption != null) {
        // Only the longest name is used for enumerating
        names.add(lastOption);
      }
    }
    return names;
  }

  @Override
  @Nullable
  public List<String> getOptionValues(String name) {
    List<?> argValues = this.source.valuesOf(name);
    ArrayList<String> stringArgValues = new ArrayList<>();
    for (Object argValue : argValues) {
      stringArgValues.add(argValue.toString());
    }
    if (stringArgValues.isEmpty()) {
      return (this.source.has(name) ? Collections.emptyList() : null);
    }
    return Collections.unmodifiableList(stringArgValues);
  }

  @Override
  protected List<String> getNonOptionArgs() {
    List<?> argValues = this.source.nonOptionArguments();
    List<String> stringArgValues = new ArrayList<>();
    for (Object argValue : argValues) {
      stringArgValues.add(argValue.toString());
    }
    return (stringArgValues.isEmpty() ? Collections.emptyList() :
            Collections.unmodifiableList(stringArgValues));
  }

}
