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

package infra.aop.support.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Test;

import infra.aop.MethodMatcher;
import infra.aop.Pointcut;
import infra.beans.factory.annotation.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AnnotationMatchingPointcut}.
 *
 * @author Sam Brannen
 */
class AnnotationMatchingPointcutTests {

  @Test
  void classPointcutMatchesDirectAndComposedAnnotations() {
    var direct = AnnotationMatchingPointcut.forClassAnnotation(Marker.class).getClassFilter();
    var inherited = new AnnotationClassFilter(Marker.class, true);

    assertThat(direct.matches(Service.class)).isTrue();
    assertThat(direct.matches(ComposedService.class)).isFalse();
    assertThat(inherited.matches(ComposedService.class)).isTrue();
    assertThat(inherited.matches(ServiceContract.class)).isFalse();
    assertThat(inherited.equals(inherited)).isTrue();
    assertThat(inherited).isNotEqualTo("not a class filter");
  }

  @Test
  void methodPointcutResolvesAnnotationsOnTargetImplementation() throws Exception {
    var pointcut = AnnotationMatchingPointcut.forMethodAnnotation(Marker.class);
    Method run = ServiceContract.class.getMethod("run");
    Method directlyAnnotated = Service.class.getMethod("run");
    MethodMatcher direct = pointcut.getMethodMatcher();
    MethodMatcher inherited = new AnnotationMethodMatcher(Marker.class, true);
    Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { ServiceContract.class }, (instance, method, args) -> null);

    assertThat(pointcut.getClassFilter().matches(Service.class)).isTrue();
    assertThat(direct.matches(directlyAnnotated, Service.class)).isTrue();
    assertThat(direct.matches(run, Service.class)).isTrue();
    assertThat(new AnnotationMethodMatcher(Marker.class).matches(run, Service.class)).isTrue();
    assertThat(direct.matches(run, ComposedService.class)).isFalse();
    assertThat(inherited.matches(run, ComposedService.class)).isTrue();
    assertThat(direct.matches(run, proxy.getClass())).isFalse();
    assertThat(direct.equals(direct)).isTrue();
    assertThat(direct).isNotEqualTo("not a method matcher");
    assertThat(pointcut.equals(pointcut)).isTrue();
    assertThat(pointcut).isNotEqualTo("not a pointcut");
    assertThat(pointcut.getClassFilter().equals(pointcut.getClassFilter())).isTrue();
    assertThat(pointcut.getClassFilter()).isNotEqualTo("not a candidate class filter");
  }

  @Test
  void classLevelPointcuts() {
    Pointcut pointcut1 = new AnnotationMatchingPointcut(Qualifier.class, true);
    Pointcut pointcut2 = new AnnotationMatchingPointcut(Qualifier.class, true);
    Pointcut pointcut3 = new AnnotationMatchingPointcut(Qualifier.class);

    assertThat(pointcut1.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut2.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut3.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

    assertThat(pointcut1.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);
    assertThat(pointcut2.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);
    assertThat(pointcut3.getMethodMatcher()).isEqualTo(MethodMatcher.TRUE);

    assertThat(pointcut1).isEqualTo(pointcut2);
    assertThat(pointcut1).isNotEqualTo(pointcut3);
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
    // #1 and #3 have equivalent hash codes even though equals() returns false.
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
    assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
  }

  @Test
  void methodLevelPointcuts() {
    Pointcut pointcut1 = new AnnotationMatchingPointcut(null, Qualifier.class, true);
    Pointcut pointcut2 = new AnnotationMatchingPointcut(null, Qualifier.class, true);
    Pointcut pointcut3 = new AnnotationMatchingPointcut(null, Qualifier.class);

    assertThat(pointcut1.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
    assertThat(pointcut2.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
    assertThat(pointcut3.getClassFilter().getClass().getSimpleName()).isEqualTo("AnnotationCandidateClassFilter");
    assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

    assertThat(pointcut1.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut2.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut3.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);

    assertThat(pointcut1).isEqualTo(pointcut2);
    assertThat(pointcut1).isNotEqualTo(pointcut3);
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
    // #1 and #3 have equivalent hash codes even though equals() returns false.
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
    assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
  }

  @Test
  void classLevelAndMethodLevelPointcuts() {
    Pointcut pointcut1 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class, true);
    Pointcut pointcut2 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class, true);
    Pointcut pointcut3 = new AnnotationMatchingPointcut(Qualifier.class, Qualifier.class);

    assertThat(pointcut1.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut2.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut3.getClassFilter().getClass()).isEqualTo(AnnotationClassFilter.class);
    assertThat(pointcut1.getClassFilter().toString()).contains(Qualifier.class.getName());

    assertThat(pointcut1.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut2.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut3.getMethodMatcher().getClass()).isEqualTo(AnnotationMethodMatcher.class);
    assertThat(pointcut1.getMethodMatcher().toString()).contains(Qualifier.class.getName());

    assertThat(pointcut1).isEqualTo(pointcut2);
    assertThat(pointcut1).isNotEqualTo(pointcut3);
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut2.hashCode());
    // #1 and #3 have equivalent hash codes even though equals() returns false.
    assertThat(pointcut1.hashCode()).isEqualTo(pointcut3.hashCode());
    assertThat(pointcut1.toString()).isEqualTo(pointcut2.toString());
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Marker {
  }

  @Marker
  @Retention(RetentionPolicy.RUNTIME)
  @interface ComposedMarker {
  }

  interface ServiceContract {

    void run();
  }

  @Marker
  static class Service implements ServiceContract {

    @Override
    @Marker
    public void run() {
    }
  }

  @ComposedMarker
  static class ComposedService implements ServiceContract {

    @Override
    @ComposedMarker
    public void run() {
    }
  }

}
