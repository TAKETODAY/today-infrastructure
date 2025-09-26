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

package infra.context.aot;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.StreamSupport;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.annotation.Reflective;
import infra.aot.hint.annotation.ReflectiveProcessor;
import infra.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import infra.aot.hint.annotation.RegisterReflection;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.context.annotation.ClassPathScanningCandidateComponentProvider;
import infra.util.ClassUtils;

/**
 * Builder for an {@linkplain BeanFactoryInitializationAotContribution AOT
 * contribution} that detects the presence of {@link Reflective @Reflective} on
 * annotated elements and invoke the underlying {@link ReflectiveProcessor}
 * implementations.
 *
 * <p>Candidates can be provided explicitly or by scanning the classpath.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see Reflective
 * @see RegisterReflection
 * @since 5.0
 */
public class ReflectiveProcessorAotContributionBuilder {

  private static final ReflectiveRuntimeHintsRegistrar registrar = new ReflectiveRuntimeHintsRegistrar();

  private final Set<Class<?>> classes = new LinkedHashSet<>();

  /**
   * Process the given classes by checking the ones that use {@link Reflective}.
   * <p>A class is candidate if it uses {@link Reflective} directly or via a
   * meta-annotation. Type, fields, constructors, methods and enclosed types
   * are inspected.
   *
   * @param classes the classes to inspect
   */
  public ReflectiveProcessorAotContributionBuilder withClasses(Iterable<Class<?>> classes) {
    this.classes.addAll(StreamSupport.stream(classes.spliterator(), false)
            .filter(registrar::isCandidate).toList());
    return this;
  }

  /**
   * Process the given classes by checking the ones that use {@link Reflective}.
   * <p>A class is candidate if it uses {@link Reflective} directly or via a
   * meta-annotation. Type, fields, constructors, methods and enclosed types
   * are inspected.
   *
   * @param classes the classes to inspect
   */
  public ReflectiveProcessorAotContributionBuilder withClasses(Class<?>[] classes) {
    return withClasses(Arrays.asList(classes));
  }

  /**
   * Scan the given {@code packageNames} and their sub-packages for classes
   * that uses {@link Reflective}.
   * <p>This performs a "deep scan" by loading every class in the specified
   * packages and search for {@link Reflective} on types, constructors, methods,
   * and fields. Enclosed classes are candidates as well. Classes that fail to
   * load are ignored.
   *
   * @param classLoader the classloader to use
   * @param packageNames the package names to scan
   */
  public ReflectiveProcessorAotContributionBuilder scan(@Nullable ClassLoader classLoader, String... packageNames) {
    var scanner = new ReflectiveClassPathScanner(classLoader);
    return withClasses(scanner.scan(packageNames));
  }

  @Nullable
  public BeanFactoryInitializationAotContribution build() {
    return (!this.classes.isEmpty() ? new AotContribution(this.classes) : null);
  }

  private static class AotContribution implements BeanFactoryInitializationAotContribution {

    private final Class<?>[] classes;

    public AotContribution(Set<Class<?>> classes) {
      this.classes = classes.toArray(Class<?>[]::new);
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
      RuntimeHints runtimeHints = generationContext.getRuntimeHints();
      registrar.registerRuntimeHints(runtimeHints, this.classes);
    }

  }

  private static class ReflectiveClassPathScanner extends ClassPathScanningCandidateComponentProvider {

    @Nullable
    private final ClassLoader classLoader;

    ReflectiveClassPathScanner(@Nullable ClassLoader classLoader) {
      super(false);
      this.classLoader = classLoader;
      addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
      setCandidateComponentPredicate(metadata -> {
        String className = metadata.getClassName();
        try {
          Class<?> type = ClassUtils.forName(className, this.classLoader);
          return registrar.isCandidate(type);
        }
        catch (Exception ex) {
          if (logger.isTraceEnabled()) {
            logger.trace("Ignoring '%s' for reflective usage: %s".formatted(className, ex.getMessage()));
          }
        }
        return false;
      });
    }

    Set<Class<?>> scan(String... packageNames) {
      if (logger.isDebugEnabled()) {
        logger.debug("Scanning all types for reflective usage from {}", (Object) packageNames);
      }
      Set<Class<?>> candidates = new HashSet<>();
      for (String packageName : packageNames) {
        try {
          scanCandidateComponents(packageName, (reader, factory) -> {
            String className = reader.getClassMetadata().getClassName();
            Class<?> type = ClassUtils.resolveClassName(className, classLoader);
            candidates.add(type);
          });
        }
        catch (IOException e) {
          throw new IllegalStateException("Failed to scan " + packageName, e);
        }
      }
      return candidates;
    }

  }

}
