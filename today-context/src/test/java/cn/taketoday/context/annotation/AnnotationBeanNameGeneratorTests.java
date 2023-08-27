/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.SimpleBeanDefinitionRegistry;
import cn.taketoday.stereotype.Component;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.stereotype.Service;
import example.scannable.DefaultNamedComponent;
import example.scannable.JakartaManagedBeanComponent;
import example.scannable.JakartaNamedComponent;
import example.scannable.JavaxManagedBeanComponent;
import example.scannable.JavaxNamedComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/8 14:13
 */
class AnnotationBeanNameGeneratorTests {

  private final BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

  private final AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

  @Test
  void buildDefaultBeanName() {
    BeanDefinition bd = annotatedBeanDef(ComponentFromNonStringMeta.class);
    assertThat(this.beanNameGenerator.buildDefaultBeanName(bd, this.registry))
            .isEqualTo("annotationBeanNameGeneratorTests.ComponentFromNonStringMeta");
  }

  @Test
  void generateBeanNameWithNamedComponent() {
    assertGeneratedName(ComponentWithName.class, "walden");
  }

  @Test
  void generateBeanNameWithNamedComponentWhereTheNameIsBlank() {
    assertGeneratedNameIsDefault(ComponentWithBlankName.class);
  }

  @Test
  void generateBeanNameForComponentWithDuplicateIdenticalNames() {
    assertGeneratedName(ComponentWithDuplicateIdenticalNames.class, "myComponent");
  }

  @Test
  void generateBeanNameForComponentWithConflictingNames() {
    BeanDefinition bd = annotatedBeanDef(ComponentWithMultipleConflictingNames.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> generateBeanName(bd))
            .withMessage("Stereotype annotations suggest inconsistent component names: '%s' versus '%s'",
                    "myComponent", "myService");
  }

  @Test
  void generateBeanNameWithJakartaNamedComponent() {
    assertGeneratedName(JakartaNamedComponent.class, "myJakartaNamedComponent");
  }

  @Test
  void generateBeanNameWithJavaxNamedComponent() {
    assertGeneratedName(JavaxNamedComponent.class, "myJavaxNamedComponent");
  }

  @Test
  void generateBeanNameWithJakartaManagedBeanComponent() {
    assertGeneratedName(JakartaManagedBeanComponent.class, "myJakartaManagedBeanComponent");
  }

  @Test
  void generateBeanNameWithJavaxManagedBeanComponent() {
    assertGeneratedName(JavaxManagedBeanComponent.class, "myJavaxManagedBeanComponent");
  }

  @Test
  void generateBeanNameWithCustomStereotypeComponent() {
    assertGeneratedName(DefaultNamedComponent.class, "thoreau");
  }

  @Test
  void generateBeanNameWithAnonymousComponentYieldsGeneratedBeanName() {
    assertGeneratedNameIsDefault(AnonymousComponent.class);
  }

  @Test
  void generateBeanNameFromMetaComponentWithStringValue() {
    assertGeneratedName(ComponentFromStringMeta.class, "henry");
  }

  @Test
  void generateBeanNameFromMetaComponentWithNonStringValue() {
    assertGeneratedNameIsDefault(ComponentFromNonStringMeta.class);
  }

  @Test
    // SPR-11360
  void generateBeanNameFromComposedControllerAnnotationWithoutName() {
    assertGeneratedNameIsDefault(ComposedControllerAnnotationWithoutName.class);
  }

  @Test
    // SPR-11360
  void generateBeanNameFromComposedControllerAnnotationWithBlankName() {
    assertGeneratedNameIsDefault(ComposedControllerAnnotationWithBlankName.class);
  }

  @Test
    // SPR-11360
  void generateBeanNameFromComposedControllerAnnotationWithStringValue() {
    assertGeneratedName(ComposedControllerAnnotationWithStringValue.class, "restController");
  }

  private void assertGeneratedName(Class<?> clazz, String expectedName) {
    BeanDefinition bd = annotatedBeanDef(clazz);
    assertThat(generateBeanName(bd)).isNotBlank().isEqualTo(expectedName);
  }

  private void assertGeneratedNameIsDefault(Class<?> clazz) {
    BeanDefinition bd = annotatedBeanDef(clazz);
    String expectedName = this.beanNameGenerator.buildDefaultBeanName(bd);
    assertThat(generateBeanName(bd)).isNotBlank().isEqualTo(expectedName);
  }

  private AnnotatedBeanDefinition annotatedBeanDef(Class<?> clazz) {
    return new AnnotatedGenericBeanDefinition(clazz);
  }

  private String generateBeanName(BeanDefinition bd) {
    return this.beanNameGenerator.generateBeanName(bd, registry);
  }

  @Component("walden")
  private static class ComponentWithName {
  }

  @Component(" ")
  private static class ComponentWithBlankName {
  }

  @Component("myComponent")
  @Service("myComponent")
  static class ComponentWithDuplicateIdenticalNames {
  }

  @Component("myComponent")
  @Service("myService")
  static class ComponentWithMultipleConflictingNames {
  }

  @Component
  private static class AnonymousComponent {
  }

  @Service("henry")
  private static class ComponentFromStringMeta {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Component
  @interface NonStringMetaComponent {

    long value();
  }

  @NonStringMetaComponent(123)
  private static class ComponentFromNonStringMeta {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Controller
  @interface TestRestController {

    String value() default "";
  }

  @TestRestController
  static class ComposedControllerAnnotationWithoutName {
  }

  @TestRestController(" ")
  static class ComposedControllerAnnotationWithBlankName {
  }

  @TestRestController("restController")
  static class ComposedControllerAnnotationWithStringValue {
  }

}
