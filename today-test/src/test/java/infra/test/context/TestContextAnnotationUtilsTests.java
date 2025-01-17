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

package infra.test.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.core.annotation.AnnotationUtils;
import infra.core.annotation.Order;
import infra.stereotype.Component;
import infra.stereotype.Service;
import infra.lang.TodayStrategies;
import infra.test.context.TestContextAnnotationUtils.AnnotationDescriptor;
import infra.test.context.TestContextAnnotationUtils.UntypedAnnotationDescriptor;
import infra.transaction.annotation.Transactional;

import static infra.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static infra.test.context.TestContextAnnotationUtils.findAnnotationDescriptor;
import static infra.test.context.TestContextAnnotationUtils.findAnnotationDescriptorForTypes;
import static infra.test.context.TestContextAnnotationUtils.searchEnclosingClass;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TestContextAnnotationUtils}.
 *
 * @author Sam Brannen
 * @see OverriddenMetaAnnotationAttributesTestContextAnnotationUtilsTests
 */
class TestContextAnnotationUtilsTests {

  @Nested
  @DisplayName("searchEnclosingClass() tests")
  class SearchEnclosingClassTests {

    @BeforeEach
    @AfterEach
    void clearCaches() {
      TestContextAnnotationUtils.clearCaches();
    }

    @AfterEach
    void clearGlobalFlag() {
      setGlobalFlag(null);
    }

    @Test
    void standardDefaultMode() {
      assertThat(searchEnclosingClass(OuterTestCase.class)).isFalse();
      assertThat(searchEnclosingClass(OuterTestCase.NestedTestCase.class)).isTrue();
      assertThat(searchEnclosingClass(OuterTestCase.NestedTestCase.DoubleNestedTestCase.class)).isTrue();
    }

    @Test
    void overriddenDefaultMode() {
      setGlobalFlag("\t" + OVERRIDE.name().toLowerCase() + "   ");
      assertThat(searchEnclosingClass(OuterTestCase.class)).isFalse();
      assertThat(searchEnclosingClass(OuterTestCase.NestedTestCase.class)).isFalse();
      assertThat(searchEnclosingClass(OuterTestCase.NestedTestCase.DoubleNestedTestCase.class)).isFalse();
    }

    private void setGlobalFlag(String flag) {
      TodayStrategies.setProperty(NestedTestConfiguration.ENCLOSING_CONFIGURATION_PROPERTY_NAME, flag);
    }
  }

  @Nested
  @DisplayName("findAnnotationDescriptor() tests")
  class FindAnnotationDescriptorTests {

    @Test
    void findAnnotationDescriptorWithNoAnnotationPresent() {
      assertThat(findAnnotationDescriptor(NonAnnotatedInterface.class, Transactional.class)).isNull();
      assertThat(findAnnotationDescriptor(NonAnnotatedClass.class, Transactional.class)).isNull();
    }

