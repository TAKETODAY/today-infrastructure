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

import infra.aot.AotDetector;
import infra.aot.generate.GeneratedFiles.Kind;
import infra.aot.generate.InMemoryGeneratedFiles;
import infra.aot.hint.RuntimeHints;
import infra.aot.test.generate.CompilerFiles;
import infra.core.test.tools.CompileWithForkedClassLoader;
import infra.core.test.tools.TestCompiler;
import infra.test.context.aot.samples.basic.BasicInfraJupiterImportedConfigTests;
import infra.test.context.aot.samples.basic.BasicInfraJupiterSharedConfigTests;
import infra.test.context.aot.samples.basic.BasicInfraJupiterTests;
import infra.test.context.aot.samples.basic.BasicInfraVintageTests;
import infra.test.context.aot.samples.basic.DisabledInAotProcessingTests;
import infra.test.context.aot.samples.basic.DisabledInAotRuntimeClassLevelTests;
import infra.test.context.aot.samples.basic.DisabledInAotRuntimeMethodLevelTests;

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
    Stream<Class<?>> testClasses = createTestClassScanner().scan("infra.test.context.aot.samples.basic");

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
                    // .scan("infra.test.context.junit.jupiter")
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
