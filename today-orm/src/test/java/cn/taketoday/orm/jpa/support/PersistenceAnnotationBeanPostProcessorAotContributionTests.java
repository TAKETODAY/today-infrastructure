/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.orm.jpa.support;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.BiConsumer;

import cn.taketoday.aot.hint.FieldHint;
import cn.taketoday.aot.hint.TypeReference;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.test.tools.CompileWithForkedClassLoader;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceProperty;
import jakarta.persistence.PersistenceUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PersistenceAnnotationBeanPostProcessor} AOT contribution.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@CompileWithForkedClassLoader
class PersistenceAnnotationBeanPostProcessorAotContributionTests {

  private StandardBeanFactory beanFactory = new StandardBeanFactory();

  private TestGenerationContext generationContext;

  @BeforeEach
  void setup() {
    this.beanFactory = new StandardBeanFactory();
    this.generationContext = new TestGenerationContext();
  }

  @Test
  void processAheadOfTimeWhenPersistenceUnitOnFieldAndPropertyValueSet() {
    RegisteredBean registeredBean = registerBean(DefaultPersistenceUnitField.class);
    registeredBean.getMergedBeanDefinition().getPropertyValues().add("emf", "myEntityManagerFactory");
    assertThat(processAheadOfTime(registeredBean)).isNotNull(); // Field not handled by property values
  }

  @Test
  void processAheadOfTimeWhenPersistenceUnitOnMethodAndPropertyValueSet() {
    RegisteredBean registeredBean = registerBean(DefaultPersistenceUnitMethod.class);
    registeredBean.getMergedBeanDefinition().getPropertyValues().add("emf", "myEntityManagerFactory");
    assertThat(processAheadOfTime(registeredBean)).isNull();
  }

  @Test
  void processAheadOfTimeWhenPersistenceUnitOnPublicField() {
    RegisteredBean registeredBean = registerBean(DefaultPersistenceUnitField.class);
    testCompile(registeredBean, (actual, compiled) -> {
      EntityManagerFactory entityManagerFactory = mock();
      this.beanFactory.registerSingleton("entityManagerFactory",
              entityManagerFactory);
      DefaultPersistenceUnitField instance = new DefaultPersistenceUnitField();
      actual.accept(registeredBean, instance);
      assertThat(instance).extracting("emf").isSameAs(entityManagerFactory);
      assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
              .isEmpty();
    });
  }

  @Test
  void processAheadOfTimeWhenPersistenceUnitOnPublicSetter() {
    RegisteredBean registeredBean = registerBean(DefaultPersistenceUnitMethod.class);
    testCompile(registeredBean, (actual, compiled) -> {
      EntityManagerFactory entityManagerFactory = mock();
      this.beanFactory.registerSingleton("entityManagerFactory",
              entityManagerFactory);
      DefaultPersistenceUnitMethod instance = new DefaultPersistenceUnitMethod();
      actual.accept(registeredBean, instance);
      assertThat(instance).extracting("emf").isSameAs(entityManagerFactory);
      assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
              .isEmpty();
    });
  }

  @Test
  void processAheadOfTimeWhenCustomPersistenceUnitOnPublicSetter() {
    RegisteredBean registeredBean = registerBean(
            CustomUnitNamePublicPersistenceUnitMethod.class);
    testCompile(registeredBean, (actual, compiled) -> {
      EntityManagerFactory entityManagerFactory = mock();
      this.beanFactory.registerSingleton("custom", entityManagerFactory);
      CustomUnitNamePublicPersistenceUnitMethod instance = new CustomUnitNamePublicPersistenceUnitMethod();
      actual.accept(registeredBean, instance);
      assertThat(instance).extracting("emf").isSameAs(entityManagerFactory);
      assertThat(compiled.getSourceFile()).contains(
              "findEntityManagerFactory(registeredBean.getBeanFactory(), \"custom\")");
      assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
              .isEmpty();
    });
  }

  @Test
  void processAheadOfTimeWhenPersistenceContextOnPrivateField() {
    RegisteredBean registeredBean = registerBean(
            DefaultPersistenceContextField.class);
    testCompile(registeredBean, (actual, compiled) -> {
      EntityManagerFactory entityManagerFactory = mock();
      this.beanFactory.registerSingleton("entityManagerFactory",
              entityManagerFactory);
      DefaultPersistenceContextField instance = new DefaultPersistenceContextField();
      actual.accept(registeredBean, instance);
      assertThat(instance).extracting("entityManager").isNotNull();
      assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
              .singleElement().satisfies(typeHint -> {
                assertThat(typeHint.getType()).isEqualTo(
                        TypeReference.of(DefaultPersistenceContextField.class));
                assertThat(typeHint.fields()).singleElement().satisfies(fieldHint ->
                        assertThat(fieldHint.getName()).isEqualTo("entityManager"));
              });
    });
  }