    @Test
    void findAnnotationDescriptorWithInheritedAnnotationOnClass() {
      // Note: @Transactional is inherited
      assertThat(findAnnotationDescriptor(InheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass()).isEqualTo(InheritedAnnotationClass.class);
      assertThat(findAnnotationDescriptor(SubInheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass()).isEqualTo(InheritedAnnotationClass.class);
    }

    @Test
    void findAnnotationDescriptorWithInheritedAnnotationOnInterface() {
      // Note: @Transactional is inherited
      Transactional rawAnnotation = InheritedAnnotationInterface.class.getAnnotation(Transactional.class);
      AnnotationDescriptor<Transactional> descriptor =
              findAnnotationDescriptor(InheritedAnnotationInterface.class, Transactional.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

      descriptor = findAnnotationDescriptor(SubInheritedAnnotationInterface.class, Transactional.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubInheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

      descriptor = findAnnotationDescriptor(SubSubInheritedAnnotationInterface.class, Transactional.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubSubInheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
    }

    @Test
    void findAnnotationDescriptorForNonInheritedAnnotationOnClass() {
      // Note: @Order is not inherited.
      assertThat(findAnnotationDescriptor(NonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationClass.class);
      assertThat(findAnnotationDescriptor(SubNonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationClass.class);
    }

    @Test
    void findAnnotationDescriptorForNonInheritedAnnotationOnInterface() {
      // Note: @Order is not inherited.
      Order rawAnnotation = NonInheritedAnnotationInterface.class.getAnnotation(Order.class);

      AnnotationDescriptor<Order> descriptor =
              findAnnotationDescriptor(NonInheritedAnnotationInterface.class, Order.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

      descriptor = findAnnotationDescriptor(SubNonInheritedAnnotationInterface.class, Order.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubNonInheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
    }

    @Test
    void findAnnotationDescriptorWithMetaComponentAnnotation() {
      assertAtComponentOnComposedAnnotation(HasMetaComponentAnnotation.class, "meta1", Meta1.class);
    }

    @Test
    void findAnnotationDescriptorWithLocalAndMetaComponentAnnotation() {
      Class<Component> annotationType = Component.class;
      AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(
              HasLocalAndMetaComponentAnnotation.class, annotationType);

      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(HasLocalAndMetaComponentAnnotation.class);
      assertThat(descriptor.getAnnotationType()).isEqualTo(annotationType);
    }

    @Test
    void findAnnotationDescriptorForInterfaceWithMetaAnnotation() {
      assertAtComponentOnComposedAnnotation(InterfaceWithMetaAnnotation.class, "meta1", Meta1.class);
    }

    @Test
    void findAnnotationDescriptorForClassWithMetaAnnotatedInterface() {
      Component rawAnnotation = AnnotationUtils.findAnnotation(ClassWithMetaAnnotatedInterface.class, Component.class);
      AnnotationDescriptor<Component> descriptor =
              findAnnotationDescriptor(ClassWithMetaAnnotatedInterface.class, Component.class);

      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(ClassWithMetaAnnotatedInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(Meta1.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
    }

    @Test
    void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndAnnotatedSuperclass() {
      AnnotationDescriptor<ContextConfiguration> descriptor = findAnnotationDescriptor(
              MetaAnnotatedAndSuperAnnotatedContextConfigClass.class, ContextConfiguration.class);

      assertThat(descriptor).as("AnnotationDescriptor should not be null").isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).as("rootDeclaringClass").isEqualTo(MetaAnnotatedAndSuperAnnotatedContextConfigClass.class);
      assertThat(descriptor.getDeclaringClass()).as("declaringClass").isEqualTo(MetaConfig.class);
      assertThat(descriptor.getAnnotationType()).as("annotationType").isEqualTo(ContextConfiguration.class);

      assertThat(descriptor.getAnnotation().classes()).as("configured classes").containsExactly(String.class);
    }

    @Test
    void findAnnotationDescriptorForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
      assertAtComponentOnComposedAnnotation(ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
    }

    @Test
    void findAnnotationDescriptorForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
      assertAtComponentOnComposedAnnotation(SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
              ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
    }

    /**
     * @since 4.0
     */
    @Test
    void findAnnotationDescriptorOnMetaMetaAnnotatedClass() {
      Class<?> startClass = MetaMetaAnnotatedClass.class;
      assertAtComponentOnComposedAnnotation(startClass, startClass, Meta2.class, "meta2");
    }

    /**
     * @since 4.0
     */
    @Test
    void findAnnotationDescriptorOnMetaMetaMetaAnnotatedClass() {
      Class<?> startClass = MetaMetaMetaAnnotatedClass.class;
      assertAtComponentOnComposedAnnotation(startClass, startClass, Meta2.class, "meta2");
    }

    /**
     * @since 4.0
     */
    @Test
    void findAnnotationDescriptorOnAnnotatedClassWithMissingTargetMetaAnnotation() {
      // InheritedAnnotationClass is NOT annotated or meta-annotated with @Component
      AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(
              InheritedAnnotationClass.class, Component.class);
      assertThat(descriptor).as("Should not find @Component on InheritedAnnotationClass").isNull();
    }

    /**
     * @since 4.0
     */
    @Test
    void findAnnotationDescriptorOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
      AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(
              MetaCycleAnnotatedClass.class, Component.class);
      assertThat(descriptor).as("Should not find @Component on MetaCycleAnnotatedClass").isNull();
    }

    private void assertAtComponentOnComposedAnnotation(
            Class<?> rootDeclaringClass, String name, Class<? extends Annotation> composedAnnotationType) {

      assertAtComponentOnComposedAnnotation(rootDeclaringClass, rootDeclaringClass, name, composedAnnotationType);
    }

    private void assertAtComponentOnComposedAnnotation(
            Class<?> startClass, Class<?> rootDeclaringClass, String name, Class<? extends Annotation> composedAnnotationType) {

      assertAtComponentOnComposedAnnotation(startClass, rootDeclaringClass, composedAnnotationType, name);
    }

    private void assertAtComponentOnComposedAnnotation(Class<?> startClass, Class<?> rootDeclaringClass,
            Class<?> declaringClass, String name) {

      AnnotationDescriptor<Component> descriptor = findAnnotationDescriptor(startClass, Component.class);
      assertThat(descriptor).as("AnnotationDescriptor should not be null").isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).as("rootDeclaringClass").isEqualTo(rootDeclaringClass);
      assertThat(descriptor.getDeclaringClass()).as("declaringClass").isEqualTo(declaringClass);
      assertThat(descriptor.getAnnotationType()).as("annotationType").isEqualTo(Component.class);
      assertThat(descriptor.getAnnotation().value()).as("component name").contains(name);
    }

  }

  @Nested
  @DisplayName("findAnnotationDescriptorForTypes() tests")
  class FindAnnotationDescriptorForTypesTests {

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesWithNoAnnotationPresent() {
      assertThat(findAnnotationDescriptorForTypes(NonAnnotatedInterface.class, Transactional.class, Component.class)).isNull();
      assertThat(findAnnotationDescriptorForTypes(NonAnnotatedClass.class, Transactional.class, Order.class)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesWithInheritedAnnotationOnClass() {
      // Note: @Transactional is inherited
      assertThat(findAnnotationDescriptorForTypes(InheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass()).isEqualTo(InheritedAnnotationClass.class);
      assertThat(findAnnotationDescriptorForTypes(SubInheritedAnnotationClass.class, Transactional.class).getRootDeclaringClass()).isEqualTo(InheritedAnnotationClass.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesWithInheritedAnnotationOnInterface() {
      // Note: @Transactional is inherited
      Transactional rawAnnotation = InheritedAnnotationInterface.class.getAnnotation(Transactional.class);

      UntypedAnnotationDescriptor descriptor =
              findAnnotationDescriptorForTypes(InheritedAnnotationInterface.class, Transactional.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

      descriptor = findAnnotationDescriptorForTypes(SubInheritedAnnotationInterface.class, Transactional.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubInheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

      descriptor = findAnnotationDescriptorForTypes(SubSubInheritedAnnotationInterface.class, Transactional.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubSubInheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(InheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnClass() {
      // Note: @Order is not inherited.
      assertThat(findAnnotationDescriptorForTypes(NonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationClass.class);
      assertThat(findAnnotationDescriptorForTypes(SubNonInheritedAnnotationClass.class, Order.class).getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationClass.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesForNonInheritedAnnotationOnInterface() {
      // Note: @Order is not inherited.
      Order rawAnnotation = NonInheritedAnnotationInterface.class.getAnnotation(Order.class);

      UntypedAnnotationDescriptor descriptor =
              findAnnotationDescriptorForTypes(NonInheritedAnnotationInterface.class, Order.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);

      descriptor = findAnnotationDescriptorForTypes(SubNonInheritedAnnotationInterface.class, Order.class);
      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(SubNonInheritedAnnotationInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(NonInheritedAnnotationInterface.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesWithLocalAndMetaComponentAnnotation() {
      Class<Component> annotationType = Component.class;
      UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
              HasLocalAndMetaComponentAnnotation.class, Transactional.class, annotationType, Order.class);
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(HasLocalAndMetaComponentAnnotation.class);
      assertThat(descriptor.getAnnotationType()).isEqualTo(annotationType);
    }

    @Test
    void findAnnotationDescriptorForTypesWithMetaComponentAnnotation() {
      Class<?> startClass = HasMetaComponentAnnotation.class;
      assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta1", Meta1.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesWithMetaAnnotationWithDefaultAttributes() {
      Class<?> startClass = MetaConfigWithDefaultAttributesTestCase.class;
      Class<ContextConfiguration> annotationType = ContextConfiguration.class;

      UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(startClass,
              Service.class, ContextConfiguration.class, Order.class, Transactional.class);

      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(startClass);
      assertThat(descriptor.getAnnotationType()).isEqualTo(annotationType);
      assertThat(((ContextConfiguration) descriptor.getAnnotation()).value()).isEmpty();
      assertThat(((ContextConfiguration) descriptor.getAnnotation()).classes())
              .containsExactly(MetaConfig.DevConfig.class, MetaConfig.ProductionConfig.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesWithMetaAnnotationWithOverriddenAttributes() {
      Class<?> startClass = MetaConfigWithOverriddenAttributesTestCase.class;
      Class<ContextConfiguration> annotationType = ContextConfiguration.class;

      UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
              startClass, Service.class, ContextConfiguration.class, Order.class, Transactional.class);

      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(startClass);
      assertThat(descriptor.getAnnotationType()).isEqualTo(annotationType);
      assertThat(((ContextConfiguration) descriptor.getAnnotation()).value()).isEmpty();
      assertThat(((ContextConfiguration) descriptor.getAnnotation()).classes())
              .containsExactly(TestContextAnnotationUtilsTests.class);
    }

    @Test
    void findAnnotationDescriptorForTypesForInterfaceWithMetaAnnotation() {
      Class<?> startClass = InterfaceWithMetaAnnotation.class;
      assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta1", Meta1.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesForClassWithMetaAnnotatedInterface() {
      Component rawAnnotation = AnnotationUtils.findAnnotation(ClassWithMetaAnnotatedInterface.class, Component.class);

      UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
              ClassWithMetaAnnotatedInterface.class, Service.class, Component.class, Order.class, Transactional.class);

      assertThat(descriptor).isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).isEqualTo(ClassWithMetaAnnotatedInterface.class);
      assertThat(descriptor.getDeclaringClass()).isEqualTo(Meta1.class);
      assertThat(descriptor.getAnnotation()).isEqualTo(rawAnnotation);
    }

    @Test
    void findAnnotationDescriptorForTypesForClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
      Class<?> startClass = ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class;
      assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(startClass, "meta2", Meta2.class);
    }

    @Test
    void findAnnotationDescriptorForTypesForSubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface() {
      assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
              SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class,
              ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface.class, "meta2", Meta2.class);
    }

    /**
     * @since 4.0
     */
    @Test
    void findAnnotationDescriptorForTypesOnMetaMetaAnnotatedClass() {
      Class<?> startClass = MetaMetaAnnotatedClass.class;
      assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
              startClass, startClass, Meta2.class, "meta2");
    }

    /**
     * @since 4.0
     */
    @Test
    void findAnnotationDescriptorForTypesOnMetaMetaMetaAnnotatedClass() {
      Class<?> startClass = MetaMetaMetaAnnotatedClass.class;
      assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
              startClass, startClass, Meta2.class, "meta2");
    }

    /**
     * @since 4.0
     */
    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesOnAnnotatedClassWithMissingTargetMetaAnnotation() {
      // InheritedAnnotationClass is NOT annotated or meta-annotated with @Component,
      // @Service, or @Order, but it is annotated with @Transactional.
      UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
              InheritedAnnotationClass.class, Service.class, Component.class, Order.class);
      assertThat(descriptor).as("Should not find @Component on InheritedAnnotationClass").isNull();
    }

    /**
     * @since 4.0
     */
    @Test
    @SuppressWarnings("unchecked")
    void findAnnotationDescriptorForTypesOnMetaCycleAnnotatedClassWithMissingTargetMetaAnnotation() {
      UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
              MetaCycleAnnotatedClass.class, Service.class, Component.class, Order.class);
      assertThat(descriptor).as("Should not find @Component on MetaCycleAnnotatedClass").isNull();
    }

    private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
            Class<?> startClass, String name, Class<? extends Annotation> composedAnnotationType) {

      assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
              startClass, startClass, name, composedAnnotationType);
    }

    private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Class<?> startClass,
            Class<?> rootDeclaringClass, String name, Class<? extends Annotation> composedAnnotationType) {

      assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(
              startClass, rootDeclaringClass, composedAnnotationType, name);
    }

    @SuppressWarnings("unchecked")
    private void assertAtComponentOnComposedAnnotationForMultipleCandidateTypes(Class<?> startClass,
            Class<?> rootDeclaringClass, Class<?> declaringClass, String name) {

      Class<Component> annotationType = Component.class;
      UntypedAnnotationDescriptor descriptor = findAnnotationDescriptorForTypes(
              startClass, Service.class, annotationType, Order.class, Transactional.class);

      assertThat(descriptor).as("UntypedAnnotationDescriptor should not be null").isNotNull();
      assertThat(descriptor.getRootDeclaringClass()).as("rootDeclaringClass").isEqualTo(rootDeclaringClass);
      assertThat(descriptor.getDeclaringClass()).as("declaringClass").isEqualTo(declaringClass);
      assertThat(descriptor.getAnnotationType()).as("annotationType").isEqualTo(annotationType);
      assertThat(((Component) descriptor.getAnnotation()).value()).as("component name").contains(name);
    }

  }

  // -------------------------------------------------------------------------

  @Component(value = "meta1")
  @Order
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Meta1 {
  }

  @Component(value = "meta2")
  @Transactional
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface Meta2 {
  }

  @Meta2
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaMeta {
  }

  @MetaMeta
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaMetaMeta {
  }

  @MetaCycle3
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  @interface MetaCycle1 {
  }

  @MetaCycle1
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  @interface MetaCycle2 {
  }

  @MetaCycle2
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaCycle3 {
  }

  @ContextConfiguration
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface MetaConfig {

    class DevConfig {
    }

    class ProductionConfig {
    }

    Class<?>[] classes() default { DevConfig.class, ProductionConfig.class };
  }

  // -------------------------------------------------------------------------

  @Meta1
  static class HasMetaComponentAnnotation {
  }

  @Meta1
  @Component(value = "local")
  @Meta2
  static class HasLocalAndMetaComponentAnnotation {
  }

  @Meta1
  interface InterfaceWithMetaAnnotation {
  }

  static class ClassWithMetaAnnotatedInterface implements InterfaceWithMetaAnnotation {
  }

  @Meta2
  static class ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface implements InterfaceWithMetaAnnotation {
  }

  static class SubClassWithLocalMetaAnnotationAndMetaAnnotatedInterface extends
          ClassWithLocalMetaAnnotationAndMetaAnnotatedInterface {
  }

  @MetaMeta
  static class MetaMetaAnnotatedClass {
  }

  @MetaMetaMeta
  static class MetaMetaMetaAnnotatedClass {
  }

  @MetaCycle3
  static class MetaCycleAnnotatedClass {
  }

  @MetaConfig
  static class MetaConfigWithDefaultAttributesTestCase {
  }

  @MetaConfig(classes = TestContextAnnotationUtilsTests.class)
  static class MetaConfigWithOverriddenAttributesTestCase {
  }

  // -------------------------------------------------------------------------

  @Transactional
  interface InheritedAnnotationInterface {
  }

  interface SubInheritedAnnotationInterface extends InheritedAnnotationInterface {
  }

  interface SubSubInheritedAnnotationInterface extends SubInheritedAnnotationInterface {
  }

  @Order
  interface NonInheritedAnnotationInterface {
  }

  interface SubNonInheritedAnnotationInterface extends NonInheritedAnnotationInterface {
  }

  static class NonAnnotatedClass {
  }

  interface NonAnnotatedInterface {
  }

  @Transactional
  static class InheritedAnnotationClass {
  }

  static class SubInheritedAnnotationClass extends InheritedAnnotationClass {
  }

  @Order
  static class NonInheritedAnnotationClass {
  }

  static class SubNonInheritedAnnotationClass extends NonInheritedAnnotationClass {
  }

  @ContextConfiguration(classes = Number.class)
  static class AnnotatedContextConfigClass {
  }

  @MetaConfig(classes = String.class)
  static class MetaAnnotatedAndSuperAnnotatedContextConfigClass extends AnnotatedContextConfigClass {
  }

  static class OuterTestCase {
    class NestedTestCase {
      class DoubleNestedTestCase {
      }
    }
  }

}
