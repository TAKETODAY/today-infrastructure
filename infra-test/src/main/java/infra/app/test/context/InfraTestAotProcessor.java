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
