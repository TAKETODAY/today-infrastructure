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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import cn.taketoday.lang.Nullable;

/**
 * A simple representation of command line arguments, broken into "option arguments" and
 * "non-option arguments".
 *
 * @author Chris Beams
 * @see SimpleCommandLineArgsParser
 * @since 4.0
 */
final class CommandLineArgs {

  private final ArrayList<String> nonOptionArgs = new ArrayList<>();
  private final HashMap<String, List<String>> optionArgs = new HashMap<>();

  /**
   * Add an option argument for the given option name and add the given value to the
   * list of values associated with this option (of which there may be zero or more).
   * The given value may be {@code null}, indicating that the option was specified
   * without an associated value (e.g. "--foo" vs. "--foo=bar").
   */
  public void addOptionArg(String optionName, @Nullable String optionValue) {
    if (!this.optionArgs.containsKey(optionName)) {
      this.optionArgs.put(optionName, new ArrayList<>());
    }
    if (optionValue != null) {
      this.optionArgs.get(optionName).add(optionValue);
    }
  }

  /**
   * Return the set of all option arguments present on the command line.
   */
  public Set<String> getOptionNames() {
    return Collections.unmodifiableSet(this.optionArgs.keySet());
  }

  /**
   * Return whether the option with the given name was present on the command line.
   */
  public boolean containsOption(String optionName) {
    return this.optionArgs.containsKey(optionName);
  }

  /**
   * Return the list of values associated with the given option. {@code null} signifies
   * that the option was not present; empty list signifies that no values were associated
   * with this option.
   */
  @Nullable
  public List<String> getOptionValues(String optionName) {
    return this.optionArgs.get(optionName);
  }

  /**
   * Add the given value to the list of non-option arguments.
   */
  public void addNonOptionArg(String value) {
    this.nonOptionArgs.add(value);
  }

  /**
   * Return the list of non-option arguments specified on the command line.
   */
  public List<String> getNonOptionArgs() {
    return Collections.unmodifiableList(this.nonOptionArgs);
  }

}
