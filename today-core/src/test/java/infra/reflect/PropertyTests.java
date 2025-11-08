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

package infra.reflect;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import infra.core.MethodParameter;
import infra.core.ResolvableType;
import infra.core.TypeDescriptor;
import infra.core.annotation.AnnotationFilter;
import infra.core.annotation.MergedAnnotation;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.RepeatableContainers;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/2/3 19:37
 */
class PropertyTests {

  @Test
  void annotations() throws Exception {
    Method getInt = Bean.class.getDeclaredMethod("getInt");
    Method setInt = Bean.class.getDeclaredMethod("setInt", Integer.class);
    Property property = new Property(Bean.class, getInt, setInt);
    MergedAnnotations annotations = MergedAnnotations.from(property, property.getAnnotations(),
            RepeatableContainers.standard(), AnnotationFilter.JAVA);

    assertThat(annotations.get(Nullable.class).isPresent()).isFalse();
    assertThat(annotations.get(Override.class).isPresent()).isFalse();
    assertThat(annotations.get(Required.class).isPresent()).isTrue();

    assertThat(property.isNullable()).isTrue();
  }

  @Test
  void getPropertyTypeFromField() throws NoSuchFieldException {
    Property property = new Property(NameField.class.getDeclaredField("name"), null, null);
    assertThat(property.getType()).isEqualTo(String.class);
  }

