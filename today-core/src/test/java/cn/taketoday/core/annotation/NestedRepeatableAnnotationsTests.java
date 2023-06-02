/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.core.annotation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Set;

import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for various ways to search for repeatable annotations that are
 * nested (i.e., repeatable annotations used as meta-annotations on other
 * repeatable annotations).
 *
 * @author Sam Brannen
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/20279">issues/20279</a>
 */
@SuppressWarnings("unused")
class NestedRepeatableAnnotationsTests {

  @Nested
  class SingleRepeatableAnnotationTests {

    private final Method method = ReflectionUtils.findMethod(getClass(), "annotatedMethod");

    @Test
    void streamRepeatableAnnotations_MergedAnnotationsApi() {
      Set<A> annotations = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY)
              .stream(A.class).collect(MergedAnnotationCollectors.toAnnotationSet());
      // Merged, so we expect to find @A once with its value coming from @B(5).
      assertThat(annotations).extracting(A::value).containsExactly(5);
    }

    @Test
    void findMergedRepeatableAnnotations_AnnotatedElementUtils() {
      Set<A> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(method, A.class);
      // Merged, so we expect to find @A once with its value coming from @B(5).
      assertThat(annotations).extracting(A::value).containsExactly(5);
    }

    @Test
    void getMergedRepeatableAnnotationsWithStandardRepeatables_AnnotatedElementUtils() {
      Set<A> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, A.class);
      // Merged, so we expect to find @A once with its value coming from @B(5).
      assertThat(annotations).extracting(A::value).containsExactly(5);
    }

    @Test
    void getMergedRepeatableAnnotationsWithExplicitContainer_AnnotatedElementUtils() {
      Set<A> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, A.class, A.Container.class);
      // Merged, so we expect to find @A once with its value coming from @B(5).
      assertThat(annotations).extracting(A::value).containsExactly(5);
    }

    @Test
    @SuppressWarnings("deprecation")
    void getRepeatableAnnotations_AnnotationUtils() {
      Set<A> annotations = AnnotationUtils.getRepeatableAnnotations(method, A.class);
      // Not merged, so we expect to find @A once with the default value of 0.
      // @A will actually be found twice, but we have Set semantics here.
      assertThat(annotations).extracting(A::value).containsExactly(0);
    }

    @B(5)
    void annotatedMethod() {
    }

  }

  @Nested
  class MultipleRepeatableAnnotationsTests {

    private final Method method = ReflectionUtils.findMethod(getClass(), "annotatedMethod");

    @Test
    void streamRepeatableAnnotationsWithStandardRepeatables_MergedAnnotationsApi() {
      RepeatableContainers repeatableContainers = RepeatableContainers.standard();
      Set<A> annotations = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, repeatableContainers)
              .stream(A.class).collect(MergedAnnotationCollectors.toAnnotationSet());
      // Merged, so we expect to find @A twice with values coming from @B(5) and @B(10).
      assertThat(annotations).extracting(A::value).containsExactly(5, 10);
    }

    @Test
    void streamRepeatableAnnotationsWithExplicitRepeatables_MergedAnnotationsApi() {
      var repeatableContainers =
              RepeatableContainers.valueOf(A.class, A.Container.class)
                      .and(B.Container.class, B.class);
      Set<A> annotations = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, repeatableContainers)
              .stream(A.class).collect(MergedAnnotationCollectors.toAnnotationSet());
      // Merged, so we expect to find @A twice with values coming from @B(5) and @B(10).
      assertThat(annotations).extracting(A::value).containsExactly(5, 10);
    }

    @Test
    void findMergedRepeatableAnnotationsWithStandardRepeatables_AnnotatedElementUtils() {
      Set<A> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(method, A.class);
      // Merged, so we expect to find @A twice with values coming from @B(5) and @B(10).
      assertThat(annotations).extracting(A::value).containsExactly(5, 10);
    }

    @Test
    void findMergedRepeatableAnnotationsWithExplicitContainer_AnnotatedElementUtils() {
      Set<A> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(method, A.class, A.Container.class);
      // When findMergedRepeatableAnnotations(...) is invoked with an explicit container
      // type, it uses RepeatableContainers.of(...) which limits the repeatable annotation
      // support to a single container type.
      //
      // In this test case, we are therefore limiting the support to @A.Container, which
      // means that @B.Container is unsupported and effectively ignored as a repeatable
      // container type.
      //
      // Long story, short: the search doesn't find anything.
      assertThat(annotations).isEmpty();
    }

    @Test
    void getMergedRepeatableAnnotationsWithStandardRepeatables_AnnotatedElementUtils() {
      Set<A> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, A.class);
      // Merged, so we expect to find @A twice with values coming from @B(5) and @B(10).
      assertThat(annotations).extracting(A::value).containsExactly(5, 10);
    }

    @Test
    void getMergedRepeatableAnnotationsWithExplicitContainer_AnnotatedElementUtils() {
      Set<A> annotations = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, A.class, A.Container.class);
      // When getMergedRepeatableAnnotations(...) is invoked with an explicit container
      // type, it uses RepeatableContainers.of(...) which limits the repeatable annotation
      // support to a single container type.
      //
      // In this test case, we are therefore limiting the support to @A.Container, which
      // means that @B.Container is unsupported and effectively ignored as a repeatable
      // container type.
      //
      // Long story, short: the search doesn't find anything.
      assertThat(annotations).isEmpty();
    }

    @Test
    @SuppressWarnings("deprecation")
    void getRepeatableAnnotations_AnnotationUtils() {
      Set<A> annotations = AnnotationUtils.getRepeatableAnnotations(method, A.class);
      // Not merged, so we expect to find a single @A with default value of 0.
      // @A will actually be found twice, but we have Set semantics here.
      assertThat(annotations).extracting(A::value).containsExactly(0);
    }

    @B(5)
    @B(10)
    void annotatedMethod() {
    }

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
  @Repeatable(A.Container.class)
  @interface A {

    int value() default 0;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
    @interface Container {
      A[] value();
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
  @Repeatable(B.Container.class)
  @A
  @A
  @interface B {

    @AliasFor(annotation = A.class)
    int value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
    @interface Container {
      B[] value();
    }
  }

}
