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

package infra.beans.factory.annotation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import javax.lang.model.element.Modifier;

import infra.aot.generate.MethodReference;
import infra.aot.generate.MethodReference.ArgumentCodeGenerator;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.aot.CodeWarnings;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.factory.annotation.DeprecatedInjectionSamples.DeprecatedFieldInjectionPointSample;
import infra.beans.testfixture.beans.factory.annotation.DeprecatedInjectionSamples.DeprecatedFieldInjectionTypeSample;
import infra.beans.testfixture.beans.factory.annotation.DeprecatedInjectionSamples.DeprecatedMethodInjectionPointSample;
import infra.beans.testfixture.beans.factory.annotation.DeprecatedInjectionSamples.DeprecatedMethodInjectionTypeSample;
import infra.beans.testfixture.beans.factory.annotation.DeprecatedInjectionSamples.DeprecatedPrivateFieldInjectionTypeSample;
import infra.beans.testfixture.beans.factory.annotation.DeprecatedInjectionSamples.DeprecatedPrivateMethodInjectionTypeSample;
import infra.beans.testfixture.beans.factory.annotation.DeprecatedInjectionSamples.DeprecatedSample;
import infra.beans.testfixture.beans.factory.annotation.PackagePrivateFieldInjectionSample;
import infra.beans.testfixture.beans.factory.annotation.PackagePrivateMethodInjectionSample;
import infra.beans.testfixture.beans.factory.annotation.PrivateFieldInjectionSample;
import infra.beans.testfixture.beans.factory.annotation.PrivateMethodInjectionSample;
import infra.beans.testfixture.beans.factory.annotation.subpkg.PackagePrivateFieldInjectionFromParentSample;
import infra.beans.testfixture.beans.factory.annotation.subpkg.PackagePrivateMethodInjectionFromParentSample;
import infra.beans.testfixture.beans.factory.aot.MockBeanRegistrationCode;
import infra.core.env.Environment;
import infra.core.env.StandardEnvironment;
import infra.core.test.tools.CompileWithForkedClassLoader;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.SourceFile;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.CodeBlock;
import infra.javapoet.MethodSpec;
import infra.javapoet.ParameterizedTypeName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link AutowiredAnnotationBeanPostProcessor} for AOT contributions.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 5.0
 */
class AutowiredAnnotationBeanRegistrationAotContributionTests {

  private final TestGenerationContext generationContext;

  private final MockBeanRegistrationCode beanRegistrationCode;

  private final StandardBeanFactory beanFactory;

  private final AutowiredAnnotationBeanPostProcessor beanPostProcessor;

  AutowiredAnnotationBeanRegistrationAotContributionTests() {
    this.generationContext = new TestGenerationContext();
    this.beanRegistrationCode = new MockBeanRegistrationCode(this.generationContext);
    this.beanFactory = new StandardBeanFactory();
    this.beanPostProcessor = new AutowiredAnnotationBeanPostProcessor();
    this.beanPostProcessor.setBeanFactory(this.beanFactory);
  }

