/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.aot;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import infra.aot.generate.GeneratedFiles;
import infra.context.aot.AbstractAotProcessor;
import infra.context.aot.ContextAotProcessor;

/**
 * Filesystem-based ahead-of-time (AOT) processing base implementation that scans
 * the provided classpath roots for Infra integration test classes and then
 * generates AOT artifacts for those test classes in the configured output directories.
 *
 * <p>Concrete implementations are typically used to kick off optimization of a
 * test suite in a build tool.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TestContextAotGenerator
 * @see ContextAotProcessor
 * @since 4.0
 */
public abstract class TestAotProcessor extends AbstractAotProcessor<Void> {

  private final Set<Path> classpathRoots;

  /**
   * Create a new processor for the specified test classpath roots and
   * common settings.
   *
   * @param classpathRoots the classpath roots to scan for test classes
   * @param settings the settings to apply
   */
  protected TestAotProcessor(Set<Path> classpathRoots, Settings settings) {
    super(settings);
    this.classpathRoots = classpathRoots;
  }

  /**
   * Get the classpath roots to scan for test classes.
   */
  protected Set<Path> getClasspathRoots() {
    return this.classpathRoots;
  }

  /**
   * Trigger processing of the test classes by
   * {@linkplain #deleteExistingOutput() clearing output directories} first and
   * then {@linkplain #performAotProcessing() performing AOT processing}.
   */
  @Override
  protected Void doProcess() {
    deleteExistingOutput();
    performAotProcessing();
    return null;
  }

  /**
   * Perform ahead-of-time processing of Infra integration test classes.
   * <p>Code, resources, and generated classes are stored in the configured
   * output directories. In addition, run-time hints are registered for the
   * application contexts used by the test classes as well as test infrastructure
   * components used by the tests.
   *
   * @see #scanClasspathRoots()
   * @see #createFileSystemGeneratedFiles()
   * @see TestContextAotGenerator#processAheadOfTime(Stream)
   * @see #writeHints(infra.aot.hint.RuntimeHints)
   */
  protected void performAotProcessing() {
    Stream<Class<?>> testClasses = scanClasspathRoots();
    GeneratedFiles generatedFiles = createFileSystemGeneratedFiles();
    TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
    generator.processAheadOfTime(testClasses);
    writeHints(generator.getRuntimeHints());
  }

  /**
   * Scan the configured {@linkplain #getClasspathRoots() classpath roots} for
   * Infra integration test classes.
   *
   * @return a stream of Infra integration test classes
   */
  protected Stream<Class<?>> scanClasspathRoots() {
    TestClassScanner scanner = new TestClassScanner(getClasspathRoots());
    return scanner.scan();
  }

}
