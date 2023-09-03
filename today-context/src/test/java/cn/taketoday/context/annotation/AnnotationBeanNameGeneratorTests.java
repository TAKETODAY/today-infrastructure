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
import java.util.List;

import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanNameHolder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.SimpleBeanDefinitionRegistry;
import cn.taketoday.core.annotation.AliasFor;
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
  void nameAndAliases() {
    BeanDefinition bd = annotatedBeanDef(NameAndAliases.class);
    assertThat(this.beanNameGenerator.generateBeanName(bd, this.registry))
            .isEqualTo("name");

    assertThat(bd.getAttributes()).containsKey(BeanNameHolder.AttributeName);

    BeanNameHolder holder = BeanNameHolder.find(bd);
    assertThat(holder).isNotNull();

    assertThat(holder.getBeanName()).isEqualTo("name");
    assertThat(holder.getAliases()).contains("aliases", "aliases1");

  }

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
  void generateBeanNameForConventionBasedComponentWithDuplicateIdenticalNames() {
    assertGeneratedName(ConventionBasedComponentWithDuplicateIdenticalNames.class, "myComponent");
  }

  @Test
  void generateBeanNameForComponentWithDuplicateIdenticalNames() {
    assertGeneratedName(ComponentWithDuplicateIdenticalNames.class, "myComponent");
  }

  @Test
  void generateBeanNameForConventionBasedComponentWithConflictingNames() {
    BeanDefinition bd = annotatedBeanDef(ConventionBasedComponentWithMultipleConflictingNames.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> generateBeanName(bd))
            .withMessage("Stereotype annotations suggest inconsistent component names: '%s' versus '%s'",
                    "myComponent", "myService");
  }

  @Test
  void generateBeanNameForComponentWithConflictingNames() {
    BeanDefinition bd = annotatedBeanDef(ComponentWithMultipleConflictingNames.class);
    assertThatIllegalStateException()
            .isThrownBy(() -> generateBeanName(bd))
            .withMessage("Stereotype annotations suggest inconsistent component names: " +
                    List.of("myComponent", "myService"));
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

  @Test
    // gh-31089
  void generateBeanNameFromStereotypeAnnotationWithStringArrayValueAndExplicitComponentNameAlias() {
    assertGeneratedName(ControllerAdviceClass.class, "myControllerAdvice");
  }

  @Test
    // gh-31089
  void generateBeanNameFromSubStereotypeAnnotationWithStringArrayValueAndExplicitComponentNameAlias() {
    assertGeneratedName(RestControllerAdviceClass.class, "myRestControllerAdvice");
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

  static AnnotatedBeanDefinition annotatedBeanDef(Class<?> clazz) {
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

  @Retention(RetentionPolicy.RUNTIME)
  @Component
  @interface ConventionBasedComponent1 {
    // This intentionally convention-based. Please do not add @AliasFor.
    // See gh-31093.
    String value() default "";
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Component
  @interface ConventionBasedComponent2 {
    // This intentionally convention-based. Please do not add @AliasFor.
    // See gh-31093.
    String value() default "";
  }

  @ConventionBasedComponent1("myComponent")
  @ConventionBasedComponent2("myComponent")
  static class ConventionBasedComponentWithDuplicateIdenticalNames {
  }

  @ConventionBasedComponent1("myComponent")
  @ConventionBasedComponent2("myService")
  static class ConventionBasedComponentWithMultipleConflictingNames {
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
  static class ComponentFromNonStringMeta {
  }

  /**
   * @see cn.taketoday.web.annotation.RestController
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Controller
  @interface TestRestController {
    // This intentionally convention-based. Please do not add @AliasFor.
    // See gh-31093.
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

  /**
   * Mock of {@code cn.taketoday.web.bind.annotation.ControllerAdvice},
   * which also has a {@code value} attribute that is NOT a {@code String} that
   * is meant to be used for the component name.
   * <p>Declares a custom {@link #name} that explicitly aliases {@link Component#value()}.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Component
  @interface TestControllerAdvice {

    @AliasFor(annotation = Component.class, attribute = "value")
    String name() default "";

    @AliasFor("basePackages")
    String[] value() default {};

    @AliasFor("value")
    String[] basePackages() default {};
  }

  /**
   * Mock of {@code cn.taketoday.web.bind.annotation.RestControllerAdvice},
   * which also has a {@code value} attribute that is NOT a {@code String} that
   * is meant to be used for the component name.
   * <p>Declares a custom {@link #name} that explicitly aliases
   * {@link TestControllerAdvice#name()} instead of {@link Component#value()}.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @TestControllerAdvice
  @interface TestRestControllerAdvice {

    @AliasFor(annotation = TestControllerAdvice.class)
    String name() default "";

    @AliasFor(annotation = TestControllerAdvice.class)
    String[] value() default {};

    @AliasFor(annotation = TestControllerAdvice.class)
    String[] basePackages() default {};
  }

  @TestControllerAdvice(basePackages = "com.example", name = "myControllerAdvice")
  static class ControllerAdviceClass {
  }

  @TestRestControllerAdvice(basePackages = "com.example", name = "myRestControllerAdvice")
  static class RestControllerAdviceClass {
  }

  @Service({ "name", "", "aliases", "  ", "aliases1" })
  static class NameAndAliases {

  }

}