  @Test
  void contributeWhenPrivateFieldInjectionInjectsUsingReflection() {
    Environment environment = new StandardEnvironment();
    this.beanFactory.registerSingleton("environment", environment);
    RegisteredBean registeredBean = getAndApplyContribution(
            PrivateFieldInjectionSample.class);
    assertThat(RuntimeHintsPredicates.reflection()
            .onField(PrivateFieldInjectionSample.class, "environment"))
            .accepts(this.generationContext.getRuntimeHints());
    compile(registeredBean, (postProcessor, compiled) -> {
      PrivateFieldInjectionSample instance = new PrivateFieldInjectionSample();
      postProcessor.apply(registeredBean, instance);
      assertThat(instance).extracting("environment").isSameAs(environment);
      assertThat(getSourceFile(compiled, PrivateFieldInjectionSample.class))
              .contains("resolveAndSet(");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void contributeWhenPackagePrivateFieldInjectionInjectsUsingConsumer() {
    Environment environment = new StandardEnvironment();
    this.beanFactory.registerSingleton("environment", environment);
    RegisteredBean registeredBean = getAndApplyContribution(
            PackagePrivateFieldInjectionSample.class);
    assertThat(RuntimeHintsPredicates.reflection()
            .onField(PackagePrivateFieldInjectionSample.class, "environment"))
            .accepts(this.generationContext.getRuntimeHints());
    compile(registeredBean, (postProcessor, compiled) -> {
      PackagePrivateFieldInjectionSample instance = new PackagePrivateFieldInjectionSample();
      postProcessor.apply(registeredBean, instance);
      assertThat(instance).extracting("environment").isSameAs(environment);
      assertThat(getSourceFile(compiled, PackagePrivateFieldInjectionSample.class))
              .contains("instance.environment =");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void contributeWhenPackagePrivateFieldInjectionOnParentClassInjectsUsingReflection() {
    Environment environment = new StandardEnvironment();
    this.beanFactory.registerSingleton("environment", environment);
    RegisteredBean registeredBean = getAndApplyContribution(
            PackagePrivateFieldInjectionFromParentSample.class);
    assertThat(RuntimeHintsPredicates.reflection()
            .onField(PackagePrivateFieldInjectionSample.class, "environment"))
            .accepts(this.generationContext.getRuntimeHints());
    compile(registeredBean, (postProcessor, compiled) -> {
      PackagePrivateFieldInjectionFromParentSample instance = new PackagePrivateFieldInjectionFromParentSample();
      postProcessor.apply(registeredBean, instance);
      assertThat(instance).extracting("environment").isSameAs(environment);
      assertThat(getSourceFile(compiled, PackagePrivateFieldInjectionFromParentSample.class))
              .contains("resolveAndSet");
    });
  }

  @Test
  void contributeWhenPrivateMethodInjectionInjectsUsingReflection() {
    Environment environment = new StandardEnvironment();
    this.beanFactory.registerSingleton("environment", environment);
    RegisteredBean registeredBean = getAndApplyContribution(
            PrivateMethodInjectionSample.class);
    assertThat(RuntimeHintsPredicates.reflection()
            .onMethod(PrivateMethodInjectionSample.class, "setTestBean").invoke())
            .accepts(this.generationContext.getRuntimeHints());
    compile(registeredBean, (postProcessor, compiled) -> {
      PrivateMethodInjectionSample instance = new PrivateMethodInjectionSample();
      postProcessor.apply(registeredBean, instance);
      assertThat(instance).extracting("environment").isSameAs(environment);
      assertThat(getSourceFile(compiled, PrivateMethodInjectionSample.class))
              .contains("resolveAndInvoke(");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void contributeWhenPackagePrivateMethodInjectionInjectsUsingConsumer() {
    Environment environment = new StandardEnvironment();
    this.beanFactory.registerSingleton("environment", environment);
    RegisteredBean registeredBean = getAndApplyContribution(
            PackagePrivateMethodInjectionSample.class);
    assertThat(RuntimeHintsPredicates.reflection()
            .onMethod(PackagePrivateMethodInjectionSample.class, "setTestBean").introspect())
            .accepts(this.generationContext.getRuntimeHints());
    compile(registeredBean, (postProcessor, compiled) -> {
      PackagePrivateMethodInjectionSample instance = new PackagePrivateMethodInjectionSample();
      postProcessor.apply(registeredBean, instance);
      assertThat(instance.environment).isSameAs(environment);
      assertThat(getSourceFile(compiled, PackagePrivateMethodInjectionSample.class))
              .contains("args -> instance.setTestBean(");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void contributeWhenPackagePrivateMethodInjectionOnParentClassInjectsUsingReflection() {
    Environment environment = new StandardEnvironment();
    this.beanFactory.registerSingleton("environment", environment);
    RegisteredBean registeredBean = getAndApplyContribution(
            PackagePrivateMethodInjectionFromParentSample.class);
    assertThat(RuntimeHintsPredicates.reflection()
            .onMethod(PackagePrivateMethodInjectionSample.class, "setTestBean"))
            .accepts(this.generationContext.getRuntimeHints());
    compile(registeredBean, (postProcessor, compiled) -> {
      PackagePrivateMethodInjectionFromParentSample instance = new PackagePrivateMethodInjectionFromParentSample();
      postProcessor.apply(registeredBean, instance);
      assertThat(instance.environment).isSameAs(environment);
      assertThat(getSourceFile(compiled, PackagePrivateMethodInjectionFromParentSample.class))
              .contains("resolveAndInvoke(");
    });
  }

  @Test
  void contributeWhenMethodInjectionHasMatchingPropertyValue() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(InjectionBean.class);
    beanDefinition.getPropertyValues().add("counter", 42);
    this.beanFactory.registerBeanDefinition("test", beanDefinition);
    BeanRegistrationAotContribution contribution = this.beanPostProcessor
            .processAheadOfTime(RegisteredBean.of(this.beanFactory, "test"));
    assertThat(contribution).isNull();
  }

  @Nested
  @SuppressWarnings("deprecation")
  class DeprecationTests {

    private static final TestCompiler TEST_COMPILER = TestCompiler.forSystem()
            .withCompilerOptions("-Xlint:all", "-Xlint:-rawtypes", "-Werror");

    @Test
    void contributeWhenTargetClassIsDeprecated() {
      RegisteredBean registeredBean = getAndApplyContribution(DeprecatedSample.class);
      compileAndCheckWarnings(registeredBean);
    }

    @Test
    void contributeWhenFieldInjectionsUsesADeprecatedType() {
      RegisteredBean registeredBean = getAndApplyContribution(
              DeprecatedFieldInjectionTypeSample.class);
      compileAndCheckWarnings(registeredBean);
    }

    @Test
    void contributeWhenFieldInjectionsUsesADeprecatedTypeWithReflection() {
      RegisteredBean registeredBean = getAndApplyContribution(
              DeprecatedPrivateFieldInjectionTypeSample.class);
      compileAndCheckWarnings(registeredBean);
    }

    @Test
    void contributeWhenFieldInjectionsIsDeprecated() {
      RegisteredBean registeredBean = getAndApplyContribution(
              DeprecatedFieldInjectionPointSample.class);
      compileAndCheckWarnings(registeredBean);
    }

    @Test
    void contributeWhenMethodInjectionsUsesADeprecatedType() {
      RegisteredBean registeredBean = getAndApplyContribution(
              DeprecatedMethodInjectionTypeSample.class);
      compileAndCheckWarnings(registeredBean);
    }

    @Test
    void contributeWhenMethodInjectionsUsesADeprecatedTypeWithReflection() {
      RegisteredBean registeredBean = getAndApplyContribution(
              DeprecatedPrivateMethodInjectionTypeSample.class);
      compileAndCheckWarnings(registeredBean);
    }

    @Test
    void contributeWhenMethodInjectionsIsDeprecated() {
      RegisteredBean registeredBean = getAndApplyContribution(
              DeprecatedMethodInjectionPointSample.class);
      compileAndCheckWarnings(registeredBean);
    }

    private void compileAndCheckWarnings(RegisteredBean registeredBean) {
      assertThatNoException().isThrownBy(() -> compile(TEST_COMPILER, registeredBean,
              ((instanceSupplier, compiled) -> { })));
    }

  }

  private RegisteredBean getAndApplyContribution(Class<?> beanClass) {
    RegisteredBean registeredBean = registerBean(beanClass);
    BeanRegistrationAotContribution contribution = this.beanPostProcessor.processAheadOfTime(registeredBean);
    assertThat(contribution).isNotNull();
    contribution.applyTo(this.generationContext, this.beanRegistrationCode);
    return registeredBean;
  }

  private RegisteredBean registerBean(Class<?> beanClass) {
    String beanName = "testBean";
    this.beanFactory.registerBeanDefinition(beanName,
            new RootBeanDefinition(beanClass));
    return RegisteredBean.of(this.beanFactory, beanName);
  }

  private static SourceFile getSourceFile(Compiled compiled, Class<?> sample) {
    return compiled.getSourceFileFromPackage(sample.getPackageName());
  }

  private void compile(RegisteredBean registeredBean,
          BiConsumer<BiFunction<RegisteredBean, Object, Object>, Compiled> result) {
    compile(TestCompiler.forSystem(), registeredBean, result);
  }

  @SuppressWarnings("unchecked")
  private void compile(TestCompiler testCompiler, RegisteredBean registeredBean,
          BiConsumer<BiFunction<RegisteredBean, Object, Object>, Compiled> result) {
    Class<?> target = registeredBean.getBeanClass();
    MethodReference methodReference = this.beanRegistrationCode.getInstancePostProcessors().get(0);
    CodeWarnings codeWarnings = new CodeWarnings();
    this.beanRegistrationCode.getTypeBuilder().set(type -> {
      CodeBlock methodInvocation = methodReference.toInvokeCodeBlock(
              ArgumentCodeGenerator.of(RegisteredBean.class, "registeredBean").and(target, "instance"),
              this.beanRegistrationCode.getClassName());
      type.addModifiers(Modifier.PUBLIC);
      type.addSuperinterface(ParameterizedTypeName.get(BiFunction.class, RegisteredBean.class, target, target));
      type.addMethod(MethodSpec.methodBuilder("apply")
              .addModifiers(Modifier.PUBLIC)
              .addParameter(RegisteredBean.class, "registeredBean")
              .addParameter(target, "instance").returns(target)
              .addStatement("return $L", methodInvocation)
              .build());
      codeWarnings.detectDeprecation(target);
      codeWarnings.suppress(type);
    });
    this.generationContext.writeGeneratedContent();
    testCompiler.with(this.generationContext).printFiles(System.out).compile(compiled ->
            result.accept(compiled.getInstance(BiFunction.class), compiled));
  }

  static class InjectionBean {

    @SuppressWarnings("unused")
    private Integer counter;

    @Autowired
    public void setCounter(Integer counter) {
      this.counter = counter;
    }

  }

}
