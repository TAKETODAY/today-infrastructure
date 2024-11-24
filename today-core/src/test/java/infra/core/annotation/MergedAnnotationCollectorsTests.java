/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import infra.core.annotation.MergedAnnotation.Adapt;
import infra.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MergedAnnotationCollectors}.
 *
 * @author Phillip Webb
 */
class MergedAnnotationCollectorsTests {

  @Test
  void toAnnotationSetCollectsLinkedHashSetWithSynthesizedAnnotations() {
    Set<TestAnnotation> set = stream().collect(
            MergedAnnotationCollectors.toAnnotationSet());
    assertThat(set).isInstanceOf(LinkedHashSet.class).flatExtracting(
            TestAnnotation::value).containsExactly("a", "b", "c");
    assertThat(set).allMatch(AnnotationUtils::isSynthesizedAnnotation);
  }

  @Test
  void toAnnotationArrayCollectsAnnotationArrayWithSynthesizedAnnotations() {
    Annotation[] array = stream().collect(
            MergedAnnotationCollectors.toAnnotationArray());
    assertThat(Arrays.stream(array).map(
            annotation -> ((TestAnnotation) annotation).value())).containsExactly("a",
            "b", "c");
    assertThat(array).allMatch(AnnotationUtils::isSynthesizedAnnotation);
  }

  @Test
  void toSuppliedAnnotationArrayCollectsAnnotationArrayWithSynthesizedAnnotations() {
    TestAnnotation[] array = stream().collect(
            MergedAnnotationCollectors.toAnnotationArray(TestAnnotation[]::new));
    assertThat(Arrays.stream(array).map(TestAnnotation::value)).containsExactly("a",
            "b", "c");
    assertThat(array).allMatch(AnnotationUtils::isSynthesizedAnnotation);
  }

  @Test
  void toMultiValueMapCollectsMultiValueMap() {
    MultiValueMap<String, Object> map = stream().map(
            MergedAnnotation::filterDefaultValues).collect(
            MergedAnnotationCollectors.toMultiValueMap(
                    Adapt.CLASS_TO_STRING));
    assertThat(map.get("value")).containsExactly("a", "b", "c");
    assertThat(map.get("extra")).containsExactly("java.lang.String",
            "java.lang.Integer");
  }

  @Test
  void toFinishedMultiValueMapCollectsMultiValueMap() {
    MultiValueMap<String, Object> map = stream().collect(
            MergedAnnotationCollectors.toMultiValueMap(result -> {
              result.add("finished", true);
              return result;
            }));
    assertThat(map.get("value")).containsExactly("a", "b", "c");
    assertThat(map.get("extra")).containsExactly(void.class, String.class,
            Integer.class);
    assertThat(map.get("finished")).containsExactly(true);
  }

  private Stream<MergedAnnotation<TestAnnotation>> stream() {
    return MergedAnnotations.from(WithTestAnnotations.class).stream(TestAnnotation.class);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(TestAnnotations.class)
  @interface TestAnnotation {

    @AliasFor("name")
    String value() default "";

    @AliasFor("value")
    String name() default "";

    Class<?> extra() default void.class;

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface TestAnnotations {

    TestAnnotation[] value();
  }

  @TestAnnotation("a")
  @TestAnnotation(name = "b", extra = String.class)
  @TestAnnotation(name = "c", extra = Integer.class)
  static class WithTestAnnotations {
  }

}
