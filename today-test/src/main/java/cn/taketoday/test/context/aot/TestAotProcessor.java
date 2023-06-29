/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.aot;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.aot.generate.GeneratedFiles;
import cn.taketoday.context.aot.AbstractAotProcessor;

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
 * @see cn.taketoday.context.aot.ContextAotProcessor
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
   * @see #writeHints(cn.taketoday.aot.hint.RuntimeHints)
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
