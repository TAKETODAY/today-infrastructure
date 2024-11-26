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

package infra.app.loader.tools;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import infra.lang.Assert;
import infra.util.StringUtils;

/**
 * Provides access to the java binary executable, regardless of OS.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JavaExecutable {

  private final File file;

  public JavaExecutable() {
    String javaHome = System.getProperty("java.home");
    Assert.state(StringUtils.isNotEmpty(javaHome), "Unable to find java executable due to missing 'java.home'");
    this.file = findInJavaHome(javaHome);
  }

  private File findInJavaHome(String javaHome) {
    File bin = new File(new File(javaHome), "bin");
    File command = new File(bin, "java.exe");
    command = command.exists() ? command : new File(bin, "java");
    Assert.state(command.exists(), () -> "Unable to find java in " + javaHome);
    return command;
  }

  /**
   * Create a new {@link ProcessBuilder} that will run with the Java executable.
   *
   * @param arguments the command arguments
   * @return a {@link ProcessBuilder}
   */
  public ProcessBuilder processBuilder(String... arguments) {
    ProcessBuilder processBuilder = new ProcessBuilder(toString());
    processBuilder.command().addAll(Arrays.asList(arguments));
    return processBuilder;
  }

  @Override
  public String toString() {
    try {
      return this.file.getCanonicalPath();
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
