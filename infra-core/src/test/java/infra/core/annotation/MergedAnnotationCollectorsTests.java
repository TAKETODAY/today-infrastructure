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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.core.annotation.MergedAnnotation.Adapt;
import infra.util.LinkedMultiValueMap;
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

  @Test
  void toAnnotationSetReturnsEmptySetWhenStreamIsEmpty() {
    Set<TestAnnotation> set = MergedAnnotations.from(Object.class)
            .stream(TestAnnotation.class)
            .collect(MergedAnnotationCollectors.toAnnotationSet());
    assertThat(set).isEmpty();
  }

  @Test
  void toAnnotationSetPreservesOrder() {
    Set<TestAnnotation> set = stream().collect(MergedAnnotationCollectors.toAnnotationSet());
    assertThat(set).hasSize(3);
    assertThat(new ArrayList<>(set)).extracting(TestAnnotation::value)
            .containsExactly("a", "b", "c");
  }

  @Test
  void toAnnotationArrayReturnsEmptyArrayWhenStreamIsEmpty() {
    Annotation[] array = MergedAnnotations.from(Object.class)
            .stream(TestAnnotation.class)
            .collect(MergedAnnotationCollectors.toAnnotationArray());
    assertThat(array).isEmpty();
  }

  @Test
  void toAnnotationArrayWithGeneratorReturnsEmptyArrayWhenStreamIsEmpty() {
    TestAnnotation[] array = MergedAnnotations.from(Object.class)
            .stream(TestAnnotation.class)
            .collect(MergedAnnotationCollectors.toAnnotationArray(TestAnnotation[]::new));
    assertThat(array).isEmpty();
  }

  @Test
  void toAnnotationArrayWithGeneratorReturnsCorrectTypeAndContent() {
    TestAnnotation[] array = stream()
            .collect(MergedAnnotationCollectors.toAnnotationArray(TestAnnotation[]::new));
    assertThat(array).isInstanceOf(TestAnnotation[].class);
    assertThat(array).hasSize(3);
    assertThat(array).extracting(TestAnnotation::value)
            .containsExactly("a", "b", "c");
  }

  @Test
  void toMultiValueMapReturnsEmptyMapWhenStreamIsEmpty() {
    MultiValueMap<String, Object> map = MergedAnnotations.from(Object.class)
            .stream(TestAnnotation.class)
            .collect(MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map).isEmpty();
  }

  @Test
  void toMultiValueMapWithAdaptations() {
    MultiValueMap<String, Object> map = stream()
            .collect(MergedAnnotationCollectors.toMultiValueMap(Adapt.CLASS_TO_STRING));
    assertThat(map.get("value")).containsExactly("a", "b", "c");
    assertThat(map.get("extra")).containsExactly("void", "java.lang.String", "java.lang.Integer");
  }

  @Test
  void toMultiValueMapWithIdentityFinisher() {
    MultiValueMap<String, Object> map = stream()
            .collect(MergedAnnotationCollectors.toMultiValueMap(Function.identity()));
    assertThat(map.get("value")).containsExactly("a", "b", "c");
    assertThat(map.get("extra")).containsExactly(void.class, String.class, Integer.class);
  }

  @Test
  void toMultiValueMapWithCustomFinisherThatAddsData() {
    MultiValueMap<String, Object> map = stream()
            .collect(MergedAnnotationCollectors.toMultiValueMap(result -> {
              result.add("count", result.size());
              return result;
            }));
    assertThat(map.get("value")).containsExactly("a", "b", "c");
    assertThat(map.get("extra")).containsExactly(void.class, String.class, Integer.class);
    assertThat(map.get("count")).containsExactly(3);
  }

  @Test
  void toMultiValueMapWithNoAdaptations() {
    MultiValueMap<String, Object> map = stream()
            .collect(MergedAnnotationCollectors.toMultiValueMap());
    assertThat(map.get("value")).containsExactly("a", "b", "c");
    assertThat(map.get("extra")).containsExactly(void.class, String.class, Integer.class);
  }

  @Test
  void combinerMethodCombinesCollections() {
    ArrayList<String> list1 = new ArrayList<>();
    list1.add("a");
    list1.add("b");

    ArrayList<String> list2 = new ArrayList<>();
    list2.add("c");
    list2.add("d");

    ArrayList<String> combined = MergedAnnotationCollectors.combiner(list1, list2);
    assertThat(combined).containsExactly("a", "b", "c", "d");
    assertThat(combined).isSameAs(list1);
  }

  @Test
  void combinerMethodCombinesMultiValueMaps() {
    LinkedMultiValueMap<String, Object> map1 = new LinkedMultiValueMap<>();
    map1.add("key1", "value1");
    map1.add("key2", "value2");

    LinkedMultiValueMap<String, Object> map2 = new LinkedMultiValueMap<>();
    map2.add("key2", "value3");
    map2.add("key3", "value4");

    MultiValueMap<String, Object> combined = MergedAnnotationCollectors.combiner(map1, map2);
    assertThat(combined.get("key1")).containsExactly("value1");
    assertThat(combined.get("key2")).containsExactly("value2", "value3");
    assertThat(combined.get("key3")).containsExactly("value4");
    assertThat(combined).isSameAs(map1);
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
