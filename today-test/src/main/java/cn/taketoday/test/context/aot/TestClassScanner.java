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

import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.BootstrapWith;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.util.ClassUtils;

import static cn.taketoday.core.annotation.MergedAnnotation.VALUE;
import static cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy.INHERITED_ANNOTATIONS;
import static cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClasspathRoots;
import static org.junit.platform.engine.discovery.PackageNameFilter.includePackageNames;

/**
 * {@code TestClassScanner} scans provided classpath roots for Infra integration
 * test classes using the JUnit Platform {@link Launcher} API which allows all
 * registered {@link org.junit.platform.engine.TestEngine TestEngines} to discover
 * tests according to their own rules.
 *
 * <p>The scanner currently detects the following categories of Infra integration
 * test classes.
 *
 * <ul>
 * <li>JUnit Jupiter: classes that register the {@code InfraExtension} via
 * {@code @ExtendWith}.</li>
 * <li>JUnit 4: classes that register the {@code InfraJUnit4ClassRunner} or
 * {@code InfraRunner} via {@code @RunWith}.</li>
 * <li>Generic: classes that are annotated with {@code @ContextConfiguration} or
 * {@code @BootstrapWith}.</li>
 * </ul>
 *
 * <p>The scanner has been tested with the following
 * {@link org.junit.platform.engine.TestEngine TestEngines}.
 *
 * <ul>
 * <li>JUnit Jupiter</li>
 * <li>JUnit Vintage</li>
 * <li>JUnit Platform Suite Engine</li>
 * <li>TestNG Engine for the JUnit Platform</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TestClassScanner {

  // JUnit Jupiter
  private static final String EXTEND_WITH_ANNOTATION_NAME = "org.junit.jupiter.api.extension.ExtendWith";
  private static final String SPRING_EXTENSION_NAME = "cn.taketoday.test.context.junit.jupiter.InfraExtension";

  // JUnit 4
  private static final String RUN_WITH_ANNOTATION_NAME = "org.junit.runner.RunWith";
  private static final String SPRING_JUNIT4_CLASS_RUNNER_NAME = "cn.taketoday.test.context.junit4.InfraJUnit4ClassRunner";
  private static final String SPRING_RUNNER_NAME = "cn.taketoday.test.context.junit4.InfraRunner";

  private final Logger logger = LoggerFactory.getLogger(TestClassScanner.class);

  private final Set<Path> classpathRoots;

  /**
   * Create a {@code TestClassScanner} for the given classpath roots.
   * <p>For example, in a Gradle project that only supports Java-based tests,
   * the supplied set would contain a single {@link Path} representing the
   * absolute path to the project's {@code build/classes/java/test} folder.
   *
   * @param classpathRoots the classpath roots to scan
   */
  TestClassScanner(Set<Path> classpathRoots) {
    this.classpathRoots = assertPreconditions(classpathRoots);
  }

  /**
   * Scan the configured classpath roots for Infra integration test classes.
   */
  Stream<Class<?>> scan() {
    return scan(new String[0]);
  }

  /**
   * Scan the configured classpath roots for Infra integration test classes
   * in the given packages.
   * <p>This method is currently only intended to be used within our own test
   * suite to validate the behavior of this scanner with a limited scope. In
   * production scenarios one should invoke {@link #scan()} to scan all packages
   * in the configured classpath roots.
   */
  Stream<Class<?>> scan(String... packageNames) {
    Assert.noNullElements(packageNames, "'packageNames' must not contain null elements");

    if (logger.isInfoEnabled()) {
      if (packageNames.length > 0) {
        logger.info("Scanning for Infra test classes in packages %s in classpath roots %s"
                .formatted(Arrays.toString(packageNames), this.classpathRoots));
      }
      else {
        logger.info("Scanning for Infra test classes in all packages in classpath roots %s"
                .formatted(this.classpathRoots));
      }
    }

    LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request();
    builder.selectors(selectClasspathRoots(this.classpathRoots));
    if (packageNames.length > 0) {
      builder.filters(includePackageNames(packageNames));
    }
    LauncherDiscoveryRequest request = builder.build();
    Launcher launcher = LauncherFactory.create();
    TestPlan testPlan = launcher.discover(request);

    return testPlan.getRoots().stream()
            .map(testPlan::getDescendants)
            .flatMap(Set::stream)
            .map(TestIdentifier::getSource)
            .flatMap(Optional::stream)
            .filter(ClassSource.class::isInstance)
            .map(ClassSource.class::cast)
            .map(this::getJavaClass)
            .flatMap(Optional::stream)
            .filter(this::isInfraTestClass)
            .distinct()
            .sorted(Comparator.comparing(Class::getName));
  }

  private Optional<Class<?>> getJavaClass(ClassSource classSource) {
    try {
      return Optional.of(classSource.getJavaClass());
    }
    catch (Exception ex) {
      // ignore exception
      return Optional.empty();
    }
  }

  private boolean isInfraTestClass(Class<?> clazz) {
    boolean isInfraTestClass = (isJupiterInfraTestClass(clazz) || isJUnit4InfraTestClass(clazz) ||
            isGenericInfraTestClass(clazz));
    if (isInfraTestClass && logger.isTraceEnabled()) {
      logger.trace("Found Infra test class: " + clazz.getName());
    }
    return isInfraTestClass;
  }

  private static boolean isJupiterInfraTestClass(Class<?> clazz) {
    return MergedAnnotations.search(TYPE_HIERARCHY)
            .withEnclosingClasses(ClassUtils::isInnerClass)
            .from(clazz)
            .stream(EXTEND_WITH_ANNOTATION_NAME)
            .map(annotation -> annotation.getClassArray(VALUE))
            .flatMap(Arrays::stream)
            .map(Class::getName)
            .anyMatch(SPRING_EXTENSION_NAME::equals);
  }

  private static boolean isJUnit4InfraTestClass(Class<?> clazz) {
    MergedAnnotation<Annotation> mergedAnnotation =
            MergedAnnotations.from(clazz, INHERITED_ANNOTATIONS).get(RUN_WITH_ANNOTATION_NAME);
    if (mergedAnnotation.isPresent()) {
      String name = mergedAnnotation.getClass(VALUE).getName();
      return (SPRING_JUNIT4_CLASS_RUNNER_NAME.equals(name) || SPRING_RUNNER_NAME.equals(name));
    }
    return false;
  }

  private static boolean isGenericInfraTestClass(Class<?> clazz) {
    MergedAnnotations mergedAnnotations = MergedAnnotations.from(clazz, TYPE_HIERARCHY);
    return (mergedAnnotations.isPresent(ContextConfiguration.class) ||
            mergedAnnotations.isPresent(BootstrapWith.class));
  }

  private static Set<Path> assertPreconditions(Set<Path> classpathRoots) {
    Assert.notEmpty(classpathRoots, "'classpathRoots' must not be null or empty");
    Assert.noNullElements(classpathRoots, "'classpathRoots' must not contain null elements");
    classpathRoots.forEach(classpathRoot -> Assert.isTrue(Files.exists(classpathRoot),
            () -> "Classpath root [%s] does not exist".formatted(classpathRoot)));
    return classpathRoots;
  }

}
