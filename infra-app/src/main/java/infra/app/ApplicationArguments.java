/*
 * Copyright 2012-present the original author or authors.
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

package infra.app;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.core.env.SimpleCommandLinePropertySource;
import infra.lang.Assert;

/**
 * Provides access to the arguments that were used to run a {@link Application}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 19:57
 */
public class ApplicationArguments {
  public static final String BEAN_NAME = "applicationArguments";

  private final Source source;

  private final String[] args;

  public ApplicationArguments(String... args) {
    Assert.notNull(args, "Args is required");
    this.source = new Source(args);
    this.args = args;
  }

  /**
   * Return the raw unprocessed arguments that were passed to the application.
   *
   * @return the arguments
   */
  public String[] getSourceArgs() {
    return this.args;
  }

  /**
   * Return the names of all option arguments. For example, if the arguments were
   * "--foo=bar --debug" would return the values {@code ["foo", "debug"]}.
   *
   * @return the option names or an empty set
   */
  public Set<String> getOptionNames() {
    String[] names = this.source.getPropertyNames();
    return Set.of(names);
  }

  /**
   * Return whether the set of option arguments parsed from the arguments contains an
   * option with the given name.
   *
   * @param name the name to check
   * @return {@code true} if the arguments contain an option with the given name
   */
  public boolean containsOption(String name) {
    return this.source.containsProperty(name);
  }

  /**
   * Return the collection of values associated with the arguments option having the
   * given name.
   * <ul>
   * <li>if the option is present and has no argument (e.g.: "--foo"), return an empty
   * collection ({@code []})</li>
   * <li>if the option is present and has a single value (e.g. "--foo=bar"), return a
   * collection having one element ({@code ["bar"]})</li>
   * <li>if the option is present and has multiple values (e.g. "--foo=bar --foo=baz"),
   * return a collection having elements for each value ({@code ["bar", "baz"]})</li>
   * <li>if the option is not present, return {@code null}</li>
   * </ul>
   *
   * @param name the name of the option
   * @return a list of option values for the given name
   */
  @Nullable
  public List<String> getOptionValues(String name) {
    List<String> values = this.source.getOptionValues(name);
    return values != null ? Collections.unmodifiableList(values) : null;
  }

  /**
   * Return the collection of non-option arguments parsed.
   *
   * @return the non-option arguments or an empty list
   */
  public List<String> getNonOptionArgs() {
    return this.source.getNonOptionArgs();
  }

  private static class Source extends SimpleCommandLinePropertySource {

    Source(String[] args) {
      super(args);
    }

    @Override
    public List<String> getNonOptionArgs() {
      return super.getNonOptionArgs();
    }

    @Nullable
    @Override
    public List<String> getOptionValues(String name) {
      return super.getOptionValues(name);
    }

  }

}