  @Test
  void getPropertyTypeFromReadMethod() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.getType()).isEqualTo(String.class);
  }

  @Test
  void getPropertyTypeFromWriteMethod() {
    Property property = new Property("name", null,
            ReflectionUtils.findMethod(Bean.class, "setName", String.class), Bean.class);
    assertThat(property.getType()).isEqualTo(String.class);
  }

  @Test
  void writableWithWriteMethod() {
    Property property = new Property("name", null,
            ReflectionUtils.findMethod(Bean.class, "setName", String.class), Bean.class);
    assertThat(property.isWriteable()).isTrue();
  }

  @Test
  void writableWithNonFinalField() throws NoSuchFieldException {
    Field nameField = NameField.class.getDeclaredField("name");
    Property property = new Property(nameField, null, null);
    assertThat(property.isWriteable()).isTrue();
  }

  @Test
  void nonWritableWithFinalField() throws NoSuchFieldException {
    Field finalNameField = FinalNameField.class.getDeclaredField("name");
    Property property = new Property(finalNameField, null, null);
    assertThat(property.isWriteable()).isFalse();
  }

  @Test
  void readableWithReadMethod() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.isReadable()).isTrue();
  }

  @Test
  void readableWithField() throws NoSuchFieldException {
    Field nameField = NameField.class.getDeclaredField("name");
    Property property = new Property(nameField, null, null);
    assertThat(property.isReadable()).isTrue();
  }

  @Test
  void nonReadableWithoutReadMethodOrField() {
    Property property = new Property("name", null,
            ReflectionUtils.findMethod(Bean.class, "setName", String.class), Bean.class);
    assertThat(property.isReadable()).isTrue();
  }

  @Test
  void primitiveType() {
    Property property = new Property("age",
            ReflectionUtils.findMethod(Bean.class, "getAge"), null, Bean.class);
    assertThat(property.isPrimitive()).isTrue();
  }

  @Test
  void nonPrimitiveType() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.isPrimitive()).isFalse();
  }

  @Test
  void isInstanceReturnsTrueForCompatibleType() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.isInstance("test")).isTrue();
  }

  @Test
  void isInstanceReturnsFalseForIncompatibleType() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.isInstance(123)).isFalse();
  }

  @Test
  void throwsExceptionWhenNoReadOrWriteMethod() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new Property("test", null, null, Bean.class))
            .withMessageContaining("Property 'test' in");
  }

  @Test
  void equalsWithSamePropertyReturnsTrue() {
    Property property1 = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"),
            ReflectionUtils.findMethod(Bean.class, "setName", String.class),
            Bean.class);
    Property property2 = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"),
            ReflectionUtils.findMethod(Bean.class, "setName", String.class),
            Bean.class);
    assertThat(property1.equals(property2)).isTrue();
  }

  @Test
  void equalsWithDifferentNameReturnsFalse() {
    Property property1 = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    Property property2 = new Property("age",
            ReflectionUtils.findMethod(Bean.class, "getAge"), null, Bean.class);
    assertThat(property1.equals(property2)).isFalse();
  }

  @Test
  void getDeclaringClassFromField() throws NoSuchFieldException {
    Field nameField = NameField.class.getDeclaredField("name");
    Property property = new Property(nameField, null, null);
    assertThat(property.getDeclaringClass()).isEqualTo(NameField.class);
  }

  @Test
  void getDeclaringClassFromReadMethod() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.getDeclaringClass()).isEqualTo(Bean.class);
  }

  @Test
  void getDeclaringClassFromWriteMethod() {
    Property property = new Property("name", null,
            ReflectionUtils.findMethod(Bean.class, "setName", String.class), Bean.class);
    assertThat(property.getDeclaringClass()).isEqualTo(Bean.class);
  }

  @Test
  void toStringContainsTypeAndName() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.toString()).isEqualTo("String name");
  }

  @Test
  void methodBasedReturnsTrueForReadMethod() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.isMethodBased()).isTrue();
  }

  @Test
  void methodBasedReturnsTrueForWriteMethod() {
    Property property = new Property("name", null,
            ReflectionUtils.findMethod(Bean.class, "setName", String.class), Bean.class);
    assertThat(property.isMethodBased()).isTrue();
  }

  @Test
  void methodBasedReturnsFalseForFieldOnly() throws NoSuchFieldException {
    Field nameField = NameField.class.getDeclaredField("name");
    Property property = new Property(nameField, null, null);
    assertThat(property.isMethodBased()).isFalse();
  }

  @Test
  void getModifiersFromReadMethod() throws NoSuchMethodException {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property.getModifiers()).isEqualTo(Bean.class.getDeclaredMethod("getName").getModifiers());
  }

  @Test
  void getModifiersFromWriteMethod() throws NoSuchMethodException {
    Property property = new Property("name", null,
            ReflectionUtils.findMethod(Bean.class, "setName", String.class), Bean.class);
    assertThat(property.getModifiers()).isEqualTo(Bean.class.getDeclaredMethod("setName", String.class).getModifiers());
  }

  @Test
  void getModifiersFromField() throws NoSuchFieldException {
    Field nameField = NameField.class.getDeclaredField("name");
    Property property = new Property(nameField, null, null);
    assertThat(property.getModifiers()).isEqualTo(nameField.getModifiers());
  }

  @Test
  void hashCodeEqualForSameProperties() {
    Property property1 = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    Property property2 = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    assertThat(property1.hashCode()).isEqualTo(property2.hashCode());
  }

  @Test
  void getTypeDescriptorReturnsConsistentResult() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    TypeDescriptor descriptor1 = property.getTypeDescriptor();
    TypeDescriptor descriptor2 = property.getTypeDescriptor();
    assertThat(descriptor1).isSameAs(descriptor2);
  }

  @Test
  void getResolvableTypeReturnsConsistentResult() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(Bean.class, "getName"), null, Bean.class);
    ResolvableType type1 = property.getResolvableType();
    ResolvableType type2 = property.getResolvableType();
    assertThat(type1).isSameAs(type2);
  }

  @Test
  void getResolvableTypeFromField() throws NoSuchFieldException {
    Field nameField = NameField.class.getDeclaredField("name");
    Property property = new Property(nameField, null, null);
    assertThat(property.getResolvableType()).isEqualTo(ResolvableType.forField(nameField));
  }

  @Test
  void getResolvableTypeFromMethod() {
    Method method = ReflectionUtils.findMethod(Bean.class, "getName");
    Property property = new Property("name", method, null, Bean.class);
    assertThat(property.getResolvableType())
            .isEqualTo(ResolvableType.forMethodParameter(property.getMethodParameter()));
  }

  @Test
  void methodParameterResolutionWithReadMethodOnly() {
    Method readMethod = ReflectionUtils.findMethod(Bean.class, "getName");
    Property property = new Property("name", readMethod, null, Bean.class);
    assertThat(property.getMethodParameter().getMethod()).isSameAs(readMethod);
  }

  @Test
  void methodParameterResolutionWithWriteMethodOnly() {
    Method writeMethod = ReflectionUtils.findMethod(Bean.class, "setName", String.class);
    Property property = new Property("name", null, writeMethod, Bean.class);
    assertThat(property.getMethodParameter().getMethod()).isSameAs(writeMethod);
  }

  @Test
  void writeMethodParameterResolution() {
    Method writeMethod = ReflectionUtils.findMethod(Bean.class, "setName", String.class);
    Property property = new Property("name", null, writeMethod, Bean.class);
    assertThat(property.getWriteMethodParameter().getMethod()).isSameAs(writeMethod);
  }

  @Test
  void nullableWithFieldLevelAnnotation() throws NoSuchFieldException {
    Field field = NullableFieldBean.class.getDeclaredField("name");
    Property property = new Property(field, null, null);
    assertThat(property.isNullable()).isTrue();
  }

  @Test
  void nullableWithReadMethodAnnotation() {
    Property property = new Property("name",
            ReflectionUtils.findMethod(NullableMethodBean.class, "getName"), null, NullableMethodBean.class);
    assertThat(property.isNullable()).isTrue();
  }

  @Test
  void nullableWithWriteMethodParameter() {
    Property property = new Property("name", null,
            ReflectionUtils.findMethod(NullableParamBean.class, "setName", String.class), NullableParamBean.class);
    assertThat(property.isNullable()).isTrue();
  }

  @Test
  void mergedAnnotationsWithMultipleSources() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(AnnotatedBean.class, "setName", String.class);
    Field field = AnnotatedBean.class.getDeclaredField("name");

    Property property = new Property(field, readMethod, writeMethod);

    MergedAnnotations mergedAnnotations = property.mergedAnnotations();
    assertThat(mergedAnnotations.get(ReadAnnotation.class).isPresent()).isTrue();
    assertThat(mergedAnnotations.get(WriteAnnotation.class).isPresent()).isTrue();
    assertThat(mergedAnnotations.get(FieldAnnotation.class).isPresent()).isTrue();
  }

  @Test
  void annotationCacheReturnsSameInstance() {
    Method readMethod = ReflectionUtils.findMethod(AnnotatedBean.class, "getName");
    Property property = new Property("name", readMethod, null, AnnotatedBean.class);

    Annotation[] first = property.getAnnotations();
    Annotation[] second = property.getAnnotations();

    assertThat(first).isNotSameAs(second);
    assertThat(first).isEqualTo(second);
  }

  @Test
  void getDeclaredAnnotationsReturnsCopy() {
    Method readMethod = ReflectionUtils.findMethod(AnnotatedBean.class, "getName");
    Property property = new Property("name", readMethod, null, AnnotatedBean.class);

    Annotation[] first = property.getDeclaredAnnotations();
    Annotation[] second = property.getDeclaredAnnotations();

    assertThat(first).isNotSameAs(second);
    assertThat(first).containsExactlyInAnyOrder(second);
  }

  @Test
  void findFieldByCapitalizedName() throws NoSuchFieldException {
    Property property = new Property("Name", null,
            ReflectionUtils.findMethod(Bean.class, "setName", String.class), Bean.class);
    assertThat(property.getField()).isNotNull();
    assertThat(property.getField().getName()).isEqualTo("name");
  }

  @Test
  void getMethodParameterResolvesToWriteMethodWhenMoreSpecific() {
    Method readMethod = ReflectionUtils.findMethod(SuperClass.class, "getValue");
    Method writeMethod = ReflectionUtils.findMethod(SubClass.class, "setValue", String.class);
    Property property = new Property("value", readMethod, writeMethod, SubClass.class);
    assertThat(property.getMethodParameter().getMethod()).isSameAs(writeMethod);
  }

  @Test
  void getPropertyTypeFromInterfaceMethod() {
    Property property = new Property("int",
            ReflectionUtils.findMethod(Ifc.class, "getInt"), null, Ifc.class);
    assertThat(property.getType()).isEqualTo(Integer.class);
  }

  @Test
  void annotationsFromInterfaceMethodsAreIncluded() {
    Method method = ReflectionUtils.findMethod(Bean.class, "getInt");
    Property property = new Property("int", method, null, Bean.class);
    assertThat(property.getAnnotation(Nullable.class)).isNull(); // TYPE_USE
  }

  @Test
  void mergingAnnotationsPreservesOrder() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(AnnotatedBean.class, "setName", String.class);
    Field field = AnnotatedBean.class.getDeclaredField("name");

    Property property1 = new Property("name", readMethod, writeMethod, AnnotatedBean.class);
    Property property2 = new Property("name", readMethod, writeMethod, AnnotatedBean.class);

    MergedAnnotations annotations1 = property1.mergedAnnotations();
    MergedAnnotations annotations2 = property2.mergedAnnotations();

    assertThat(annotations1.stream().map(MergedAnnotation::getType).toList())
            .isEqualTo(annotations2.stream().map(MergedAnnotation::getType).toList());
  }

  @Test
  void shouldCreatePropertyWithFieldOnly() throws NoSuchFieldException {
    Field field = PropertyTests.NameField.class.getDeclaredField("name");
    Property property = new Property(field, null, null);

    assertThat(property.getName()).isEqualTo("name");
    assertThat(property.getField()).isEqualTo(field);
    assertThat(property.getType()).isEqualTo(String.class);
    assertThat(property.getDeclaringClass()).isEqualTo(PropertyTests.NameField.class);
  }

  @Test
  void shouldCreatePropertyWithReadAndWriteMethods() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);
    Property property = new Property(PropertyTests.Bean.class, readMethod, writeMethod);

    assertThat(property.getName()).isNotNull();
    assertThat(property.getReadMethod()).isEqualTo(readMethod);
    assertThat(property.getWriteMethod()).isEqualTo(writeMethod);
    assertThat(property.getType()).isEqualTo(String.class);
  }

  @Test
  void shouldCreatePropertyWithNameAndMethods() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);
    Property property = new Property("customName", readMethod, writeMethod, PropertyTests.Bean.class);

    assertThat(property.getName()).isEqualTo("customName");
    assertThat(property.getReadMethod()).isEqualTo(readMethod);
    assertThat(property.getWriteMethod()).isEqualTo(writeMethod);
    assertThat(property.getType()).isEqualTo(String.class);
  }

  @Test
  void shouldThrowExceptionWhenNoMethodsProvided() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new Property("testName", null, null, PropertyTests.Bean.class))
            .withMessageContaining("Property 'testName' in");
  }

  @Test
  void shouldHandlePrimitivePropertyType() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getAge");
    Property property = new Property("age", readMethod, null, PropertyTests.Bean.class);

    assertThat(property.getType()).isEqualTo(int.class);
    assertThat(property.isPrimitive()).isTrue();
  }

  @Test
  void shouldHandleNonPrimitivePropertyType() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    assertThat(property.getType()).isEqualTo(String.class);
    assertThat(property.isPrimitive()).isFalse();
  }

  @Test
  void shouldDetermineWriteableBasedOnWriteMethod() {
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);
    Property property = new Property("name", null, writeMethod, PropertyTests.Bean.class);

    assertThat(property.isWriteable()).isTrue();
  }

  @Test
  void shouldDetermineWriteableBasedOnNonFinalField() throws NoSuchFieldException {
    Field field = PropertyTests.NameField.class.getDeclaredField("name");
    Property property = new Property(field, null, null);

    assertThat(property.isWriteable()).isTrue();
  }

  @Test
  void shouldDetermineNotWriteableBasedOnFinalField() throws NoSuchFieldException {
    Field field = PropertyTests.FinalNameField.class.getDeclaredField("name");
    Property property = new Property(field, null, null);

    assertThat(property.isWriteable()).isFalse();
  }

  @Test
  void shouldDetermineReadableBasedOnReadMethod() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    assertThat(property.isReadable()).isTrue();
  }

  @Test
  void shouldDetermineReadableBasedOnField() throws NoSuchFieldException {
    Field field = PropertyTests.NameField.class.getDeclaredField("name");
    Property property = new Property(field, null, null);

    assertThat(property.isReadable()).isTrue();
  }

  @Test
  void shouldGetInstanceCheck() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    assertThat(property.isInstance("testString")).isTrue();
    assertThat(property.isInstance(123)).isFalse();
  }

  @Test
  void shouldGetModifiersFromField() throws NoSuchFieldException {
    Field field = PropertyTests.NameField.class.getDeclaredField("name");
    Property property = new Property(field, null, null);

    assertThat(property.getModifiers()).isEqualTo(field.getModifiers());
  }

  @Test
  void shouldGetModifiersFromReadMethod() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    assertThat(property.getModifiers()).isEqualTo(readMethod.getModifiers());
  }

  @Test
  void shouldHandleSyntheticMember() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    // Usually false for normal methods
    assertThat(property.isSynthetic()).isEqualTo(readMethod.isSynthetic());
  }

  @Test
  void shouldGetPropertyFromInterface() {
    Method method = ReflectionUtils.findMethod(PropertyTests.Ifc.class, "getInt");
    Property property = new Property("int", method, null, PropertyTests.Ifc.class);

    assertThat(property.getType()).isEqualTo(Integer.class);
  }

  @Test
  void shouldResolveFieldByNameVariations() throws NoSuchFieldException {
    Field actualField = PropertyTests.NameField.class.getDeclaredField("name");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);

    // Test with capitalized name that should find the lowercase field
    Property property = new Property("Name", null, writeMethod, PropertyTests.NameField.class);

    assertThat(property.getField()).isEqualTo(actualField);
  }

  @Test
  void shouldGetTypeDescriptorAndCacheIt() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    TypeDescriptor descriptor1 = property.getTypeDescriptor();
    TypeDescriptor descriptor2 = property.getTypeDescriptor();

    assertThat(descriptor1).isNotNull();
    assertThat(descriptor1).isSameAs(descriptor2);
  }

  @Test
  void shouldGetResolvableTypeFromReadMethod() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    ResolvableType resolvableType = property.getResolvableType();

    assertThat(resolvableType).isNotNull();
    assertThat(resolvableType).isEqualTo(ResolvableType.forMethodParameter(property.getMethodParameter()));
  }

  @Test
  void shouldGetResolvableTypeFromWriteMethod() {
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);
    Property property = new Property("name", null, writeMethod, PropertyTests.Bean.class);

    ResolvableType resolvableType = property.getResolvableType();

    assertThat(resolvableType).isNotNull();
    assertThat(resolvableType).isEqualTo(ResolvableType.forMethodParameter(property.getMethodParameter()));
  }

  @Test
  void shouldGetResolvableTypeFromField() throws NoSuchFieldException {
    Field field = PropertyTests.NameField.class.getDeclaredField("name");
    Property property = new Property(field, null, null);

    ResolvableType resolvableType = property.getResolvableType();

    assertThat(resolvableType).isNotNull();
    assertThat(resolvableType).isEqualTo(ResolvableType.forField(field));
  }

  @Test
  void shouldGetMethodParameterFromReadMethod() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    MethodParameter methodParameter = property.getMethodParameter();

    assertThat(methodParameter).isNotNull();
    assertThat(methodParameter.getMethod()).isEqualTo(readMethod);
  }

  @Test
  void shouldGetMethodParameterFromWriteMethod() {
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);
    Property property = new Property("name", null, writeMethod, PropertyTests.Bean.class);

    MethodParameter methodParameter = property.getMethodParameter();

    assertThat(methodParameter).isNotNull();
    assertThat(methodParameter.getMethod()).isEqualTo(writeMethod);
  }

  @Test
  void shouldGetWriteMethodParameter() {
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);
    Property property = new Property("name", null, writeMethod, PropertyTests.Bean.class);

    MethodParameter writeMethodParameter = property.getWriteMethodParameter();

    assertThat(writeMethodParameter).isNotNull();
    assertThat(writeMethodParameter.getMethod()).isEqualTo(writeMethod);
  }

  @Test
  void shouldReturnNullForWriteMethodParameterWhenNoWriteMethod() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    MethodParameter writeMethodParameter = property.getWriteMethodParameter();

    assertThat(writeMethodParameter).isNull();
  }

  @Test
  void shouldDetermineMethodBasedProperty() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);

    Property propertyWithReadMethod = new Property("name", readMethod, null, PropertyTests.Bean.class);
    Property propertyWithWriteMethod = new Property("name", null, writeMethod, PropertyTests.Bean.class);
    Property propertyWithBothMethods = new Property("name", readMethod, writeMethod, PropertyTests.Bean.class);

    assertThat(propertyWithReadMethod.isMethodBased()).isTrue();
    assertThat(propertyWithWriteMethod.isMethodBased()).isTrue();
    assertThat(propertyWithBothMethods.isMethodBased()).isTrue();
  }

  @Test
  void shouldNotBeMethodBasedForFieldOnly() throws NoSuchFieldException {
    Field field = PropertyTests.NameField.class.getDeclaredField("name");
    Property property = new Property(field, null, null);

    assertThat(property.isMethodBased()).isFalse();
  }

  @Test
  void shouldGetMergedAnnotations() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "setName", String.class);
    Field field = PropertyTests.AnnotatedBean.class.getDeclaredField("name");

    Property property = new Property(field, readMethod, writeMethod);
    MergedAnnotations mergedAnnotations = property.mergedAnnotations();

    assertThat(mergedAnnotations).isNotNull();
    assertThat(mergedAnnotations.get(PropertyTests.ReadAnnotation.class).isPresent()).isTrue();
    assertThat(mergedAnnotations.get(PropertyTests.WriteAnnotation.class).isPresent()).isTrue();
    assertThat(mergedAnnotations.get(PropertyTests.FieldAnnotation.class).isPresent()).isTrue();
  }

  @Test
  void shouldCacheMergedAnnotations() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "setName", String.class);
    Field field = PropertyTests.AnnotatedBean.class.getDeclaredField("name");

    Property property = new Property(field, readMethod, writeMethod);
    MergedAnnotations annotations1 = property.mergedAnnotations();
    MergedAnnotations annotations2 = property.mergedAnnotations();

    assertThat(annotations1).isSameAs(annotations2);
  }

  @Test
  void shouldGetAnnotations() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "setName", String.class);
    Field field = PropertyTests.AnnotatedBean.class.getDeclaredField("name");

    Property property = new Property(field, readMethod, writeMethod);
    Annotation[] annotations = property.getAnnotations();

    assertThat(annotations).isNotNull();
    assertThat(annotations).hasSize(3); // ReadAnnotation, WriteAnnotation, FieldAnnotation
  }

  @Test
  void shouldGetDeclaredAnnotations() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "setName", String.class);
    Field field = PropertyTests.AnnotatedBean.class.getDeclaredField("name");

    Property property = new Property(field, readMethod, writeMethod);
    Annotation[] declaredAnnotations = property.getDeclaredAnnotations();

    assertThat(declaredAnnotations).isNotNull();
    assertThat(declaredAnnotations).hasSize(3); // ReadAnnotation, WriteAnnotation, FieldAnnotation
  }

  @Test
  void shouldCheckAnnotationPresence() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "setName", String.class);
    Field field = PropertyTests.AnnotatedBean.class.getDeclaredField("name");

    Property property = new Property(field, readMethod, writeMethod);

    assertThat(property.isAnnotationPresent(PropertyTests.ReadAnnotation.class)).isTrue();
    assertThat(property.isAnnotationPresent(PropertyTests.WriteAnnotation.class)).isTrue();
    assertThat(property.isAnnotationPresent(PropertyTests.FieldAnnotation.class)).isTrue();
    assertThat(property.isAnnotationPresent(Deprecated.class)).isFalse();
  }

  @Test
  void shouldGetSpecificAnnotation() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "setName", String.class);
    Field field = PropertyTests.AnnotatedBean.class.getDeclaredField("name");

    Property property = new Property(field, readMethod, writeMethod);

    PropertyTests.ReadAnnotation readAnnotation = property.getAnnotation(PropertyTests.ReadAnnotation.class);
    PropertyTests.WriteAnnotation writeAnnotation = property.getAnnotation(PropertyTests.WriteAnnotation.class);
    PropertyTests.FieldAnnotation fieldAnnotation = property.getAnnotation(PropertyTests.FieldAnnotation.class);

    assertThat(readAnnotation).isNotNull();
    assertThat(writeAnnotation).isNotNull();
    assertThat(fieldAnnotation).isNotNull();
  }

  @Test
  void shouldReturnNullForNonExistentAnnotation() throws NoSuchFieldException {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.AnnotatedBean.class, "setName", String.class);
    Field field = PropertyTests.AnnotatedBean.class.getDeclaredField("name");

    Property property = new Property(field, readMethod, writeMethod);

    Deprecated deprecated = property.getAnnotation(Deprecated.class);
    assertThat(deprecated).isNull();
  }

  @Test
  void shouldHandleEqualsAndHashCode() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Method writeMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "setName", String.class);

    Property property1 = new Property("name", readMethod, writeMethod, PropertyTests.Bean.class);
    Property property2 = new Property("name", readMethod, writeMethod, PropertyTests.Bean.class);
    Property property3 = new Property("age",
            ReflectionUtils.findMethod(PropertyTests.Bean.class, "getAge"), null, PropertyTests.Bean.class);

    assertThat(property1.equals(property2)).isTrue();
    assertThat(property1.equals(property3)).isFalse();
    assertThat(property1.equals(null)).isFalse();
    assertThat(property1.equals(new Object())).isFalse();

    assertThat(property1.hashCode()).isEqualTo(property2.hashCode());
  }

  @Test
  void shouldToStringReturnFormattedString() {
    Method readMethod = ReflectionUtils.findMethod(PropertyTests.Bean.class, "getName");
    Property property = new Property("name", readMethod, null, PropertyTests.Bean.class);

    String result = property.toString();

    assertThat(result).isEqualTo("String name");
  }

  @Test
  void shouldHandleSyntheticField() throws NoSuchFieldException {
    Field field = PropertyTests.NameField.class.getDeclaredField("name");
    Property property = new Property(field, null, null);

    boolean synthetic = property.isSynthetic();

    assertThat(synthetic).isEqualTo(field.isSynthetic());
  }

  static class SuperClass {
    public Object getValue() { return null; }
  }

  static class SubClass extends SuperClass {
    public void setValue(String value) { }
  }

  static class NullableFieldBean {
    @Nullable
    String name;
  }

  static class NullableMethodBean {
    @Nullable
    public String getName() { return null; }
  }

  static class NullableParamBean {
    public void setName(@Nullable String name) { }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ReadAnnotation { }

  @Retention(RetentionPolicy.RUNTIME)
  @interface WriteAnnotation { }

  @Retention(RetentionPolicy.RUNTIME)
  @interface FieldAnnotation { }

  static class AnnotatedBean {
    @FieldAnnotation
    private String name;

    @ReadAnnotation
    public String getName() { return name; }

    @WriteAnnotation
    public void setName(String name) { this.name = name; }
  }

  static class Bean implements Ifc {
    private String name;
    private int age;

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }

    @Nullable
    @Override
    public Integer getInt() {
      return 0;
    }

    @Required
    @Override
    public void setInt(Integer val) {

    }

  }

  static class NameField {
    String name;
  }

  static class FinalNameField {
    final String name = "";
  }

  interface Ifc {

    @Nullable
    Integer getInt();

    void setInt(Integer val);

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.FIELD,
          ElementType.TYPE, ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.METHOD })
  public @interface Required {

  }

}