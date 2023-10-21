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

package cn.taketoday.test.context.aot;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.junit.platform.launcher.listeners.TestExecutionSummary.Failure;
import org.opentest4j.MultipleFailuresError;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.aot.generate.GeneratedFiles.Kind;
import cn.taketoday.aot.generate.InMemoryGeneratedFiles;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.test.generate.CompilerFiles;
import cn.taketoday.core.test.tools.CompileWithForkedClassLoader;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterImportedConfigTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterSharedConfigTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraVintageTests;
import cn.taketoday.test.context.aot.samples.basic.DisabledInAotProcessingTests;
import cn.taketoday.test.context.aot.samples.basic.DisabledInAotRuntimeClassLevelTests;
import cn.taketoday.test.context.aot.samples.basic.DisabledInAotRuntimeMethodLevelTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.TagFilter.excludeTags;

/**
 * End-to-end integration tests for AOT support in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@Disabled
@CompileWithForkedClassLoader
class AotIntegrationTests extends AbstractAotTests {

  private static final String CLASSPATH_ROOT = "AotSmokeTests.classpath_root";

  // We have to determine the classpath root and store it in a system property
  // since @CompileWithTargetClassAccess uses a custom ClassLoader that does
  // not support CodeSource.
  //
  // The system property will only be set when this class is loaded by the
  // original ClassLoader used to launch the JUnit Platform. The attempt to
  // access the CodeSource will fail when the tests are executed in the
  // nested JUnit Platform launched by the CompileWithTargetClassAccessExtension.
  static {
    try {
      Path classpathRoot = Paths.get(AotIntegrationTests.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      System.setProperty(CLASSPATH_ROOT, classpathRoot.toFile().getCanonicalPath());
    }
    catch (Exception ex) {
      // ignore
    }
  }

  @Test
  void endToEndTests() {
    // AOT BUILD-TIME: CLASSPATH SCANNING
    Stream<Class<?>> testClasses = createTestClassScanner().scan("cn.taketoday.test.context.aot.samples.basic");

    // AOT BUILD-TIME: PROCESSING
    InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
    TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles, new RuntimeHints());
    generator.processAheadOfTime(testClasses);

    List<String> sourceFiles = generatedFiles.getGeneratedFiles(Kind.SOURCE).keySet().stream().toList();
    assertThat(sourceFiles).containsExactlyInAnyOrder(expectedSourceFilesForBasicInfraTests);

    // AOT BUILD-TIME: COMPILATION
    TestCompiler.forSystem().with(CompilerFiles.from(generatedFiles))
            // .printFiles(System.out)
            .compile(compiled ->
                    // AOT RUN-TIME: EXECUTION
                    runTestsInAotMode(5, List.of(
                            BasicInfraJupiterSharedConfigTests.class,
                            BasicInfraJupiterTests.class, // NestedTests get executed automatically
                            // Run @Import tests AFTER the tests with otherwise identical config
                            // in order to ensure that the other test classes are not accidentally
                            // using the config for the @Import tests.
                            BasicInfraJupiterImportedConfigTests.class,
                            BasicInfraVintageTests.class,
                            /* 0 */ DisabledInAotProcessingTests.class,
                            /* 0 */ DisabledInAotRuntimeClassLevelTests.class,
                            /* 1 */ DisabledInAotRuntimeMethodLevelTests.class)));
  }

  @Disabled("Uncomment to run all Infra integration tests in `today-test`")
  @Test
  void endToEndTestsForEntireInfraTestModule() {
    // AOT BUILD-TIME: CLASSPATH SCANNING
    List<Class<?>> testClasses =
            // FYI: you can limit execution to a particular set of test classes as follows.
            // List.of(DirtiesContextTransactionalTestNGInfraContextTests.class, ...);
            createTestClassScanner()
                    .scan()
                    // FYI: you can limit execution to a particular package and its subpackages as follows.
                    // .scan("cn.taketoday.test.context.junit.jupiter")
                    .toList();

    // AOT BUILD-TIME: PROCESSING
    InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
    TestContextAotGenerator generator = new TestContextAotGenerator(generatedFiles);
    generator.processAheadOfTime(testClasses.stream());

    // AOT BUILD-TIME: COMPILATION
    TestCompiler.forSystem().with(CompilerFiles.from(generatedFiles))
            // .printFiles(System.out)
            .compile(compiled ->
                    // AOT RUN-TIME: EXECUTION
                    runTestsInAotMode(testClasses));
  }

  private static void runTestsInAotMode(List<Class<?>> testClasses) {
    runTestsInAotMode(-1, testClasses);
  }

  private static void runTestsInAotMode(long expectedNumTests, List<Class<?>> testClasses) {
    try {
      System.setProperty(AotDetector.AOT_ENABLED, "true");

      LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request()
              .filters(ClassNameFilter.includeClassNamePatterns(".*Tests?$"))
              .filters(excludeTags("failing-test-case"));
      testClasses.forEach(testClass -> builder.selectors(selectClass(testClass)));
      LauncherDiscoveryRequest request = builder.build();
      SummaryGeneratingListener listener = new SummaryGeneratingListener();
      LauncherFactory.create().execute(request, listener);
      TestExecutionSummary summary = listener.getSummary();
      if (expectedNumTests < 0) {
        summary.printTo(new PrintWriter(System.err));
      }
      if (summary.getTotalFailureCount() > 0) {
        printFailingTestClasses(summary);
        List<Throwable> exceptions = summary.getFailures().stream().map(Failure::getException).toList();
        throw new MultipleFailuresError("Test execution failures", exceptions);
      }
      if (expectedNumTests >= 0) {
        assertThat(summary.getTestsSucceededCount()).isEqualTo(expectedNumTests);
      }
    }
    finally {
      System.clearProperty(AotDetector.AOT_ENABLED);
    }
  }

  private static void printFailingTestClasses(TestExecutionSummary summary) {
    System.err.println("Failing Test Classes:");
    summary.getFailures().stream()
            .map(Failure::getTestIdentifier)
            .map(TestIdentifier::getSource)
            .flatMap(Optional::stream)
            .map(TestSource.class::cast)
            .map(source -> {
              if (source instanceof ClassSource classSource) {
                return getJavaClass(classSource);
              }
              else if (source instanceof MethodSource methodSource) {
                return getJavaClass(methodSource);
              }
              return Optional.<Class<?>>empty();
            })
            .flatMap(Optional::stream)
            .map(Class::getName)
            .distinct()
            .sorted()
            .forEach(System.err::println);
    System.err.println();
  }

  private static Optional<Class<?>> getJavaClass(ClassSource classSource) {
    try {
      return Optional.of(classSource.getJavaClass());
    }
    catch (Exception ex) {
      // ignore exception
      return Optional.empty();
    }
  }

  private static Optional<Class<?>> getJavaClass(MethodSource methodSource) {
    try {
      return Optional.of(methodSource.getJavaClass());
    }
    catch (Exception ex) {
      // ignore exception
      return Optional.empty();
    }
  }

  private static TestClassScanner createTestClassScanner() {
    String classpathRoot = System.getProperty(CLASSPATH_ROOT);
    assertThat(classpathRoot).as(CLASSPATH_ROOT).isNotNull();
    Set<Path> classpathRoots = Set.of(Paths.get(classpathRoot));
    return new TestClassScanner(classpathRoots);
  }

}
