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

package cn.taketoday.infra.maven;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Helper class to build the command-line arguments of a java process.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class CommandLineBuilder {

  private final List<String> options = new ArrayList<>();

  private final List<URL> classpathElements = new ArrayList<>();

  private final String mainClass;

  private final List<String> arguments = new ArrayList<>();

  private CommandLineBuilder(String mainClass) {
    this.mainClass = mainClass;
  }

  static CommandLineBuilder forMainClass(String mainClass) {
    return new CommandLineBuilder(mainClass);
  }

  CommandLineBuilder withJvmArguments(String... jvmArguments) {
    if (jvmArguments != null) {
      this.options.addAll(Arrays.stream(jvmArguments).filter(Objects::nonNull).toList());
    }
    return this;
  }

  CommandLineBuilder withSystemProperties(Map<String, String> systemProperties) {
    if (systemProperties != null) {
      systemProperties.entrySet()
              .stream()
              .map((e) -> SystemPropertyFormatter.format(e.getKey(), e.getValue()))
              .forEach(this.options::add);
    }
    return this;
  }

  CommandLineBuilder withClasspath(URL... elements) {
    this.classpathElements.addAll(Arrays.asList(elements));
    return this;
  }

  CommandLineBuilder withArguments(String... arguments) {
    if (arguments != null) {
      this.arguments.addAll(Arrays.stream(arguments).filter(Objects::nonNull).toList());
    }
    return this;
  }

  List<String> build() {
    List<String> commandLine = new ArrayList<>();
    if (!this.options.isEmpty()) {
      commandLine.addAll(this.options);
    }
    if (!this.classpathElements.isEmpty()) {
      commandLine.add("-cp");
      commandLine.add(ClasspathBuilder.build(this.classpathElements));
    }
    commandLine.add(this.mainClass);
    if (!this.arguments.isEmpty()) {
      commandLine.addAll(this.arguments);
    }
    return commandLine;
  }

  static class ClasspathBuilder {

    static String build(List<URL> classpathElements) {
      StringBuilder classpath = new StringBuilder();
      for (URL element : classpathElements) {
        if (classpath.length() > 0) {
          classpath.append(File.pathSeparator);
        }
        classpath.append(toFile(element));
      }
      return classpath.toString();
    }

    private static File toFile(URL element) {
      try {
        return new File(element.toURI());
      }
      catch (URISyntaxException ex) {
        throw new IllegalArgumentException(ex);
      }
    }

  }

  /**
   * Format System properties.
   */
  private static class SystemPropertyFormatter {

    static String format(String key, String value) {
      if (key == null) {
        return "";
      }
      if (value == null || value.isEmpty()) {
        return String.format("-D%s", key);
      }
      return String.format("-D%s=\"%s\"", key, value);
    }

  }

}
