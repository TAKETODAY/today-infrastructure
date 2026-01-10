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

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AttributeMethods}.
 *
 * @author Phillip Webb
 */
class AttributeMethodsTests {

  @Test
  void forAnnotationTypeWhenNullReturnsNone() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(null);
    assertThat(methods).isSameAs(AttributeMethods.NONE);
  }

  @Test
  void forAnnotationTypeWhenHasNoAttributesReturnsNone() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(NoAttributes.class);
    assertThat(methods).isSameAs(AttributeMethods.NONE);
  }

  @Test
  void forAnnotationTypeWhenHasMultipleAttributesReturnsAttributes() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    assertThat(methods.get("value").getName()).isEqualTo("value");
    assertThat(methods.get("intValue").getName()).isEqualTo("intValue");
    assertThat(getAll(methods)).flatExtracting(Method::getName).containsExactly("intValue", "value");
  }

  @Test
  void indexOfNameReturnsIndex() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    assertThat(methods.indexOf("value")).isEqualTo(1);
  }

  @Test
  void indexOfMethodReturnsIndex() throws Exception {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    Method method = MultipleAttributes.class.getDeclaredMethod("value");
    assertThat(methods.indexOf(method)).isEqualTo(1);
  }

  @Test
  void sizeReturnsSize() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    assertThat(methods.size()).isEqualTo(2);
  }

  @Test
  void canThrowTypeNotPresentExceptionWhenHasClassAttributeReturnsTrue() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(ClassValue.class);
    assertThat(methods.canThrowTypeNotPresentException(0)).isTrue();
  }

  @Test
  void canThrowTypeNotPresentExceptionWhenHasClassArrayAttributeReturnsTrue() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(ClassArrayValue.class);
    assertThat(methods.canThrowTypeNotPresentException(0)).isTrue();
  }

  @Test
  void canThrowTypeNotPresentExceptionWhenNotClassOrClassArrayAttributeReturnsFalse() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(ValueOnly.class);
    assertThat(methods.canThrowTypeNotPresentException(0)).isFalse();
  }

  @Test
  void hasDefaultValueMethodWhenHasDefaultValueMethodReturnsTrue() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(DefaultValueAttribute.class);
    assertThat(methods.hasDefaultValueMethod).isTrue();
  }

  @Test
  void hasDefaultValueMethodWhenHasNoDefaultValueMethodsReturnsFalse() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    assertThat(methods.hasDefaultValueMethod).isFalse();
  }

  @Test
  void isValidWhenHasTypeNotPresentExceptionReturnsFalse() {
    ClassValue annotation = mockAnnotation(ClassValue.class);
    given(annotation.value()).willThrow(TypeNotPresentException.class);
    AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
    assertThat(attributes.isValid(annotation, getClass())).isFalse();
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  void isValidWhenDoesNotHaveTypeNotPresentExceptionReturnsTrue() {
    ClassValue annotation = mock(ClassValue.class);
    given(annotation.value()).willReturn((Class) InputStream.class);
    AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
    assertThat(attributes.isValid(annotation, getClass())).isTrue();
  }

  @Test
  void validateWhenHasTypeNotPresentExceptionThrowsException() {
    ClassValue annotation = mockAnnotation(ClassValue.class);
    given(annotation.value()).willThrow(TypeNotPresentException.class);
    AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
    assertThatIllegalStateException().isThrownBy(() -> attributes.validate(annotation));
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  void validateWhenDoesNotHaveTypeNotPresentExceptionThrowsNothing() {
    ClassValue annotation = mockAnnotation(ClassValue.class);
    given(annotation.value()).willReturn((Class) InputStream.class);
    AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
    attributes.validate(annotation);
  }

  @Test
  void forAnnotationTypeReturnsCachedInstance() {
    AttributeMethods methods1 = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    AttributeMethods methods2 = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    assertThat(methods1).isSameAs(methods2);
  }

  @Test
  void getMethodByNameReturnsCorrectMethod() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    Method method = methods.get("intValue");
    assertThat(method).isNotNull();
    assertThat(method.getName()).isEqualTo("intValue");
  }

  @Test
  void getMethodByIndexReturnsCorrectMethod() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    Method method = methods.get(0);
    assertThat(method.getName()).isEqualTo("intValue");
  }

  @Test
  void getMethodByInvalidNameReturnsNull() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    Method method = methods.get("nonexistent");
    assertThat(method).isNull();
  }

  @Test
  void indexOfInvalidNameReturnsNegativeOne() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    int index = methods.indexOf("nonexistent");
    assertThat(index).isEqualTo(-1);
  }

  @Test
  void indexOfInvalidMethodReturnsNegativeOne() throws Exception {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    Method invalidMethod = String.class.getDeclaredMethod("toString");
    int index = methods.indexOf(invalidMethod);
    assertThat(index).isEqualTo(-1);
  }

  @Test
  void sizeReturnsCorrectSize() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    assertThat(methods.size()).isEqualTo(2);

    AttributeMethods noAttrMethods = AttributeMethods.forAnnotationType(NoAttributes.class);
    assertThat(noAttrMethods.size()).isEqualTo(0);
  }

  @Test
  void canThrowTypeNotPresentExceptionReturnsFalseForSimpleTypes() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(ValueOnly.class);
    assertThat(methods.canThrowTypeNotPresentException(0)).isFalse();
  }

  @Test
  void canThrowTypeNotPresentExceptionReturnsTrueForEnum() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(EnumValue.class);
    assertThat(methods.canThrowTypeNotPresentException(0)).isTrue();
  }

  @Test
  void hasNestedAnnotationReturnsTrueWhenHasNestedAnnotation() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(NestedAnnotation.class);
    assertThat(methods.hasNestedAnnotation).isTrue();
  }

  @Test
  void hasNestedAnnotationReturnsFalseWhenNoNestedAnnotation() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(ValueOnly.class);
    assertThat(methods.hasNestedAnnotation).isFalse();
  }

  @Test
  void hasDefaultValueMethodReturnsFalseWhenNoDefaultValues() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    assertThat(methods.hasDefaultValueMethod).isFalse();
  }

  @Test
  void hasDefaultValueMethodReturnsTrueWhenHasDefaultValues() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(DefaultValueAttribute.class);
    assertThat(methods.hasDefaultValueMethod).isTrue();
  }

  @Test
  void validateDoesNotThrowForValidAnnotation() {
    ValueOnly annotation = mock(ValueOnly.class);
    given(annotation.annotationType()).willReturn((Class) ValueOnly.class);
    given(annotation.value()).willReturn("test");

    AttributeMethods methods = AttributeMethods.forAnnotationType(ValueOnly.class);
    assertThatCode(() -> methods.validate(annotation)).doesNotThrowAnyException();
  }

  @Test
  void describeMethodReturnsCorrectDescription() throws Exception {
    Method method = MultipleAttributes.class.getDeclaredMethod("intValue");
    String description = AttributeMethods.describe(method);
    assertThat(description).isEqualTo("attribute 'intValue' in annotation [infra.core.annotation.AttributeMethodsTests$MultipleAttributes]");
  }

  @Test
  void describeWithNullMethodReturnsNone() {
    String description = AttributeMethods.describe((Method) null);
    assertThat(description).isEqualTo("(none)");
  }

  @Test
  void describeWithNullAttributeNameReturnsNone() {
    String description = AttributeMethods.describe(MultipleAttributes.class, null);
    assertThat(description).isEqualTo("(none)");
  }

  @Test
  void getNameReturnsCanonicalNameWhenAvailable() {
    String name = AttributeMethods.getName(MultipleAttributes.class);
    assertThat(name).isEqualTo("infra.core.annotation.AttributeMethodsTests.MultipleAttributes");
  }

  @Test
  void getMethodByIndexThrowsIndexOutOfBoundsExceptionForInvalidIndex() {
    AttributeMethods methods = AttributeMethods.forAnnotationType(MultipleAttributes.class);
    assertThatThrownBy(() -> methods.get(-1))
            .isInstanceOf(IndexOutOfBoundsException.class);
    assertThatThrownBy(() -> methods.get(methods.size()))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface EnumValue {
    RetentionPolicy value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface NestedAnnotation {
    ValueOnly value();
  }

  private List<Method> getAll(AttributeMethods attributes) {
    List<Method> result = new ArrayList<>(attributes.size());
    for (int i = 0; i < attributes.size(); i++) {
      result.add(attributes.get(i));
    }
    return result;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <A extends Annotation> A mockAnnotation(Class<A> annotationType) {
    A annotation = mock(annotationType);
    given(annotation.annotationType()).willReturn((Class) annotationType);
    return annotation;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface NoAttributes {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface MultipleAttributes {

    int intValue();

    String value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ValueOnly {

    String value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface NonValueOnly {

    String test();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ClassValue {

    Class<?> value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ClassArrayValue {

    Class<?>[] value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface DefaultValueAttribute {

    String one();

    String two();

    String three() default "3";

  }

}
