/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.aspects;

import org.aspectj.lang.NoAspectBoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.stream.Stream;

import infra.aop.interceptor.AsyncExecutionAspectSupport;
import infra.cache.annotation.AnnotationCacheOperationSource;
import infra.cache.annotation.DefaultCacheAnnotationParser;
import infra.cache.aspectj.AnnotationCacheAspect;
import infra.cache.aspectj.JCacheCacheAspect;
import infra.cache.interceptor.AbstractCacheInvoker;
import infra.cache.jcache.interceptor.JCacheAspectSupport;
import infra.scheduling.aspectj.AbstractAsyncExecutionAspect;
import infra.scheduling.aspectj.AnnotationAsyncExecutionAspect;
import infra.transaction.annotation.AnnotationTransactionAttributeSource;
import infra.transaction.aspectj.AnnotationTransactionAspect;
import infra.transaction.aspectj.JtaAnnotationTransactionAspect;
import infra.util.CollectionUtils;
import infra.util.function.SingletonSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AspectInitializationTests {

  @ParameterizedTest
  @MethodSource("aspects")
  void reportsMissingDependencyWhenAspectCannotInitialize(AspectClasspath classpath) throws Exception {
    Class<?> aspect = classpath.aspect();
    assertThat(aspect.getMethod("hasAspect").invoke(null)).isEqualTo(true);

    URL[] locations = Stream.concat(Stream.of(aspect), classpath.isolatedClasses().stream())
            .map(type -> type.getProtectionDomain().getCodeSource().getLocation())
            .distinct()
            .toArray(URL[]::new);

    // Isolate the singleton while preserving class file locations for coverage instrumentation.
    try (var loader = new URLClassLoader(locations, aspect.getClassLoader()) {
      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.equals(classpath.missingDependency().getName())) {
          throw new ClassNotFoundException(name);
        }
        if (name.equals(aspect.getName()) || classpath.isolatedClasses().stream()
                .anyMatch(type -> name.equals(type.getName()))) {
          synchronized (getClassLoadingLock(name)) {
            Class<?> type = findLoadedClass(name);
            if (type == null) {
              type = findClass(name);
            }
            if (resolve) {
              resolveClass(type);
            }
            return type;
          }
        }
        return super.loadClass(name, resolve);
      }
    }) {
      Class<?> isolatedAspect = Class.forName(aspect.getName(), true, loader);
      assertThat(isolatedAspect.getMethod("hasAspect").invoke(null)).isEqualTo(false);
      assertThatExceptionOfType(InvocationTargetException.class)
              .isThrownBy(() -> isolatedAspect.getMethod("aspectOf").invoke(null))
              .withCauseInstanceOf(NoAspectBoundException.class)
              .withRootCauseExactlyInstanceOf(ClassNotFoundException.class)
              .withStackTraceContaining(classpath.missingDependency().getName());
    }
  }

  static Stream<AspectClasspath> aspects() {
    return Stream.of(
            new AspectClasspath(AnnotationAsyncExecutionAspect.class, SingletonSupplier.class,
                    Set.of(AbstractAsyncExecutionAspect.class, AsyncExecutionAspectSupport.class)),
            new AspectClasspath(AnnotationCacheAspect.class, DefaultCacheAnnotationParser.class,
                    Set.of(AnnotationCacheOperationSource.class)),
            new AspectClasspath(JCacheCacheAspect.class, SingletonSupplier.class,
                    Set.of(JCacheAspectSupport.class, AbstractCacheInvoker.class)),
            new AspectClasspath(AnnotationTransactionAspect.class, CollectionUtils.class,
                    Set.of(AnnotationTransactionAttributeSource.class)),
            new AspectClasspath(JtaAnnotationTransactionAspect.class, CollectionUtils.class,
                    Set.of(AnnotationTransactionAttributeSource.class)));
  }

  record AspectClasspath(Class<?> aspect, Class<?> missingDependency, Set<Class<?>> isolatedClasses) {

    @Override
    public String toString() {
      return this.aspect.getSimpleName();
    }
  }
}
