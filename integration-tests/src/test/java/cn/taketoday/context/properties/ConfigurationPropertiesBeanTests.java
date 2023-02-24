/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationDescription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.lang.reflect.Constructor;
import java.util.Map;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportSelector;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.properties.ConfigurationPropertiesBean.BindMethod;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.ConstructorBinding;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.stereotype.Component;
import cn.taketoday.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigurationPropertiesBean}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class ConfigurationPropertiesBeanTests {

  @Test
  void getAllReturnsAll() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            NonAnnotatedComponent.class, AnnotatedComponent.class, AnnotatedBeanConfiguration.class,
            ValueObjectConfiguration.class);
    Map<String, ConfigurationPropertiesBean> all = ConfigurationPropertiesBean.getAll(context);
    assertThat(all).containsOnlyKeys("annotatedComponent", "annotatedBean", ValueObject.class.getName());
    ConfigurationPropertiesBean component = all.get("annotatedComponent");
    assertThat(component.getName()).isEqualTo("annotatedComponent");
    assertThat(component.getInstance()).isInstanceOf(AnnotatedComponent.class);
    assertThat(component.getAnnotation()).isNotNull();
    assertThat(component.getType()).isEqualTo(AnnotatedComponent.class);
    assertThat(component.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
    ConfigurationPropertiesBean bean = all.get("annotatedBean");
    assertThat(bean.getName()).isEqualTo("annotatedBean");
    assertThat(bean.getInstance()).isInstanceOf(AnnotatedBean.class);
    assertThat(bean.getType()).isEqualTo(AnnotatedBean.class);
    assertThat(bean.getAnnotation()).isNotNull();
    assertThat(bean.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
    ConfigurationPropertiesBean valueObject = all.get(ValueObject.class.getName());
    assertThat(valueObject.getName()).isEqualTo(ValueObject.class.getName());
    assertThat(valueObject.getInstance()).isInstanceOf(ValueObject.class);
    assertThat(valueObject.getType()).isEqualTo(ValueObject.class);
    assertThat(valueObject.getAnnotation()).isNotNull();
    assertThat(valueObject.getBindMethod()).isEqualTo(BindMethod.VALUE_OBJECT);
    context.close();
  }

  @Test
  void getAllDoesNotFindABeanDeclaredInAStaticBeanMethodOnAConfigurationAndConfigurationPropertiesAnnotatedClass() {
    var context = new AnnotationConfigApplicationContext(StaticBeanMethodConfiguration.class);
    Map<String, ConfigurationPropertiesBean> all = ConfigurationPropertiesBean.getAll(context);
    assertThat(all).containsOnlyKeys("configurationPropertiesBeanTests.StaticBeanMethodConfiguration");
    context.close();
  }

  @Test
  void getAllWhenHasBadBeanDoesNotFail() {
    var context = new AnnotationConfigApplicationContext(
            NonAnnotatedComponent.class, AnnotatedComponent.class, AnnotatedBeanConfiguration.class,
            ValueObjectConfiguration.class, BadBeanConfiguration.class);
    Map<String, ConfigurationPropertiesBean> all = ConfigurationPropertiesBean.getAll(context);
    assertThat(all).isNotEmpty();
    context.close();
  }

  @Test
  void getWhenNotAnnotatedReturnsNull() throws Throwable {
    get(NonAnnotatedComponent.class, "nonAnnotatedComponent",
            (propertiesBean) -> assertThat(propertiesBean).isNull());
  }

  @Test
  void getWhenBeanIsAnnotatedReturnsBean() throws Throwable {
    get(AnnotatedComponent.class, "annotatedComponent", (propertiesBean) -> {
      assertThat(propertiesBean).isNotNull();
      assertThat(propertiesBean.getName()).isEqualTo("annotatedComponent");
      assertThat(propertiesBean.getInstance()).isInstanceOf(AnnotatedComponent.class);
      assertThat(propertiesBean.getType()).isEqualTo(AnnotatedComponent.class);
      assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
      assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
    });
  }

  @Test
  void getWhenFactoryMethodIsAnnotatedReturnsBean() throws Throwable {
    get(NonAnnotatedBeanConfiguration.class, "nonAnnotatedBean", (propertiesBean) -> {
      assertThat(propertiesBean).isNotNull();
      assertThat(propertiesBean.getName()).isEqualTo("nonAnnotatedBean");
      assertThat(propertiesBean.getInstance()).isInstanceOf(NonAnnotatedBean.class);
      assertThat(propertiesBean.getType()).isEqualTo(NonAnnotatedBean.class);
      assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
      assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
    });
  }

  @Test
  void getWhenImportedFactoryMethodIsAnnotatedAndMetadataCachingIsDisabledReturnsBean() throws Throwable {
    getWithoutBeanMetadataCaching(NonAnnotatedBeanImportConfiguration.class, "nonAnnotatedBean",
            (propertiesBean) -> {
              assertThat(propertiesBean).isNotNull();
              assertThat(propertiesBean.getName()).isEqualTo("nonAnnotatedBean");
              assertThat(propertiesBean.getInstance()).isInstanceOf(NonAnnotatedBean.class);
              assertThat(propertiesBean.getType()).isEqualTo(NonAnnotatedBean.class);
              assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
              assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
            });
  }

  @Test
  void getWhenImportedFactoryMethodIsAnnotatedReturnsBean() throws Throwable {
    get(NonAnnotatedBeanImportConfiguration.class, "nonAnnotatedBean", (propertiesBean) -> {
      assertThat(propertiesBean).isNotNull();
      assertThat(propertiesBean.getName()).isEqualTo("nonAnnotatedBean");
      assertThat(propertiesBean.getInstance()).isInstanceOf(NonAnnotatedBean.class);
      assertThat(propertiesBean.getType()).isEqualTo(NonAnnotatedBean.class);
      assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("prefix");
      assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
    });
  }

  @Test
  void getWhenHasFactoryMethodBindsUsingMethodReturnType() throws Throwable {
    get(NonAnnotatedGenericBeanConfiguration.class, "nonAnnotatedGenericBean", (propertiesBean) -> {
      assertThat(propertiesBean.getType()).isEqualTo(NonAnnotatedGenericBean.class);
      assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
      ResolvableType type = propertiesBean.asBindTarget().getType();
      assertThat(type.resolve()).isEqualTo(NonAnnotatedGenericBean.class);
      assertThat(type.resolveGeneric(0)).isEqualTo(String.class);
    });
  }

  @Test
  void getWhenHasFactoryMethodWithoutAnnotationBindsUsingMethodType() throws Throwable {
    get(AnnotatedGenericBeanConfiguration.class, "annotatedGenericBean", (propertiesBean) -> {
      assertThat(propertiesBean.getType()).isEqualTo(AnnotatedGenericBean.class);
      assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
      ResolvableType type = propertiesBean.asBindTarget().getType();
      assertThat(type.resolve()).isEqualTo(AnnotatedGenericBean.class);
      assertThat(type.resolveGeneric(0)).isEqualTo(String.class);
    });
  }

  @Test
  void getWhenHasNoFactoryMethodBindsUsingObjectType() throws Throwable {
    get(AnnotatedGenericComponent.class, "annotatedGenericComponent", (propertiesBean) -> {
      assertThat(propertiesBean.getType()).isEqualTo(AnnotatedGenericComponent.class);
      assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.JAVA_BEAN);
      ResolvableType type = propertiesBean.asBindTarget().getType();
      assertThat(type.resolve()).isEqualTo(AnnotatedGenericComponent.class);
      assertThat(type.getGeneric(0).resolve()).isNull();
    });
  }

  @Test
  void getWhenHasFactoryMethodAndBeanAnnotationFavorsFactoryMethod() throws Throwable {
    get(AnnotatedBeanConfiguration.class, "annotatedBean",
            (propertiesBean) -> assertThat(propertiesBean.getAnnotation().prefix()).isEqualTo("factory"));
  }

  @Test
  void getWhenHasValidatedBeanBindsWithBeanAnnotation() throws Throwable {
    get(ValidatedBeanConfiguration.class, "validatedBean", (propertiesBean) -> {
      Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
      assertThat(validated.value()).containsExactly(BeanGroup.class);
    });
  }

  @Test
  void getWhenHasValidatedFactoryMethodBindsWithFactoryMethodAnnotation() throws Throwable {
    get(ValidatedMethodConfiguration.class, "annotatedBean", (propertiesBean) -> {
      Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
      assertThat(validated.value()).containsExactly(FactoryMethodGroup.class);
    });
  }

  @Test
  void getWhenHasValidatedBeanAndFactoryMethodBindsWithFactoryMethodAnnotation() throws Throwable {
    get(ValidatedMethodAndBeanConfiguration.class, "validatedBean", (propertiesBean) -> {
      Validated validated = propertiesBean.asBindTarget().getAnnotation(Validated.class);
      assertThat(validated.value()).containsExactly(FactoryMethodGroup.class);
    });
  }

  @Test
  void forValueObjectWithConstructorBindingAnnotatedClassReturnsBean() {
    ConfigurationPropertiesBean propertiesBean = ConfigurationPropertiesBean
            .forValueObject(ConstructorBindingOnConstructor.class, "valueObjectBean");
    assertThat(propertiesBean.getName()).isEqualTo("valueObjectBean");
    assertThat(propertiesBean.getInstance()).isNull();
    assertThat(propertiesBean.getType()).isEqualTo(ConstructorBindingOnConstructor.class);
    assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.VALUE_OBJECT);
    assertThat(propertiesBean.getAnnotation()).isNotNull();
    Bindable<?> target = propertiesBean.asBindTarget();
    assertThat(target.getType()).isEqualTo(ResolvableType.fromClass(ConstructorBindingOnConstructor.class));
    assertThat(target.getValue()).isNull();
    assertThat(ConfigurationPropertiesBindConstructorProvider.INSTANCE
            .getBindConstructor(ConstructorBindingOnConstructor.class, false)).isNotNull();
  }

  @Test
  void forValueObjectWithRecordReturnsBean() {
    Class<?> implicitConstructorBinding = new ByteBuddy(ClassFileVersion.JAVA_V16).makeRecord()
            .name("cn.taketoday.context.properties.ImplicitConstructorBinding")
            .annotateType(AnnotationDescription.Builder.ofType(ConfigurationProperties.class)
                    .define("prefix", "implicit").build())
            .defineRecordComponent("someString", String.class).defineRecordComponent("someInteger", Integer.class)
            .make().load(getClass().getClassLoader()).getLoaded();
    ConfigurationPropertiesBean propertiesBean = ConfigurationPropertiesBean
            .forValueObject(implicitConstructorBinding, "implicitBindingRecord");
    assertThat(propertiesBean.getName()).isEqualTo("implicitBindingRecord");
    assertThat(propertiesBean.getInstance()).isNull();
    assertThat(propertiesBean.getType()).isEqualTo(implicitConstructorBinding);
    assertThat(propertiesBean.getBindMethod()).isEqualTo(BindMethod.VALUE_OBJECT);
    assertThat(propertiesBean.getAnnotation()).isNotNull();
    Bindable<?> target = propertiesBean.asBindTarget();
    assertThat(target.getType()).isEqualTo(ResolvableType.fromClass(implicitConstructorBinding));
    assertThat(target.getValue()).isNull();
    Constructor<?> bindConstructor = ConfigurationPropertiesBindConstructorProvider.INSTANCE
            .getBindConstructor(implicitConstructorBinding, false);
    assertThat(bindConstructor).isNotNull();
    assertThat(bindConstructor.getParameterTypes()).containsExactly(String.class, Integer.class);
  }

  @Test
  void forValueObjectWhenJavaBeanBindTypeThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> ConfigurationPropertiesBean.forValueObject(AnnotatedBean.class, "annotatedBean"))
            .withMessage("Bean 'annotatedBean' is not a @ConfigurationProperties value object");
    assertThatIllegalStateException()
            .isThrownBy(
                    () -> ConfigurationPropertiesBean.forValueObject(NonAnnotatedBean.class, "nonAnnotatedBean"))
            .withMessage("Bean 'nonAnnotatedBean' is not a @ConfigurationProperties value object");

  }

  @Test
  void bindTypeForTypeWhenNoConstructorBindingReturnsJavaBean() {
    BindMethod bindType = BindMethod.forType(NoConstructorBinding.class);
    assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
  }

  @Test
  void bindTypeForTypeWhenConstructorBindingOnConstructorReturnsValueObject() {
    BindMethod bindType = BindMethod.forType(ConstructorBindingOnConstructor.class);
    assertThat(bindType).isEqualTo(BindMethod.VALUE_OBJECT);
  }

  @Test
  void bindTypeForTypeWhenNoConstructorBindingAnnotationOnSingleParameterizedConstructorReturnsValueObject() {
    BindMethod bindType = BindMethod.forType(ConstructorBindingNoAnnotation.class);
    assertThat(bindType).isEqualTo(BindMethod.VALUE_OBJECT);
  }

  @Test
  void bindTypeForTypeWhenConstructorBindingOnMultipleConstructorsThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> BindMethod.forType(ConstructorBindingOnMultipleConstructors.class))
            .withMessage(ConstructorBindingOnMultipleConstructors.class.getName()
                    + " has more than one @ConstructorBinding constructor");
  }

  @Test
  void bindTypeForTypeWithMultipleConstructorsReturnJavaBean() {
    BindMethod bindType = BindMethod.forType(NoConstructorBindingOnMultipleConstructors.class);
    assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
  }

  @Test
  void bindTypeForTypeWithNoArgConstructorReturnsJavaBean() {
    BindMethod bindType = BindMethod.forType(JavaBeanWithNoArgConstructor.class);
    assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
  }

  @Test
  void bindTypeForTypeWithSingleArgAutowiredConstructorReturnsJavaBean() {
    BindMethod bindType = BindMethod.forType(JavaBeanWithAutowiredConstructor.class);
    assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
  }

  @Test
  void constructorBindingAndAutowiredConstructorsShouldThrowException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> BindMethod.forType(ConstructorBindingAndAutowiredConstructors.class));
  }

  @Test
  void innerClassWithSyntheticFieldShouldReturnJavaBean() {
    BindMethod bindType = BindMethod.forType(Inner.class);
    assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
  }

  @Test
  void innerClassWithParameterizedConstructorShouldReturnJavaBean() {
    BindMethod bindType = BindMethod.forType(ParameterizedConstructorInner.class);
    assertThat(bindType).isEqualTo(BindMethod.JAVA_BEAN);
  }

  private void get(Class<?> configuration, String beanName, ThrowingConsumer<ConfigurationPropertiesBean> consumer)
          throws Throwable {
    get(configuration, beanName, true, consumer);
  }

  private void getWithoutBeanMetadataCaching(Class<?> configuration, String beanName,
          ThrowingConsumer<ConfigurationPropertiesBean> consumer) throws Throwable {
    get(configuration, beanName, false, consumer);
  }

  private void get(Class<?> configuration, String beanName, boolean cacheBeanMetadata,
          ThrowingConsumer<ConfigurationPropertiesBean> consumer) throws Throwable {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().setCacheBeanMetadata(cacheBeanMetadata);
    context.register(configuration);
    context.refresh();
    Object bean = context.getBean(beanName);
    consumer.accept(ConfigurationPropertiesBean.get(context, bean, beanName));
    context.close();
  }

  @Component("nonAnnotatedComponent")
  static class NonAnnotatedComponent {

  }

  @Component("annotatedComponent")
  @ConfigurationProperties(prefix = "prefix")
  static class AnnotatedComponent {

  }

  @ConfigurationProperties(prefix = "prefix")
  static class AnnotatedBean {

  }

  static class NonAnnotatedBean {

  }

  static class NonAnnotatedGenericBean<T> {

  }

  @ConfigurationProperties
  static class AnnotatedGenericBean<T> {

  }

  @Component("annotatedGenericComponent")
  @ConfigurationProperties
  static class AnnotatedGenericComponent<T> {

  }

  @Validated(BeanGroup.class)
  @ConfigurationProperties
  static class ValidatedBean {

  }

  @Configuration(proxyBeanMethods = false)
  static class NonAnnotatedBeanConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "prefix")
    NonAnnotatedBean nonAnnotatedBean() {
      return new NonAnnotatedBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class NonAnnotatedGenericBeanConfiguration {

    @Bean
    @ConfigurationProperties
    NonAnnotatedGenericBean<String> nonAnnotatedGenericBean() {
      return new NonAnnotatedGenericBean<>();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class AnnotatedGenericBeanConfiguration {

    @Bean
    AnnotatedGenericBean<String> annotatedGenericBean() {
      return new AnnotatedGenericBean<>();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class AnnotatedBeanConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "factory")
    AnnotatedBean annotatedBean() {
      return new AnnotatedBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ValidatedBeanConfiguration {

    @Bean
    ValidatedBean validatedBean() {
      return new ValidatedBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ValidatedMethodConfiguration {

    @Bean
    @Validated(FactoryMethodGroup.class)
    AnnotatedBean annotatedBean() {
      return new AnnotatedBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class ValidatedMethodAndBeanConfiguration {

    @Bean
    @Validated(FactoryMethodGroup.class)
    ValidatedBean validatedBean() {
      return new ValidatedBean();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(ValueObject.class)
  static class ValueObjectConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class BadBeanConfiguration {

    @Bean
    @Lazy
    BadBean badBean() {
      return new BadBean();
    }

  }

  static class BadBean {

    BadBean() {
      throw new IllegalStateException();
    }

  }

  @ConfigurationProperties
  static class ValueObject {

    ValueObject(String name) {
    }

  }

  static class BeanGroup {

  }

  static class FactoryMethodGroup {

  }

  @ConfigurationProperties
  static class NoConstructorBinding {

  }

  @ConfigurationProperties
  static class ConstructorBindingNoAnnotation {

    ConstructorBindingNoAnnotation(String name) {
    }

  }

  @ConfigurationProperties
  static class ConstructorBindingOnConstructor {

    ConstructorBindingOnConstructor(String name) {
      this(name, -1);
    }

    @ConstructorBinding
    ConstructorBindingOnConstructor(String name, int age) {
    }

  }

  @ConfigurationProperties
  static class ConstructorBindingOnMultipleConstructors {

    @ConstructorBinding
    ConstructorBindingOnMultipleConstructors(String name) {
      this(name, -1);
    }

    @ConstructorBinding
    ConstructorBindingOnMultipleConstructors(String name, int age) {
    }

  }

  @ConfigurationProperties
  static class NoConstructorBindingOnMultipleConstructors {

    NoConstructorBindingOnMultipleConstructors(String name) {
      this(name, -1);
    }

    NoConstructorBindingOnMultipleConstructors(String name, int age) {
    }

  }

  @ConfigurationProperties
  static class JavaBeanWithAutowiredConstructor {

    @Autowired
    JavaBeanWithAutowiredConstructor(String name) {
    }

  }

  @ConfigurationProperties
  static class JavaBeanWithNoArgConstructor {

    JavaBeanWithNoArgConstructor() {
    }

  }

  @ConfigurationProperties
  static class ConstructorBindingAndAutowiredConstructors {

    @Autowired
    ConstructorBindingAndAutowiredConstructors(String name) {
    }

    @ConstructorBinding
    ConstructorBindingAndAutowiredConstructors(Integer age) {
    }

  }

  @Configuration(proxyBeanMethods = false)
  @Import(NonAnnotatedBeanConfigurationImportSelector.class)
  static class NonAnnotatedBeanImportConfiguration {

  }

  static class NonAnnotatedBeanConfigurationImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
      return new String[] { NonAnnotatedBeanConfiguration.class.getName() };
    }

  }

  @ConfigurationProperties
  class Inner {

  }

  @ConfigurationProperties
  class ParameterizedConstructorInner {

    ParameterizedConstructorInner(Integer age) {
    }

  }

  @Configuration(proxyBeanMethods = false)
  @ConfigurationProperties
  static class StaticBeanMethodConfiguration {

    @Bean
    static String stringBean() {
      return "example";
    }

  }

}
