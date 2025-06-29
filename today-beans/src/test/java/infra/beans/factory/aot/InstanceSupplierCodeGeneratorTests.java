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

package infra.beans.factory.aot;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;

import infra.aot.generate.GeneratedClass;
import infra.aot.hint.ExecutableHint;
import infra.aot.hint.ExecutableMode;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.TypeHint;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.InstanceSupplier;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RegisteredBean.InstantiationDescriptor;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.TestBean;
import infra.beans.testfixture.beans.TestBeanWithPrivateConstructor;
import infra.beans.testfixture.beans.factory.aot.DefaultSimpleBeanContract;
import infra.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import infra.beans.testfixture.beans.factory.aot.SimpleBean;
import infra.beans.testfixture.beans.factory.aot.SimpleBeanContract;
import infra.beans.testfixture.beans.factory.generator.InnerComponentConfiguration;
import infra.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponent;
import infra.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.EnvironmentAwareComponentWithoutPublicConstructor;
import infra.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponent;
import infra.beans.testfixture.beans.factory.generator.InnerComponentConfiguration.NoDependencyComponentWithoutPublicConstructor;
import infra.beans.testfixture.beans.factory.generator.SimpleConfiguration;
import infra.beans.testfixture.beans.factory.generator.deprecation.DeprecatedBean;
import infra.beans.testfixture.beans.factory.generator.deprecation.DeprecatedConstructor;
import infra.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalBean;
import infra.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalConstructor;
import infra.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalMemberConfiguration;
import infra.beans.testfixture.beans.factory.generator.deprecation.DeprecatedMemberConfiguration;
import infra.beans.testfixture.beans.factory.generator.factory.NumberHolder;
import infra.beans.testfixture.beans.factory.generator.factory.NumberHolderFactoryBean;
import infra.beans.testfixture.beans.factory.generator.factory.SampleFactory;
import infra.beans.testfixture.beans.factory.generator.injection.InjectionComponent;
import infra.core.env.StandardEnvironment;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.CodeBlock;
import infra.javapoet.MethodSpec;
import infra.javapoet.ParameterizedTypeName;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link InstanceSupplierCodeGenerator}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class InstanceSupplierCodeGeneratorTests {

  private final TestGenerationContext generationContext;

  private final StandardBeanFactory beanFactory;

  InstanceSupplierCodeGeneratorTests() {
    this.generationContext = new TestGenerationContext();
    this.beanFactory = new StandardBeanFactory();
  }

  @Test
  void generateWhenHasDefaultConstructor() {
    BeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      TestBean bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(TestBean.class);
      assertThat(compiled.getSourceFile())
              .contains("InstanceSupplier.using(TestBean::new)");
    });
    assertThat(getReflectionHints().getTypeHint(TestBean.class)).isNotNull();
  }

  @Test
  void generateWhenHasConstructorWithParameter() {
    BeanDefinition beanDefinition = new RootBeanDefinition(InjectionComponent.class);
    this.beanFactory.registerSingleton("injected", "injected");
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      InjectionComponent bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(InjectionComponent.class).extracting("bean").isEqualTo("injected");
    });
    assertThat(getReflectionHints().getTypeHint(InjectionComponent.class)).isNotNull();
  }

  @Test
  void generateWhenHasConstructorWithInnerClassAndDefaultConstructor() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(NoDependencyComponent.class);
    this.beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      Object bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(NoDependencyComponent.class);
      assertThat(compiled.getSourceFile()).contains(
              "getBeanFactory().getBean(InnerComponentConfiguration.class).new NoDependencyComponent()");
    });
    assertThat(getReflectionHints().getTypeHint(NoDependencyComponent.class)).isNotNull();
  }

  @Test
  void generateWhenHasConstructorWithInnerClassAndParameter() {
    BeanDefinition beanDefinition = new RootBeanDefinition(EnvironmentAwareComponent.class);
    StandardEnvironment environment = new StandardEnvironment();
    this.beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
    this.beanFactory.registerSingleton("environment", environment);
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      Object bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(EnvironmentAwareComponent.class);
      assertThat(bean).hasFieldOrPropertyWithValue("environment", environment);
      assertThat(compiled.getSourceFile()).contains(
              "getBeanFactory().getBean(InnerComponentConfiguration.class).new EnvironmentAwareComponent(");
    });
    assertThat(getReflectionHints().getTypeHint(EnvironmentAwareComponent.class)).isNotNull();
  }

  @Test
  void generateWhenHasNonPublicConstructorWithInnerClassAndDefaultConstructor() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(NoDependencyComponentWithoutPublicConstructor.class);
    this.beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      Object bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(NoDependencyComponentWithoutPublicConstructor.class);
      assertThat(compiled.getSourceFile()).doesNotContain(
              "getBeanFactory().getBean(InnerComponentConfiguration.class)");
    });
    assertThat(getReflectionHints().getTypeHint(NoDependencyComponentWithoutPublicConstructor.class))
            .satisfies(hasConstructorWithMode(ExecutableMode.INVOKE));
  }

  @Test
  void generateWhenHasNonPublicConstructorWithInnerClassAndParameter() {
    BeanDefinition beanDefinition = new RootBeanDefinition(EnvironmentAwareComponentWithoutPublicConstructor.class);
    StandardEnvironment environment = new StandardEnvironment();
    this.beanFactory.registerSingleton("configuration", new InnerComponentConfiguration());
    this.beanFactory.registerSingleton("environment", environment);
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      Object bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(EnvironmentAwareComponentWithoutPublicConstructor.class);
      assertThat(bean).hasFieldOrPropertyWithValue("environment", environment);
      assertThat(compiled.getSourceFile()).doesNotContain(
              "getBeanFactory().getBean(InnerComponentConfiguration.class)");
    });
    assertThat(getReflectionHints().getTypeHint(EnvironmentAwareComponentWithoutPublicConstructor.class))
            .satisfies(hasConstructorWithMode(ExecutableMode.INVOKE));
  }

  @Test
  void generateWhenHasConstructorWithGeneric() {
    BeanDefinition beanDefinition = new RootBeanDefinition(NumberHolderFactoryBean.class);
    this.beanFactory.registerSingleton("number", 123);
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      NumberHolder<?> bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(NumberHolder.class);
      assertThat(bean).extracting("number").isNull(); // No property actually set
      assertThat(compiled.getSourceFile()).contains("NumberHolderFactoryBean::new");
    });
    assertThat(getReflectionHints().getTypeHint(NumberHolderFactoryBean.class)).isNotNull();
  }

  @Test
  void generateWhenHasPrivateConstructor() {
    BeanDefinition beanDefinition = new RootBeanDefinition(TestBeanWithPrivateConstructor.class);
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      TestBeanWithPrivateConstructor bean = getBean(beanDefinition, instanceSupplier);
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
    this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      String bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(String.class);
      assertThat(bean).isEqualTo("Hello");
      assertThat(compiled.getSourceFile()).contains(
              "getBeanFactory().getBean(\"config\", SimpleConfiguration.class).stringBean()");
    });
    assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class)).isNotNull();
  }

  @Test
  void generateWhenHasFactoryMethodOnInterface() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(SimpleBean.class)
            .setFactoryMethodOnBean("simpleBean", "config").getBeanDefinition();
    this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .rootBeanDefinition(DefaultSimpleBeanContract.class).getBeanDefinition());
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      Object bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(SimpleBean.class);
      assertThat(compiled.getSourceFile()).contains(
              "getBeanFactory().getBean(\"config\", DefaultSimpleBeanContract.class).simpleBean()");
    });
    assertThat(getReflectionHints().getTypeHint(SimpleBeanContract.class)).isNotNull();
  }

  @Test
  void generateWhenHasPrivateStaticFactoryMethodWithNoArg() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(String.class)
            .setFactoryMethodOnBean("privateStaticStringBean", "config")
            .getBeanDefinition();
    this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      String bean = getBean(beanDefinition, instanceSupplier);
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
            .rootBeanDefinition(SimpleConfiguration.class)
            .setFactoryMethod("integerBean").getBeanDefinition();
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      Integer bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(Integer.class);
      assertThat(bean).isEqualTo(42);
      assertThat(compiled.getSourceFile())
              .contains("(registeredBean) -> SimpleConfiguration.integerBean()");
    });
    assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class)).isNotNull();
  }

  @Test
  void generateWhenHasStaticFactoryMethodWithArg() {
    RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(SimpleConfiguration.class)
            .setFactoryMethod("create").getBeanDefinition();
    beanDefinition.setResolvedFactoryMethod(ReflectionUtils
            .findMethod(SampleFactory.class, "create", Number.class, String.class));
    this.beanFactory.registerSingleton("number", 42);
    this.beanFactory.registerSingleton("string", "test");
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      String bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(String.class);
      assertThat(bean).isEqualTo("42test");
      assertThat(compiled.getSourceFile()).contains("SampleFactory.create(");
    });
    assertThat(getReflectionHints().getTypeHint(SampleFactory.class)).isNotNull();
  }

  @Test
  void generateWhenHasFactoryMethodCheckedException() {
    BeanDefinition beanDefinition = BeanDefinitionBuilder
            .rootBeanDefinition(Integer.class)
            .setFactoryMethodOnBean("throwingIntegerBean", "config")
            .getBeanDefinition();
    this.beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
            .genericBeanDefinition(SimpleConfiguration.class).getBeanDefinition());
    compile(beanDefinition, (instanceSupplier, compiled) -> {
      Integer bean = getBean(beanDefinition, instanceSupplier);
      assertThat(bean).isInstanceOf(Integer.class);
      assertThat(bean).isEqualTo(42);
      assertThat(compiled.getSourceFile()).doesNotContain(") throws Exception {");
    });
    assertThat(getReflectionHints().getTypeHint(SimpleConfiguration.class)).isNotNull();
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
  private <T> T getBean(BeanDefinition beanDefinition, InstanceSupplier<?> instanceSupplier) {
    ((RootBeanDefinition) beanDefinition).setInstanceSupplier(instanceSupplier);
    this.beanFactory.registerBeanDefinition("testBean", beanDefinition);
    return (T) this.beanFactory.getBean("testBean");
  }

  private void compile(BeanDefinition beanDefinition, BiConsumer<InstanceSupplier<?>, Compiled> result) {
    compile(TestCompiler.forSystem(), beanDefinition, result);
  }

  private void compile(TestCompiler testCompiler, BeanDefinition beanDefinition,
          BiConsumer<InstanceSupplier<?>, Compiled> result) {

    StandardBeanFactory freshBeanFactory = new StandardBeanFactory(this.beanFactory);
    freshBeanFactory.registerBeanDefinition("testBean", beanDefinition);
    RegisteredBean registeredBean = RegisteredBean.of(freshBeanFactory, "testBean");
    DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
    GeneratedClass generateClass = this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
    InstanceSupplierCodeGenerator generator = new InstanceSupplierCodeGenerator(
            this.generationContext, generateClass.getName(),
            generateClass.getMethods(), false);
    InstantiationDescriptor instantiationDescriptor = registeredBean.resolveInstantiationDescriptor();
    assertThat(instantiationDescriptor).isNotNull();
    CodeBlock generatedCode = generator.generateCode(registeredBean, instantiationDescriptor);
    typeBuilder.set(type -> {
      type.addModifiers(Modifier.PUBLIC);
      type.addSuperinterface(ParameterizedTypeName.get(Supplier.class, InstanceSupplier.class));
      type.addMethod(MethodSpec.methodBuilder("get")
              .addModifiers(Modifier.PUBLIC)
              .returns(InstanceSupplier.class)
              .addStatement("return $L", generatedCode).build());
    });
    this.generationContext.writeGeneratedContent();
    testCompiler.with(this.generationContext).compile(compiled -> result.accept(
            (InstanceSupplier<?>) compiled.getInstance(Supplier.class).get(), compiled));
  }

  @Nested
  @SuppressWarnings("deprecation")
  class DeprecationTests {

    private static final TestCompiler TEST_COMPILER = TestCompiler.forSystem()
            .withCompilerOptions("-Xlint:all", "-Xlint:-rawtypes", "-Werror");

    @Test
    @Disabled("Need to move to a separate method so that the warning can be suppressed")
    void generateWhenTargetClassIsDeprecated() {
      compileAndCheckWarnings(new RootBeanDefinition(DeprecatedBean.class));
    }

    @Test
    void generateWhenTargetConstructorIsDeprecated() {
      compileAndCheckWarnings(new RootBeanDefinition(DeprecatedConstructor.class));
    }

    @Test
    void generateWhenTargetFactoryMethodIsDeprecated() {
      BeanDefinition beanDefinition = BeanDefinitionBuilder
              .rootBeanDefinition(String.class)
              .setFactoryMethodOnBean("deprecatedString", "config").getBeanDefinition();
      beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
              .genericBeanDefinition(DeprecatedMemberConfiguration.class).getBeanDefinition());
      compileAndCheckWarnings(beanDefinition);
    }

    @Test
    void generateWhenTargetFactoryMethodParameterIsDeprecated() {
      BeanDefinition beanDefinition = BeanDefinitionBuilder
              .rootBeanDefinition(String.class)
              .setFactoryMethodOnBean("deprecatedParameter", "config").getBeanDefinition();
      beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
              .genericBeanDefinition(DeprecatedMemberConfiguration.class).getBeanDefinition());
      beanFactory.registerBeanDefinition("parameter", new RootBeanDefinition(DeprecatedBean.class));
      compileAndCheckWarnings(beanDefinition);
    }

    @Test
    void generateWhenTargetFactoryMethodReturnTypeIsDeprecated() {
      BeanDefinition beanDefinition = BeanDefinitionBuilder
              .rootBeanDefinition(DeprecatedBean.class)
              .setFactoryMethodOnBean("deprecatedReturnType", "config").getBeanDefinition();
      beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
              .genericBeanDefinition(DeprecatedMemberConfiguration.class).getBeanDefinition());
      compileAndCheckWarnings(beanDefinition);
    }

    private void compileAndCheckWarnings(BeanDefinition beanDefinition) {
      assertThatNoException().isThrownBy(() -> compile(TEST_COMPILER, beanDefinition,
              ((instanceSupplier, compiled) -> { })));
    }
  }

  @Nested
  @SuppressWarnings("removal")
  class DeprecationForRemovalTests {

    private static final TestCompiler TEST_COMPILER = TestCompiler.forSystem()
            .withCompilerOptions("-Xlint:all", "-Xlint:-rawtypes", "-Werror");

    @Test
    @Disabled("Need to move to a separate method so that the warning can be suppressed")
    void generateWhenTargetClassIsDeprecatedForRemoval() {
      compileAndCheckWarnings(new RootBeanDefinition(DeprecatedForRemovalBean.class));
    }

    @Test
    void generateWhenTargetConstructorIsDeprecatedForRemoval() {
      compileAndCheckWarnings(new RootBeanDefinition(DeprecatedForRemovalConstructor.class));
    }

    @Test
    void generateWhenTargetFactoryMethodIsDeprecatedForRemoval() {
      BeanDefinition beanDefinition = BeanDefinitionBuilder
              .rootBeanDefinition(String.class)
              .setFactoryMethodOnBean("deprecatedString", "config").getBeanDefinition();
      beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
              .genericBeanDefinition(DeprecatedForRemovalMemberConfiguration.class).getBeanDefinition());
      compileAndCheckWarnings(beanDefinition);
    }

    @Test
    void generateWhenTargetFactoryMethodParameterIsDeprecatedForRemoval() {
      BeanDefinition beanDefinition = BeanDefinitionBuilder
              .rootBeanDefinition(String.class)
              .setFactoryMethodOnBean("deprecatedParameter", "config").getBeanDefinition();
      beanFactory.registerBeanDefinition("config", BeanDefinitionBuilder
              .genericBeanDefinition(DeprecatedForRemovalMemberConfiguration.class).getBeanDefinition());
      beanFactory.registerBeanDefinition("parameter", new RootBeanDefinition(DeprecatedForRemovalBean.class));
      compileAndCheckWarnings(beanDefinition);
    }

    private void compileAndCheckWarnings(BeanDefinition beanDefinition) {
      assertThatNoException().isThrownBy(() -> compile(TEST_COMPILER, beanDefinition,
              ((instanceSupplier, compiled) -> { })));
    }
  }

}
