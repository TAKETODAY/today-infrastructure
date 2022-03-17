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

package cn.taketoday.framework;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.taketoday.core.env.SimpleCommandLinePropertySource;
import cn.taketoday.lang.Assert;

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
    Assert.notNull(args, "Args must not be null");
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
    Set<String> names = this.source.getPropertyNames();
    return Collections.unmodifiableSet(names);
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

    @Override
    public List<String> getOptionValues(String name) {
      return super.getOptionValues(name);
    }

  }

}
