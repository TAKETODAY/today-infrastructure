/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.buildpack.platform.docker.type.Binding;
import cn.taketoday.buildpack.platform.docker.type.ContainerConfig;

import cn.taketoday.util.StringUtils;

/**
 * An individual build phase executed as part of a {@link Lifecycle} run.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class Phase {

  private final String name;

  private final boolean verboseLogging;

  private boolean daemonAccess = false;

  private final List<String> args = new ArrayList<>();

  private final List<Binding> bindings = new ArrayList<>();

  private final Map<String, String> env = new LinkedHashMap<>();

  private final List<String> securityOptions = new ArrayList<>();

  private String networkMode;

  /**
   * Create a new {@link Phase} instance.
   *
   * @param name the name of the phase
   * @param verboseLogging if verbose logging is requested
   */
  Phase(String name, boolean verboseLogging) {
    this.name = name;
    this.verboseLogging = verboseLogging;
  }

  /**
   * Update this phase with Docker daemon access.
   */
  void withDaemonAccess() {
    this.daemonAccess = true;
  }

  /**
   * Update this phase with a debug log level arguments if verbose logging has been
   * requested.
   */
  void withLogLevelArg() {
    if (this.verboseLogging) {
      this.args.add("-log-level");
      this.args.add("debug");
    }
  }

  /**
   * Update this phase with additional run arguments.
   *
   * @param args the arguments to add
   */
  void withArgs(Object... args) {
    Arrays.stream(args).map(Object::toString).forEach(this.args::add);
  }

  /**
   * Update this phase with an addition volume binding.
   *
   * @param binding the binding
   */
  void withBinding(Binding binding) {
    this.bindings.add(binding);
  }

  /**
   * Update this phase with an additional environment variable.
   *
   * @param name the variable name
   * @param value the variable value
   */
  void withEnv(String name, String value) {
    this.env.put(name, value);
  }

  /**
   * Update this phase with the network the build container will connect to.
   *
   * @param networkMode the network
   */
  void withNetworkMode(String networkMode) {
    this.networkMode = networkMode;
  }

  /**
   * Update this phase with a security option.
   *
   * @param option the security option
   */
  void withSecurityOption(String option) {
    this.securityOptions.add(option);
  }

  /**
   * Return the name of the phase.
   *
   * @return the phase name
   */
  String getName() {
    return this.name;
  }

  @Override
  public String toString() {
    return this.name;
  }

  /**
   * Apply this phase settings to a {@link ContainerConfig} update.
   *
   * @param update the update to apply the phase to
   */
  void apply(ContainerConfig.Update update) {
    if (this.daemonAccess) {
      update.withUser("root");
    }
    update.withCommand("/cnb/lifecycle/" + this.name, StringUtils.toStringArray(this.args));
    update.withLabel("author", "infra-app");
    this.bindings.forEach(update::withBinding);
    this.env.forEach(update::withEnv);
    if (this.networkMode != null) {
      update.withNetworkMode(this.networkMode);
    }
    this.securityOptions.forEach(update::withSecurityOption);
  }

}
