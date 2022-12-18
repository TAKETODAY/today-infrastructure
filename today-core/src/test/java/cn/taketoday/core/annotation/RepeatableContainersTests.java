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

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

import static cn.taketoday.core.annotation.AnnotatedElementUtilsTests.StandardContainerWithMultipleAttributes;
import static cn.taketoday.core.annotation.AnnotatedElementUtilsTests.StandardRepeatablesWithContainerWithMultipleAttributesTestCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RepeatableContainers}.
 *
 * @author Phillip Webb
 */
class RepeatableContainersTests {

  @Nested
  class StandardRepeatableContainersTests {

    @Test
    void standardRepeatablesWhenNonRepeatableReturnsNull() {
      Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standard(),
              NonRepeatableTestCase.class, NonRepeatable.class);
      assertThat(values).isNull();
    }

    @Test
    void standardRepeatablesWhenSingleReturnsNull() {
      Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standard(),
              SingleStandardRepeatableTestCase.class, StandardRepeatable.class);
      assertThat(values).isNull();
    }

    @Test
    void standardRepeatablesWhenContainerButNotRepeatableReturnsNull() {
      Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standard(),
              ExplicitRepeatablesTestCase.class, ExplicitContainer.class);
      assertThat(values).isNull();
    }

    @Test
    void standardRepeatablesWhenContainerReturnsRepeats() {
      Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standard(),
              StandardRepeatablesTestCase.class, StandardContainer.class);
      assertThat(values).containsExactly("a", "b");
    }

    @Test
    void standardRepeatablesWithContainerWithMultipleAttributes() {
      Object[] values = findRepeatedAnnotationValues(RepeatableContainers.standard(),
              StandardRepeatablesWithContainerWithMultipleAttributesTestCase.class,
              StandardContainerWithMultipleAttributes.class);
      assertThat(values).containsExactly("a", "b");
    }

  }

  @Nested
  class ExplicitRepeatableContainerTests {

    @Test
    void ofExplicitWhenNonRepeatableReturnsNull() {
      Object[] values = findRepeatedAnnotationValues(
              RepeatableContainers.valueOf(ExplicitRepeatable.class, ExplicitContainer.class),
              NonRepeatableTestCase.class, NonRepeatable.class);
      assertThat(values).isNull();
    }

    @Test
    void ofExplicitWhenStandardRepeatableContainerReturnsNull() {
      Object[] values = findRepeatedAnnotationValues(
              RepeatableContainers.valueOf(ExplicitRepeatable.class, ExplicitContainer.class),
              StandardRepeatablesTestCase.class, StandardContainer.class);
      assertThat(values).isNull();
    }

    @Test
    void ofExplicitWhenContainerReturnsRepeats() {
      Object[] values = findRepeatedAnnotationValues(
              RepeatableContainers.valueOf(ExplicitRepeatable.class, ExplicitContainer.class),
              ExplicitRepeatablesTestCase.class, ExplicitContainer.class);
      assertThat(values).containsExactly("a", "b");
    }

    @Test
    void ofExplicitWhenContainerIsNullDeducesContainer() {
      Object[] values = findRepeatedAnnotationValues(RepeatableContainers.valueOf(StandardRepeatable.class, null),
              StandardRepeatablesTestCase.class, StandardContainer.class);
      assertThat(values).containsExactly("a", "b");
    }

    @Test
    void ofExplicitWhenHasNoValueThrowsException() {
      assertThatExceptionOfType(AnnotationConfigurationException.class)
              .isThrownBy(() -> RepeatableContainers.valueOf(ExplicitRepeatable.class, InvalidNoValue.class))
              .withMessageContaining("Invalid declaration of container type [%s] for repeatable annotation [%s]",
                      InvalidNoValue.class.getName(), ExplicitRepeatable.class.getName());
    }

    @Test
    void ofExplicitWhenValueIsNotArrayThrowsException() {
      assertThatExceptionOfType(AnnotationConfigurationException.class)
              .isThrownBy(() -> RepeatableContainers.valueOf(ExplicitRepeatable.class, InvalidNotArray.class))
              .withMessage("Container type [%s] must declare a 'value' attribute for an array of type [%s]",
                      InvalidNotArray.class.getName(), ExplicitRepeatable.class.getName());
    }

    @Test
    void ofExplicitWhenValueIsArrayOfWrongTypeThrowsException() {
      assertThatExceptionOfType(AnnotationConfigurationException.class)
              .isThrownBy(() -> RepeatableContainers.valueOf(ExplicitRepeatable.class, InvalidWrongArrayType.class))
              .withMessage("Container type [%s] must declare a 'value' attribute for an array of type [%s]",
                      InvalidWrongArrayType.class.getName(), ExplicitRepeatable.class.getName());
    }

    @Test
    void ofExplicitWhenAnnotationIsNullThrowsException() {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> RepeatableContainers.valueOf(null, null))
              .withMessage("Repeatable must not be null");
    }

    @Test
    void ofExplicitWhenContainerIsNullAndNotRepeatableThrowsException() {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> RepeatableContainers.valueOf(ExplicitRepeatable.class, null))
              .withMessage("Annotation type must be a repeatable annotation: failed to resolve container type for %s",
                      ExplicitRepeatable.class.getName());
    }

  }

  @Test
  void standardAndExplicitReturnsRepeats() {
    RepeatableContainers repeatableContainers = RepeatableContainers.standard()
            .and(ExplicitContainer.class, ExplicitRepeatable.class);
    assertThat(findRepeatedAnnotationValues(repeatableContainers, StandardRepeatablesTestCase.class, StandardContainer.class))
            .containsExactly("a", "b");
    assertThat(findRepeatedAnnotationValues(repeatableContainers, ExplicitRepeatablesTestCase.class, ExplicitContainer.class))
            .containsExactly("a", "b");
  }

  @Test
  void noneAlwaysReturnsNull() {
    Object[] values = findRepeatedAnnotationValues(RepeatableContainers.none(), StandardRepeatablesTestCase.class,
            StandardContainer.class);
    assertThat(values).isNull();
  }

  @Test
  void equalsAndHashcode() {
    RepeatableContainers c1 = RepeatableContainers.valueOf(ExplicitRepeatable.class, ExplicitContainer.class);
    RepeatableContainers c2 = RepeatableContainers.valueOf(ExplicitRepeatable.class, ExplicitContainer.class);
    RepeatableContainers c3 = RepeatableContainers.standard();
    RepeatableContainers c4 = RepeatableContainers.standard().and(ExplicitContainer.class, ExplicitRepeatable.class);
    assertThat(c1).hasSameHashCodeAs(c2);
    assertThat(c1).isEqualTo(c1).isEqualTo(c2);
    assertThat(c1).isNotEqualTo(c3).isNotEqualTo(c4);
  }

  private static Object[] findRepeatedAnnotationValues(RepeatableContainers containers,
          Class<?> element, Class<? extends Annotation> annotationType) {
    Annotation[] annotations = containers.findRepeatedAnnotations(element.getAnnotation(annotationType));
    return extractValues(annotations);
  }

  private static Object[] extractValues(Annotation[] annotations) {
    if (annotations == null) {
      return null;
    }
    return Arrays.stream(annotations).map(AnnotationUtils::getValue).toArray(Object[]::new);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface NonRepeatable {

    String value() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface StandardContainer {

    StandardRepeatable[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(StandardContainer.class)
  @interface StandardRepeatable {

    String value() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ExplicitContainer {

    ExplicitRepeatable[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ExplicitRepeatable {

    String value() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface InvalidNoValue {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface InvalidNotArray {

    int value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface InvalidWrongArrayType {

    StandardRepeatable[] value();
  }

  @NonRepeatable("a")
  static class NonRepeatableTestCase {
  }

  @StandardRepeatable("a")
  static class SingleStandardRepeatableTestCase {
  }

  @StandardRepeatable("a")
  @StandardRepeatable("b")
  static class StandardRepeatablesTestCase {
  }

  @ExplicitContainer({ @ExplicitRepeatable("a"), @ExplicitRepeatable("b") })
  static class ExplicitRepeatablesTestCase {
  }

}
