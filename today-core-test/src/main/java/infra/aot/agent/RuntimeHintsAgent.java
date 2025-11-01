/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aot.agent;

import org.jspecify.annotations.Nullable;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

import infra.aot.hint.RuntimeHints;
import infra.util.StringUtils;

/**
 * Java Agent that records method invocations related to {@link RuntimeHints} metadata.
 * <p>This agent uses {@link java.lang.instrument.ClassFileTransformer class transformers}
 * that modify bytecode to intercept and record method invocations at runtime.
 * <p>By default, this agent only instruments code in the {@code infra} package.
 * Instrumented packages can be configured by passing an argument string to the {@code -javaagent}
 * option, as a comma-separated list of packages to instrument prefixed with {@code "+"}
 * and packages to ignore prefixed with {@code "-"}:
 * <pre class="code">
 *   -javaagent:/path/to/today-core-test.jar=+infra,+org.example")
 * </pre>
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see InvocationsRecorderClassTransformer
 * @since 4.0
 */
public final class RuntimeHintsAgent {

  private static boolean loaded = false;

  private RuntimeHintsAgent() {
  }

  public static void premain(@Nullable String agentArgs, Instrumentation inst) {
    loaded = true;
    ParsedArguments arguments = ParsedArguments.parse(agentArgs);
    InvocationsRecorderClassTransformer transformer = new InvocationsRecorderClassTransformer(
            arguments.getInstrumentedPackages(), arguments.getIgnoredPackages());
    inst.addTransformer(transformer);
  }

  /**
   * Static accessor for detecting whether the agent is loaded in the current JVM.
   *
   * @return whether the agent is active for the current JVM
   */
  public static boolean isLoaded() {
    return loaded;
  }

  static final class ParsedArguments {

    List<String> instrumentedPackages;

    List<String> ignoredPackages;

    ParsedArguments(List<String> instrumentedPackages, List<String> ignoredPackages) {
      this.instrumentedPackages = instrumentedPackages;
      this.ignoredPackages = ignoredPackages;
    }

    public String[] getInstrumentedPackages() {
      return this.instrumentedPackages.toArray(new String[0]);
    }

    public String[] getIgnoredPackages() {
      return this.ignoredPackages.toArray(new String[0]);
    }

    static ParsedArguments parse(@Nullable String agentArgs) {
      List<String> included = new ArrayList<>();
      List<String> excluded = new ArrayList<>();
      if (StringUtils.hasText(agentArgs)) {
        for (String argument : agentArgs.split(",")) {
          if (argument.startsWith("+")) {
            included.add(argument.substring(1));
          }
          else if (argument.startsWith("-")) {
            excluded.add(argument.substring(1));
          }
          else {
            throw new IllegalArgumentException("Cannot parse agent arguments [" + agentArgs + "]");
          }
        }
      }
      else {
        included.add("infra");
      }
      return new ParsedArguments(included, excluded);
    }

  }
}
