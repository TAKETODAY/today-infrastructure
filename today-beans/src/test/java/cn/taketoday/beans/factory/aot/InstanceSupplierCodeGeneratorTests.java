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

package cn.taketoday.beans.factory.aot;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import cn.taketoday.aot.generate.GeneratedClass;
import cn.taketoday.aot.hint.ExecutableHint;
import cn.taketoday.aot.hint.ExecutableMode;
import cn.taketoday.aot.hint.ReflectionHints;
import cn.taketoday.aot.hint.TypeHint;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.InstanceSupplier;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.TestBeanWithPrivateConstructor;
import cn.taketoday.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import cn.taketoday.beans.testfixture.beans.factory.generator.InnerComponentConfiguration;
import cn.taketoday.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import cn.taketoday.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import cn.taketoday.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import cn.taketoday.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import cn.taketoday.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import cn.taketoday.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import cn.taketoday.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.util.ReflectionUtils;

import java.lang.reflect.Executable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InstanceSupplierCodeGenerator}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class InstanceSupplierCodeGeneratorTests {

  private final TestGenerationContext generationContext;

  InstanceSupplierCodeGeneratorTests() {
    this.generationContext = new TestGenerationContext();
  }

  @Test
  void generateWhenHasDefaultConstructor() {
    BeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      TestBean bean = getBean(beanFactory, beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(TestBean.class);
      assertThat(compiled.getSourceFile())
              .contains("InstanceSupplier.using(TestBean::new)");
    });
    assertThat(getReflectionHints().getTypeHint(TestBean.class))
            .satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
  }

  @Test
  void generateWhenHasConstructorWithParameter() {
    BeanDefinition beanDefinition = new RootBeanDefinition(InjectionComponent.class);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("injected", "injected");
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      InjectionComponent bean = getBean(beanFactory, beanDefinition,
              instanceSupplier);
      assertThat(bean).isInstanceOf(InjectionComponent.class).extracting("bean")
              .isEqualTo("injected");
    });
    assertThat(getReflectionHints().getTypeHint(InjectionComponent.class))
            .satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
  }

  @Test
  void generateWhenHasConstructorWithInnerClassAndDefaultConstructor() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(
            NoDependencyComponent.class);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      NoDependencyComponent bean = getBean(beanFactory, beanDefinition,
              instanceSupplier);
      assertThat(bean).isInstanceOf(NoDependencyComponent.class);
      assertThat(compiled.getSourceFile()).contains(
              "getBeanFactory().getBean(InnerComponentConfiguration.class).new NoDependencyComponent()");
    });
    assertThat(getReflectionHints().getTypeHint(NoDependencyComponent.class))
            .satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
  }

  @Test
  void generateWhenHasConstructorWithInnerClassAndParameter() {
    BeanDefinition beanDefinition = new RootBeanDefinition(
            EnvironmentAwareComponent.class);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
    beanFactory.registerSingleton("environment", new StandardEnvironment());
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      EnvironmentAwareComponent bean = getBean(beanFactory, beanDefinition,
              instanceSupplier);
      assertThat(bean).isInstanceOf(EnvironmentAwareComponent.class);
      assertThat(compiled.getSourceFile()).contains(
              "getBeanFactory().getBean(InnerComponentConfiguration.class).new EnvironmentAwareComponent(");
    });
    assertThat(getReflectionHints().getTypeHint(EnvironmentAwareComponent.class))
            .satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
  }

  @Test
  void generateWhenHasConstructorWithGeneric() {
    BeanDefinition beanDefinition = new RootBeanDefinition(
            NumberHolderFactoryBean.class);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerSingleton("number", 123);
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      NumberHolder<?> bean = getBean(beanFactory, beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(NumberHolder.class);
      assertThat(bean).extracting("number").isNull(); // No property actually set
      assertThat(compiled.getSourceFile()).contains("NumberHolderFactoryBean::new");
    });
    assertThat(getReflectionHints().getTypeHint(NumberHolderFactoryBean.class))
            .satisfies(hasConstructorWithMode(ExecutableMode.INTROSPECT));
  }

  @Test
  void generateWhenHasPrivateConstructor() {
    BeanDefinition beanDefinition = new RootBeanDefinition(
            TestBeanWithPrivateConstructor.class);
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      TestBeanWithPrivateConstructor bean = getBean(beanFactory, beanDefinition,
              instanceSupplier);
      assertThat(bean).isInstanceOf(TestBeanWithPrivateConstructor.class);
      assertThat(compiled.getSourceFile())
              .contains("return BeanInstanceSupplier.<TestBeanWithPrivateConstructor>forConstructor();");
    });
    assertThat(getReflectionHints().getTypeHint(TestBeanWithPrivateConstructor.class))
            .satisfies(hasConstructorWithMode(ExecutableMode.INVOKE));
  }

  @Test
  void generateWhenHasFactoryMethodWithNoArg() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(String.class)
            .setFactoryMethodOnBean("stringBean", "config").getBeanDefinition();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      String bean = getBean(beanFactory, beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(String.class);
      assertThat(bean).isEqualTo("Hello");
      assertThat(compiled.getSourceFile()).contains(
              "getBeanFactory().getBean(SimpleConfiguration.class).stringBean()");
    });
    assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
            .satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
  }

  @Test
  void generateWhenHasPrivateStaticFactoryMethodWithNoArg() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(String.class)
            .setFactoryMethodOnBean("privateStaticStringBean", "config")
            .getBeanDefinition();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      String bean = getBean(beanFactory, beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(String.class);
      assertThat(bean).isEqualTo("Hello");
      assertThat(compiled.getSourceFile())
              .contains("forFactoryMethod")
              .doesNotContain("withGenerator");
    });
    assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
            .satisfies(hasMethodWithMode(ExecutableMode.INVOKE));
  }

  @Test
  void generateWhenHasStaticFactoryMethodWithNoArg() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(Integer.class)
            .setFactoryMethodOnBean("integerBean", "config").getBeanDefinition();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      Integer bean = getBean(beanFactory, beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(Integer.class);
      assertThat(bean).isEqualTo(42);
      assertThat(compiled.getSourceFile())
              .contains("SimpleConfiguration::integerBean");
    });
    assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
            .satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
  }

  @Test
  void generateWhenHasStaticFactoryMethodWithArg() {
    RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(String.class)
            .setFactoryMethodOnBean("create", "config").getBeanDefinition();
    beanDefinition.setResolvedFactoryMethod(ReflectionUtils
            .findMethod(SampleFactory.class, "create", Number.class, String.class));
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .genericBeanDefinition(SampleFactory.class).getBeanDefinition());
    beanFactory.registerSingleton("number", 42);
    beanFactory.registerSingleton("string", "test");
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      String bean = getBean(beanFactory, beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(String.class);
      assertThat(bean).isEqualTo("42test");
      assertThat(compiled.getSourceFile()).contains("SampleFactory.create(");
    });
    assertThat(getReflectionHints().getTypeHint(SampleFactory.class))
            .satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
  }

  @Test
  void generateWhenHasStaticFactoryMethodCheckedException() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(Integer.class)
            .setFactoryMethodOnBean("throwingIntegerBean", "config")
            .getBeanDefinition();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
    compile(beanFactory, beanDefinition, (instanceSupplier, compiled) -> {
      Integer bean = getBean(beanFactory, beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(Integer.class);
      assertThat(bean).isEqualTo(42);
      assertThat(compiled.getSourceFile()).doesNotContain(") throws Exception {");
    });
    assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class))
            .satisfies(hasMethodWithMode(ExecutableMode.INTROSPECT));
  }

  private ReflectionHints getReflectionHints() {
    return this.generationContext.getRuntimeHints().reflection();
  }

  private ThrowingConsumer<TypeHint> hasConstructorWithMode(ExecutableMode mode) {
    return hint -> assertThat(hint.constructors()).anySatisfy(hasMode(mode));
  }

  private ThrowingConsumer<TypeHint> hasMethodWithMode(ExecutableMode mode) {
    return hint -> assertThat(hint.methods()).anySatisfy(hasMode(mode));
  }

  private ThrowingConsumer<ExecutableHint> hasMode(ExecutableMode mode) {
    return hint -> assertThat(hint.getMode()).isEqualTo(mode);
  }

  @SuppressWarnings("unchecked")
  private <T> T getBean(StandardBeanFactory beanFactory,
          BeanDefinition beanDefinition, InstanceSupplier<?> instanceSupplier) {
    ((RootBeanDefinition) beanDefinition).setInstanceSupplier(instanceSupplier);
    beanFactory.registerBeanDefinition("testBean", beanDefinition);
    return (T) beanFactory.getBean("testBean");
  }

  private void compile(StandardBeanFactory beanFactory, BeanDefinition beanDefinition,
          BiConsumer<InstanceSupplier<?>, Compiled> result) {

    StandardBeanFactory freshBeanFactory = new StandardBeanFactory(beanFactory);
    freshBeanFactory.registerBeanDefinition("testBean", beanDefinition);
    RegisteredBean registeredBean = RegisteredBean.of(freshBeanFactory, "testBean");
    DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
    GeneratedClass generateClass = this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
    InstanceSupplierCodeGenerator generator = new InstanceSupplierCodeGenerator(
            this.generationContext, generateClass.getName(),
            generateClass.getMethods(), false);
    Executable constructorOrFactoryMethod = registeredBean.resolveConstructorOrFactoryMethod();
    assertThat(constructorOrFactoryMethod).isNotNull();
    CodeBlock generatedCode = generator.generateCode(registeredBean, constructorOrFactoryMethod);
    typeBuilder.set(type -> {
      type.addModifiers(Modifier.PUBLIC);
      type.addSuperinterface(ParameterizedTypeName.get(Supplier.class, InstanceSupplier.class));
      type.addMethod(MethodSpec.methodBuilder("get")
              .addModifiers(Modifier.PUBLIC)
              .returns(InstanceSupplier.class)
              .addStatement("return $L", generatedCode).build());
    });
    this.generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(this.generationContext).compile(compiled ->
            result.accept((InstanceSupplier<?>) compiled.getInstance(Supplier.class).get(), compiled));
  }

}