  @Test
  void processAheadOfTimeWhenPersistenceContextWithCustomPropertiesOnMethod() {
    RegisteredBean registeredBean = registerBean(
            CustomPropertiesPersistenceContextMethod.class);
    testCompile(registeredBean, (actual, compiled) -> {
      EntityManagerFactory entityManagerFactory = mock();
      this.beanFactory.registerSingleton("entityManagerFactory",
              entityManagerFactory);
      CustomPropertiesPersistenceContextMethod instance = new CustomPropertiesPersistenceContextMethod();
      actual.accept(registeredBean, instance);
      Field field = ReflectionUtils.findField(
              CustomPropertiesPersistenceContextMethod.class, "entityManager");
      ReflectionUtils.makeAccessible(field);
      EntityManager sharedEntityManager = (EntityManager) ReflectionUtils
              .getField(field, instance);
      InvocationHandler invocationHandler = Proxy
              .getInvocationHandler(sharedEntityManager);
      assertThat(invocationHandler).extracting("properties")
              .asInstanceOf(InstanceOfAssertFactories.MAP)
              .containsEntry("jpa.test", "value")
              .containsEntry("jpa.test2", "value2");
      assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
              .isEmpty();
    });
  }

  @Test
  void processAheadOfTimeWhenPersistenceContextOnPrivateFields() {
    RegisteredBean registeredBean = registerBean(
            SeveralPersistenceContextField.class);
    testCompile(registeredBean, (actual, compiled) -> {
      EntityManagerFactory entityManagerFactory = mock();
      this.beanFactory.registerSingleton("custom", entityManagerFactory);
      this.beanFactory.registerAlias("custom", "another");
      SeveralPersistenceContextField instance = new SeveralPersistenceContextField();
      actual.accept(registeredBean, instance);
      assertThat(instance).extracting("customEntityManager").isNotNull();
      assertThat(instance).extracting("anotherEntityManager").isNotNull();
      assertThat(this.generationContext.getRuntimeHints().reflection().typeHints())
              .singleElement().satisfies(typeHint -> {
                assertThat(typeHint.getType()).isEqualTo(
                        TypeReference.of(SeveralPersistenceContextField.class));
                assertThat(typeHint.fields().map(FieldHint::getName))
                        .containsOnly("customEntityManager", "anotherEntityManager");
              });
    });
  }

  private RegisteredBean registerBean(Class<?> beanClass) {
    String beanName = "testBean";
    this.beanFactory.registerBeanDefinition(beanName,
            new RootBeanDefinition(beanClass));
    return RegisteredBean.of(this.beanFactory, beanName);
  }

  private void testCompile(RegisteredBean registeredBean,
          BiConsumer<BiConsumer<RegisteredBean, Object>, Compiled> result) {
    BeanRegistrationAotContribution contribution = processAheadOfTime(registeredBean);
    BeanRegistrationCode beanRegistrationCode = mock();
    contribution.applyTo(generationContext, beanRegistrationCode);
    generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(generationContext)
            .compile(compiled -> result.accept(new Invoker(compiled), compiled));
  }

  @Nullable
  private BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    PersistenceAnnotationBeanPostProcessor postProcessor = new PersistenceAnnotationBeanPostProcessor();
    return postProcessor.processAheadOfTime(registeredBean);
  }

  static class Invoker implements BiConsumer<RegisteredBean, Object> {

    private Compiled compiled;

    Invoker(Compiled compiled) {
      this.compiled = compiled;
    }

    @Override
    public void accept(RegisteredBean registeredBean, Object instance) {
      List<Class<?>> compiledClasses = compiled.getAllCompiledClasses();
      assertThat(compiledClasses).hasSize(1);
      Class<?> compiledClass = compiledClasses.get(0);
      for (Method method : ReflectionUtils.getDeclaredMethods(compiledClass)) {
        if (method.getName().equals("apply")) {
          ReflectionUtils.invokeMethod(method, null, registeredBean, instance);
          return;
        }
      }
      throw new IllegalStateException("Did not find apply method");
    }

  }

  static class DefaultPersistenceUnitField {

    @PersistenceUnit
    public EntityManagerFactory emf;

  }

  static class DefaultPersistenceUnitMethod {

    @SuppressWarnings("unused")
    private EntityManagerFactory emf;

    @PersistenceUnit
    public void setEmf(EntityManagerFactory emf) {
      this.emf = emf;
    }

  }

  static class CustomUnitNamePublicPersistenceUnitMethod {

    @SuppressWarnings("unused")
    private EntityManagerFactory emf;

    @PersistenceUnit(unitName = "custom")
    public void setEmf(EntityManagerFactory emf) {
      this.emf = emf;
    }

  }

  static class DefaultPersistenceContextField {

    @SuppressWarnings("unused")
    @PersistenceContext
    private EntityManager entityManager;

  }

  static class CustomPropertiesPersistenceContextMethod {

    @SuppressWarnings("unused")
    private EntityManager entityManager;

    @PersistenceContext(
            properties = { @PersistenceProperty(name = "jpa.test", value = "value"),
                    @PersistenceProperty(name = "jpa.test2", value = "value2") })
    public void setEntityManager(EntityManager entityManager) {
      this.entityManager = entityManager;
    }

  }

  static class SeveralPersistenceContextField {

    @SuppressWarnings("unused")
    @PersistenceContext(name = "custom")
    private EntityManager customEntityManager;

    @SuppressWarnings("unused")
    @PersistenceContext(name = "another")
    private EntityManager anotherEntityManager;

  }

}
