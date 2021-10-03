/*
 * Copyright 2002-2018 the original author or authors.
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

package cn.taketoday.core.env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.NonNull;
import cn.taketoday.core.Nullable;
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
 * @since 3.1
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
