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

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link RepeatableContainers}.
 *
 * @author Phillip Webb
 */
class RepeatableContainersTests {

  @Test
  void standardRepeatablesWhenNonRepeatableReturnsNull() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.standard(), WithNonRepeatable.class,
            NonRepeatable.class);
    assertThat(values).isNull();
  }

  @Test
  void standardRepeatablesWhenSingleReturnsNull() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.standard(),
            WithSingleStandardRepeatable.class, StandardRepeatable.class);
    assertThat(values).isNull();
  }

  @Test
  void standardRepeatablesWhenContainerReturnsRepeats() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.standard(), WithStandardRepeatables.class,
            StandardContainer.class);
    assertThat(values).containsExactly("a", "b");
  }

  @Test
  void standardRepeatablesWhenContainerButNotRepeatableReturnsNull() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.standard(), WithExplicitRepeatables.class,
            ExplicitContainer.class);
    assertThat(values).isNull();
  }

  @Test
  void ofExplicitWhenNonRepeatableReturnsNull() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.valueOf(ExplicitRepeatable.class,
                    ExplicitContainer.class),
            WithNonRepeatable.class, NonRepeatable.class);
    assertThat(values).isNull();
  }

  @Test
  void ofExplicitWhenStandardRepeatableContainerReturnsNull() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.valueOf(ExplicitRepeatable.class,
                    ExplicitContainer.class),
            WithStandardRepeatables.class, StandardContainer.class);
    assertThat(values).isNull();
  }

  @Test
  void ofExplicitWhenContainerReturnsRepeats() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.valueOf(ExplicitRepeatable.class,
                    ExplicitContainer.class),
            WithExplicitRepeatables.class, ExplicitContainer.class);
    assertThat(values).containsExactly("a", "b");
  }

  @Test
  void ofExplicitWhenHasNoValueThrowsException() {
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    RepeatableContainers.valueOf(ExplicitRepeatable.class, InvalidNoValue.class))
            .withMessageContaining("Invalid declaration of container type ["
                    + InvalidNoValue.class.getName()
                    + "] for repeatable annotation ["
                    + ExplicitRepeatable.class.getName() + "]");
  }

  @Test
  void ofExplicitWhenValueIsNotArrayThrowsException() {
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    RepeatableContainers.valueOf(ExplicitRepeatable.class, InvalidNotArray.class))
            .withMessage("Container type ["
                    + InvalidNotArray.class.getName()
                    + "] must declare a 'value' attribute for an array of type ["
                    + ExplicitRepeatable.class.getName() + "]");
  }

  @Test
  void ofExplicitWhenValueIsArrayOfWrongTypeThrowsException() {
    assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
                    RepeatableContainers.valueOf(ExplicitRepeatable.class, InvalidWrongArrayType.class))
            .withMessage("Container type ["
                    + InvalidWrongArrayType.class.getName()
                    + "] must declare a 'value' attribute for an array of type ["
                    + ExplicitRepeatable.class.getName() + "]");
  }

  @Test
  void ofExplicitWhenAnnotationIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    RepeatableContainers.valueOf(null, null))
            .withMessage("Repeatable must not be null");
  }

  @Test
  void ofExplicitWhenContainerIsNullDeducesContainer() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.valueOf(StandardRepeatable.class, null),
            WithStandardRepeatables.class, StandardContainer.class);
    assertThat(values).containsExactly("a", "b");
  }

  @Test
  void ofExplicitWhenContainerIsNullAndNotRepeatableThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    RepeatableContainers.valueOf(ExplicitRepeatable.class, null))
            .withMessage("Annotation type must be a repeatable annotation: " +
                    "failed to resolve container type for " +
                    ExplicitRepeatable.class.getName());
  }

  @Test
  void standardAndExplicitReturnsRepeats() {
    RepeatableContainers repeatableContainers = RepeatableContainers.standard().and(
            ExplicitContainer.class, ExplicitRepeatable.class);
    assertThat(findRepeatedAnnotationValues(repeatableContainers,
            WithStandardRepeatables.class, StandardContainer.class)).containsExactly(
            "a", "b");
    assertThat(findRepeatedAnnotationValues(repeatableContainers,
            WithExplicitRepeatables.class, ExplicitContainer.class)).containsExactly(
            "a", "b");
  }

  @Test
  void noneAlwaysReturnsNull() {
    Object[] values = findRepeatedAnnotationValues(
            RepeatableContainers.none(), WithStandardRepeatables.class,
            StandardContainer.class);
    assertThat(values).isNull();
  }

  @Test
  void equalsAndHashcode() {
    RepeatableContainers c1 = RepeatableContainers.valueOf(ExplicitRepeatable.class,
            ExplicitContainer.class);
    RepeatableContainers c2 = RepeatableContainers.valueOf(ExplicitRepeatable.class,
            ExplicitContainer.class);
    RepeatableContainers c3 = RepeatableContainers.standard();
    RepeatableContainers c4 = RepeatableContainers.standard().and(
            ExplicitContainer.class, ExplicitRepeatable.class);
    assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    assertThat(c1).isEqualTo(c1).isEqualTo(c2);
    assertThat(c1).isNotEqualTo(c3).isNotEqualTo(c4);
  }

  private Object[] findRepeatedAnnotationValues(RepeatableContainers containers,
          Class<?> element, Class<? extends Annotation> annotationType) {
    Annotation[] annotations = containers.findRepeatedAnnotations(
            element.getAnnotation(annotationType));
    return extractValues(annotations);
  }

  private Object[] extractValues(Annotation[] annotations) {
    try {
      if (annotations == null) {
        return null;
      }
      Object[] result = new String[annotations.length];
      for (int i = 0; i < annotations.length; i++) {
        result[i] = annotations[i].annotationType().getMethod("value").invoke(
                annotations[i]);
      }
      return result;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface NonRepeatable {

    String value() default "";

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Repeatable(StandardContainer.class)
  static @interface StandardRepeatable {

    String value() default "";

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface StandardContainer {

    StandardRepeatable[] value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface ExplicitRepeatable {

    String value() default "";

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface ExplicitContainer {

    ExplicitRepeatable[] value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface InvalidNoValue {

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface InvalidNotArray {

    int value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  static @interface InvalidWrongArrayType {

    StandardRepeatable[] value();

  }

  @NonRepeatable("a")
  static class WithNonRepeatable {

  }

  @StandardRepeatable("a")
  static class WithSingleStandardRepeatable {

  }

  @StandardRepeatable("a")
  @StandardRepeatable("b")
  static class WithStandardRepeatables {

  }

  @ExplicitContainer({ @ExplicitRepeatable("a"), @ExplicitRepeatable("b") })
  static class WithExplicitRepeatables {

  }

}
