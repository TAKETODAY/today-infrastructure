/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import org.junit.jupiter.api.Test;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link MergedAnnotationPredicates}.
 *
 * @author Phillip Webb
 */
class MergedAnnotationPredicatesTests {

  @Test
  void typeInStringArrayWhenNameMatchesAccepts() {
    MergedAnnotation<TestAnnotation> annotation = MergedAnnotations.from(
            WithTestAnnotation.class).get(TestAnnotation.class);
    assertThat(MergedAnnotationPredicates.typeIn(
            TestAnnotation.class.getName())).accepts(annotation);
  }

  @Test
  void typeInStringArrayWhenNameDoesNotMatchRejects() {
    MergedAnnotation<TestAnnotation> annotation = MergedAnnotations.from(
            WithTestAnnotation.class).get(TestAnnotation.class);
    assertThat(MergedAnnotationPredicates.typeIn(
            MissingAnnotation.class.getName())).rejects(annotation);
  }

  @Test
  void typeInClassArrayWhenNameMatchesAccepts() {
    MergedAnnotation<TestAnnotation> annotation =
            MergedAnnotations.from(WithTestAnnotation.class).get(TestAnnotation.class);
    assertThat(MergedAnnotationPredicates.typeIn(TestAnnotation.class)).accepts(annotation);
  }

  @Test
  void typeInClassArrayWhenNameDoesNotMatchRejects() {
    MergedAnnotation<TestAnnotation> annotation =
            MergedAnnotations.from(WithTestAnnotation.class).get(TestAnnotation.class);
    assertThat(MergedAnnotationPredicates.typeIn(MissingAnnotation.class)).rejects(annotation);
  }

  @Test
  void typeInCollectionWhenMatchesStringInCollectionAccepts() {
    MergedAnnotation<TestAnnotation> annotation = MergedAnnotations.from(
            WithTestAnnotation.class).get(TestAnnotation.class);
    assertThat(MergedAnnotationPredicates.typeIn(
            Collections.singleton(TestAnnotation.class.getName()))).accepts(annotation);
  }

  @Test
  void typeInCollectionWhenMatchesClassInCollectionAccepts() {
    MergedAnnotation<TestAnnotation> annotation = MergedAnnotations.from(
            WithTestAnnotation.class).get(TestAnnotation.class);
    assertThat(MergedAnnotationPredicates.typeIn(
            Collections.singleton(TestAnnotation.class))).accepts(annotation);
  }

  @Test
  void typeInCollectionWhenDoesNotMatchAnyRejects() {
    MergedAnnotation<TestAnnotation> annotation = MergedAnnotations.from(
            WithTestAnnotation.class).get(TestAnnotation.class);
    assertThat(MergedAnnotationPredicates.typeIn(Arrays.asList(
            MissingAnnotation.class.getName(), MissingAnnotation.class))).rejects(annotation);
  }

  @Test
  void firstRunOfAcceptsOnlyFirstRun() {
    List<MergedAnnotation<TestAnnotation>> filtered = MergedAnnotations.from(
            WithMultipleTestAnnotation.class).stream(TestAnnotation.class).filter(
            MergedAnnotationPredicates.firstRunOf(
                    this::firstCharOfValue)).collect(Collectors.toList());
    assertThat(filtered.stream().map(MergedAnnotation::getStringValue)).containsExactly("a1", "a2", "a3");
  }

  @Test
  void firstRunOfWhenValueExtractorIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            MergedAnnotationPredicates.firstRunOf(null));
  }

  @Test
  void uniqueAcceptsUniquely() {
    List<MergedAnnotation<TestAnnotation>> filtered = MergedAnnotations.from(
            WithMultipleTestAnnotation.class).stream(TestAnnotation.class).filter(
            MergedAnnotationPredicates.unique(
                    this::firstCharOfValue)).collect(Collectors.toList());
    assertThat(filtered.stream().map(MergedAnnotation::getStringValue)).containsExactly("a1", "b1", "c1");
  }

  @Test
  void uniqueWhenKeyExtractorIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            MergedAnnotationPredicates.unique(null));
  }

  private char firstCharOfValue(MergedAnnotation<TestAnnotation> annotation) {
    return annotation.getString("value").charAt(0);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(TestAnnotations.class)
  @interface TestAnnotation {

    String value() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface TestAnnotations {

    TestAnnotation[] value();
  }

  @interface MissingAnnotation {
  }

  @TestAnnotation("test")
  static class WithTestAnnotation {
  }

  @TestAnnotation("a1")
  @TestAnnotation("a2")
  @TestAnnotation("a3")
  @TestAnnotation("b1")
  @TestAnnotation("b2")
  @TestAnnotation("b3")
  @TestAnnotation("c1")
  @TestAnnotation("c2")
  @TestAnnotation("c3")
  static class WithMultipleTestAnnotation {
  }

}
