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

package infra.app.test.context;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import infra.lang.Assert;
import infra.test.context.aot.TestAotProcessor;

/**
 * Entry point for AOT processing of a Infra application's tests.
 *
 * <strong>For internal use only.</strong>
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InfraTestAotProcessor extends TestAotProcessor {

  /**
   * Create a new processor for the specified test classpath roots and general settings.
   *
   * @param classpathRoots the classpath roots to scan for test classes
   * @param settings the general AOT processor settings
   */
  public InfraTestAotProcessor(Set<Path> classpathRoots, Settings settings) {
    super(classpathRoots, settings);
  }

  public static void main(String[] args) {
    int requiredArgs = 6;
    Assert.isTrue(args.length >= requiredArgs,
            () -> "Usage: %s <classpathRoots> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId>"
                    .formatted(TestAotProcessor.class.getName()));
    Set<Path> classpathRoots = Arrays.stream(args[0].split(File.pathSeparator))
            .map(Paths::get)
            .collect(Collectors.toSet());
    Settings settings = Settings.builder()
            .sourceOutput(Paths.get(args[1]))
            .resourceOutput(Paths.get(args[2]))
            .classOutput(Paths.get(args[3]))
            .groupId(args[4])
            .artifactId(args[5])
            .build();
    new InfraTestAotProcessor(classpathRoots, settings).process();
  }

}
