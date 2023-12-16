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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory.aot;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.lang.model.element.Modifier;
import javax.xml.parsers.DocumentBuilderFactory;

import cn.taketoday.aot.generate.GeneratedMethod;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.generate.MethodReference;
import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.InstanceSupplier;
import cn.taketoday.beans.factory.support.ManagedList;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.AnnotatedBean;
import cn.taketoday.beans.testfixture.beans.GenericBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.CustomBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.CustomPropertyValue;
import cn.taketoday.beans.testfixture.beans.factory.aot.InnerBeanConfiguration;
import cn.taketoday.beans.testfixture.beans.factory.aot.MockBeanRegistrationsCode;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.SimpleBeanConfiguration;
import cn.taketoday.beans.testfixture.beans.factory.aot.TestHierarchy;
import cn.taketoday.beans.testfixture.beans.factory.aot.TestHierarchy.Implementation;
import cn.taketoday.beans.testfixture.beans.factory.aot.TestHierarchy.One;
import cn.taketoday.beans.testfixture.beans.factory.aot.TestHierarchy.Two;
import cn.taketoday.beans.testfixture.beans.factory.generator.deprecation.DeprecatedBean;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.test.io.support.MockTodayStrategies;
import cn.taketoday.core.test.tools.CompileWithForkedClassLoader;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link BeanDefinitionMethodGenerator} and
 * {@link DefaultBeanRegistrationCodeFragments}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class BeanDefinitionMethodGeneratorTests {

  private final TestGenerationContext generationContext;

  private final StandardBeanFactory beanFactory;

  private final MockBeanRegistrationsCode beanRegistrationsCode;

  private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory;

  BeanDefinitionMethodGeneratorTests() {
    this.generationContext = new TestGenerationContext();
    this.beanFactory = new StandardBeanFactory();
    this.methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
            AotServices.factoriesAndBeans(new MockTodayStrategies(), this.beanFactory));
    this.beanRegistrationsCode = new MockBeanRegistrationsCode(this.generationContext);
  }

  @Test
  void generateWithBeanClassSetsOnlyBeanClass() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("new RootBeanDefinition(TestBean.class)");
      assertThat(sourceFile).doesNotContain("setTargetType(");
      assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateWithTargetTypeWithNoGenericSetsOnlyBeanClass() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setTargetType(TestBean.class);
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("new RootBeanDefinition(TestBean.class)");
      assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateWithTargetTypeUsingGenericsSetsBothBeanClassAndTargetType() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class));
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(actual.getResolvableType().resolve()).isEqualTo(GenericBean.class);
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("new RootBeanDefinition(GenericBean.class)");
      assertThat(sourceFile).contains(
              "setTargetType(ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class))");
      assertThat(sourceFile).contains("setInstanceSupplier(GenericBean::new)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateWithBeanClassAndFactoryMethodNameSetsTargetTypeAndBeanClass() {
    this.beanFactory.registerSingleton("factory", new SimpleBeanConfiguration());
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleBean.class);
    beanDefinition.setFactoryBeanName("factory");
    beanDefinition.setFactoryMethodName("simpleBean");
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("new RootBeanDefinition(SimpleBean.class)");
      assertThat(sourceFile).contains("setTargetType(SimpleBean.class)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateWithTargetTypeAndFactoryMethodNameSetsOnlyBeanClass() {
    this.beanFactory.registerSingleton("factory", new SimpleBeanConfiguration());
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setTargetType(SimpleBean.class);
    beanDefinition.setFactoryBeanName("factory");
    beanDefinition.setFactoryMethodName("simpleBean");
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("new RootBeanDefinition(SimpleBean.class)");
      assertThat(sourceFile).doesNotContain("setTargetType(");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateWithBeanClassAndTargetTypeDifferentSetsBoth() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(One.class);
    beanDefinition.setTargetType(Implementation.class);
    beanDefinition.setResolvedFactoryMethod(ReflectionUtils.findMethod(TestHierarchy.class, "oneBean"));
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("new RootBeanDefinition(TestHierarchy.One.class)");
      assertThat(sourceFile).contains("setTargetType(TestHierarchy.Implementation.class)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateWithBeanClassAndTargetTypWithGenericSetsBoth() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(Integer.class);
    beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class));
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(actual.getResolvableType().resolve()).isEqualTo(GenericBean.class);
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("new RootBeanDefinition(Integer.class)");
      assertThat(sourceFile).contains(
              "setTargetType(ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class))");
      assertThat(sourceFile).contains("setInstanceSupplier(GenericBean::new)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateBeanDefinitionMethodUSeBeanClassNameIfNotReachable() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(PackagePrivateTestBean.class);
    beanDefinition.setTargetType(TestBean.class);
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("new RootBeanDefinition(\"cn.taketoday.beans.factory.aot.PackagePrivateTestBean\"");
      assertThat(sourceFile).contains("setTargetType(TestBean.class)");
      assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenHasInnerClassTargetMethodGeneratesMethod() {
    this.beanFactory.registerBeanDefinition("testBeanConfiguration", new RootBeanDefinition(
            InnerBeanConfiguration.Simple.class));
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleBean.class);
    beanDefinition.setFactoryBeanName("testBeanConfiguration");
    beanDefinition.setFactoryMethodName("simpleBean");
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile.getClassName()).endsWith("InnerBeanConfiguration__BeanDefinitions");
      assertThat(sourceFile).contains("public static class Simple")
              .contains("Bean definitions for {@link InnerBeanConfiguration.Simple}")
              .doesNotContain("Another__BeanDefinitions");

    });
  }

  @Test
  void generateBeanDefinitionMethodWhenHasNestedInnerClassTargetMethodGeneratesMethod() {
    this.beanFactory.registerBeanDefinition("testBeanConfiguration", new RootBeanDefinition(
            InnerBeanConfiguration.Simple.Another.class));
    RootBeanDefinition beanDefinition = new RootBeanDefinition(SimpleBean.class);
    beanDefinition.setFactoryBeanName("testBeanConfiguration");
    beanDefinition.setFactoryMethodName("anotherBean");
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile.getClassName()).endsWith("InnerBeanConfiguration__BeanDefinitions");
      assertThat(sourceFile).contains("public static class Simple")
              .contains("Bean definitions for {@link InnerBeanConfiguration.Simple}")
              .contains("public static class Another")
              .contains("Bean definitions for {@link InnerBeanConfiguration.Simple.Another}");
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenHasGenericsGeneratesMethod() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setTargetType(ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class));
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(actual.getResolvableType().resolve()).isEqualTo(GenericBean.class);
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains(
              "setTargetType(ResolvableType.forClassWithGenerics(GenericBean.class, Integer.class))");
      assertThat(sourceFile).contains("setInstanceSupplier(GenericBean::new)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenHasExplicitResolvableType() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(One.class);
    beanDefinition.setResolvedFactoryMethod(ReflectionUtils.findMethod(TestHierarchy.class, "oneBean"));
    beanDefinition.setTargetType(Two.class);
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> assertThat(actual.getResolvableType().resolve()).isEqualTo(Two.class));
  }

  @Test
  void generateBeanDefinitionMethodWhenHasInstancePostProcessorGeneratesMethod() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
    BeanRegistrationAotContribution aotContribution = (generationContext, beanRegistrationCode) -> {
      GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("postProcess", method ->
              method.addModifiers(Modifier.STATIC)
                      .addParameter(RegisteredBean.class, "registeredBean")
                      .addParameter(TestBean.class, "testBean")
                      .returns(TestBean.class).addCode("return new $T($S);", TestBean.class, "postprocessed"));
      beanRegistrationCode.addInstancePostProcessor(generatedMethod.toMethodReference());
    };
    List<BeanRegistrationAotContribution> aotContributions = Collections.singletonList(aotContribution);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null, aotContributions);
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(actual.getBeanClass()).isEqualTo(TestBean.class);
      InstanceSupplier<?> supplier = (InstanceSupplier<?>) actual.getInstanceSupplier();
      try {
        TestBean instance = (TestBean) supplier.get(registeredBean);
        assertThat(instance.getName()).isEqualTo("postprocessed");
      }
      catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("instanceSupplier.andThen(");
    });
  }

  @Test
    // gh-28748
  void generateBeanDefinitionMethodWhenHasInstancePostProcessorAndFactoryMethodGeneratesMethod() {
    this.beanFactory.registerBeanDefinition("testBeanConfiguration",
            new RootBeanDefinition(TestBeanConfiguration.class));
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    beanDefinition.setFactoryBeanName("testBeanConfiguration");
    beanDefinition.setFactoryMethodName("testBean");
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanRegistrationAotContribution aotContribution = (generationContext,
            beanRegistrationCode) -> {
      GeneratedMethod generatedMethod = beanRegistrationCode.getMethods().add("postProcess", method ->
              method.addModifiers(Modifier.STATIC)
                      .addParameter(RegisteredBean.class, "registeredBean")
                      .addParameter(TestBean.class, "testBean")
                      .returns(TestBean.class).addCode("return new $T($S);", TestBean.class, "postprocessed"));
      beanRegistrationCode.addInstancePostProcessor(generatedMethod.toMethodReference());
    };
    List<BeanRegistrationAotContribution> aotContributions = Collections.singletonList(aotContribution);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null, aotContributions);
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(compiled.getSourceFile(".*BeanDefinitions")).contains("BeanInstanceSupplier");
      assertThat(actual.getBeanClass()).isEqualTo(TestBean.class);
      InstanceSupplier<?> supplier = (InstanceSupplier<?>) actual.getInstanceSupplier();
      try {
        TestBean instance = (TestBean) supplier.get(registeredBean);
        assertThat(instance.getName()).isEqualTo("postprocessed");
      }
      catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("instanceSupplier.andThen(");
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenHasCodeFragmentsCustomizerGeneratesMethod() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
    BeanRegistrationAotContribution aotContribution =
            BeanRegistrationAotContribution.withCustomCodeFragments(this::customizeBeanDefinitionCode);
    List<BeanRegistrationAotContribution> aotContributions = Collections.singletonList(aotContribution);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null, aotContributions);
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(actual.getBeanClass()).isEqualTo(TestBean.class);
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("I am custom");
    });
  }

  private BeanRegistrationCodeFragments customizeBeanDefinitionCode(BeanRegistrationCodeFragments codeFragments) {
    return new BeanRegistrationCodeFragmentsDecorator(codeFragments) {
      @Override
      public CodeBlock generateNewBeanDefinitionCode(GenerationContext generationContext,
              ResolvableType beanType, BeanRegistrationCode beanRegistrationCode) {
        CodeBlock.Builder code = CodeBlock.builder();
        code.addStatement("// I am custom");
        code.add(super.generateNewBeanDefinitionCode(generationContext, beanType, beanRegistrationCode));
        return code.build();
      }
    };
  }

  @Test
  void generateBeanDefinitionMethodDoesNotGenerateAttributesByDefault() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    beanDefinition.setAttribute("a", "A");
    beanDefinition.setAttribute("b", "B");
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(actual.hasAttribute("a")).isFalse();
      assertThat(actual.hasAttribute("b")).isFalse();
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenHasAttributeFilterGeneratesMethod() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    beanDefinition.setAttribute("a", "A");
    beanDefinition.setAttribute("b", "B");
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanRegistrationAotContribution aotContribution =
            BeanRegistrationAotContribution.withCustomCodeFragments(this::customizeAttributeFilter);
    List<BeanRegistrationAotContribution> aotContributions = Collections.singletonList(aotContribution);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            aotContributions);
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(actual.getAttribute("a")).isEqualTo("A");
      assertThat(actual.getAttribute("b")).isNull();
    });
  }

  private BeanRegistrationCodeFragments customizeAttributeFilter(BeanRegistrationCodeFragments codeFragments) {
    return new BeanRegistrationCodeFragmentsDecorator(codeFragments) {
      @Override
      public CodeBlock generateSetBeanDefinitionPropertiesCode(GenerationContext generationContext,
              BeanRegistrationCode beanRegistrationCode, RootBeanDefinition beanDefinition,
              Predicate<String> attributeFilter) {
        return super.generateSetBeanDefinitionPropertiesCode(generationContext,
                beanRegistrationCode, beanDefinition, "a"::equals);
      }
    };
  }

  @Test
  void generateBeanDefinitionMethodWhenInnerBeanGeneratesMethod() {
    RegisteredBean parent = registerBean(new RootBeanDefinition(TestBean.class));
    RegisteredBean innerBean = RegisteredBean.ofInnerBean(parent,
            new RootBeanDefinition(AnnotatedBean.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, innerBean, "testInnerBean",
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      assertThat(compiled.getSourceFile(".*BeanDefinitions"))
              .contains("Get the inner-bean definition for 'testInnerBean'");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenHasInnerBeanPropertyValueGeneratesMethod() {
    RootBeanDefinition innerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(AnnotatedBean.class)
            .setRole(BeanDefinition.ROLE_INFRASTRUCTURE).setPrimary(true)
            .getBeanDefinition();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    beanDefinition.getPropertyValues().add("name", innerBeanDefinition);
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      RootBeanDefinition actualInnerBeanDefinition = (RootBeanDefinition) actual
              .getPropertyValues().getPropertyValue("name");
      assertThat(actualInnerBeanDefinition.isPrimary()).isTrue();
      assertThat(actualInnerBeanDefinition.getRole())
              .isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
      Supplier<?> innerInstanceSupplier = actualInnerBeanDefinition.getInstanceSupplier();
      try {
        assertThat(innerInstanceSupplier.get()).isInstanceOf(AnnotatedBean.class);
      }
      catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Test
  void generateBeanDefinitionMethodWhenHasListOfInnerBeansPropertyValueGeneratesMethod() {
    RootBeanDefinition firstInnerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(TestBean.class).addPropertyValue("name", "one")
            .getBeanDefinition();
    RootBeanDefinition secondInnerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(TestBean.class).addPropertyValue("name", "two")
            .getBeanDefinition();
    ManagedList<RootBeanDefinition> list = new ManagedList<>();
    list.add(firstInnerBeanDefinition);
    list.add(secondInnerBeanDefinition);
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    beanDefinition.getPropertyValues().add("someList", list);
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      ManagedList<RootBeanDefinition> actualPropertyValue = (ManagedList<RootBeanDefinition>) actual
              .getPropertyValues().getPropertyValue("someList");
      assertThat(actualPropertyValue).hasSize(2);
      assertThat(actualPropertyValue.get(0).getPropertyValues().getPropertyValue("name")).isEqualTo("one");
      assertThat(actualPropertyValue.get(1).getPropertyValues().getPropertyValue("name")).isEqualTo("two");
      assertThat(compiled.getSourceFileFromPackage(TestBean.class.getPackageName()))
              .contains("getSomeListBeanDefinition()", "getSomeListBeanDefinition1()");
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenHasInnerBeanConstructorValueGeneratesMethod() {
    RootBeanDefinition innerBeanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(String.class)
            .setRole(BeanDefinition.ROLE_INFRASTRUCTURE).setPrimary(true)
            .getBeanDefinition();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    ValueHolder valueHolder = new ValueHolder(innerBeanDefinition);
    valueHolder.setName("second");
    beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
            valueHolder);
    RegisteredBean registeredBean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      RootBeanDefinition actualInnerBeanDefinition = (RootBeanDefinition) actual
              .getConstructorArgumentValues()
              .getIndexedArgumentValue(0, RootBeanDefinition.class).getValue();
      assertThat(actualInnerBeanDefinition.isPrimary()).isTrue();
      assertThat(actualInnerBeanDefinition.getRole())
              .isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);
      Supplier<?> innerInstanceSupplier = actualInnerBeanDefinition.getInstanceSupplier();
      try {
        assertThat(innerInstanceSupplier.get()).isInstanceOf(String.class);
      }
      catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
      assertThat(compiled.getSourceFile(".*BeanDefinitions"))
              .contains("getSecondBeanDefinition()");
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenCustomPropertyValueUsesCustomDelegate() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(CustomBean.class);
    beanDefinition.getPropertyValues().add(
            "customPropertyValue", new CustomPropertyValue("test"));
    RegisteredBean bean = registerBean(beanDefinition);
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, bean, "test",
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) ->
            assertThat(actual.getPropertyValues().getPropertyValue("customPropertyValue"))
                    .isInstanceOfSatisfying(CustomPropertyValue.class, customPropertyValue
                            -> assertThat(customPropertyValue.value()).isEqualTo("test")));
  }

  @Test
  void generateBeanDefinitionMethodWhenHasAotContributionsAppliesContributions() {
    RegisteredBean registeredBean = registerBean(
            new RootBeanDefinition(TestBean.class));
    List<BeanRegistrationAotContribution> aotContributions = new ArrayList<>();
    aotContributions.add((generationContext, beanRegistrationCode) ->
            beanRegistrationCode.getMethods().add("aotContributedMethod", method ->
                    method.addComment("Example Contribution")));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null, aotContributions);
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("AotContributedMethod()");
      assertThat(sourceFile).contains("Example Contribution");
    });
  }

  @Test
  @CompileWithForkedClassLoader
  void generateBeanDefinitionMethodWhenPackagePrivateBean() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(PackagePrivateTestBean.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      StandardBeanFactory freshBeanFactory = new StandardBeanFactory();
      freshBeanFactory.registerBeanDefinition("test", actual);
      Object bean = freshBeanFactory.getBean("test");
      assertThat(bean).isInstanceOf(PackagePrivateTestBean.class);
      assertThat(compiled.getSourceFileFromPackage(
              PackagePrivateTestBean.class.getPackageName())).isNotNull();
    });
  }

  @Test
  void generateBeanDefinitionMethodWhenBeanIsInJavaPackage() {
    RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(String.class).addConstructorArgValue("test").getBeanDefinition();
    testBeanDefinitionMethodInCurrentFile(String.class, beanDefinition);
  }

  @Test
  void generateBeanDefinitionMethodWhenBeanIsInJavaxPackage() {
    RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(DocumentBuilderFactory.class).setFactoryMethod("newDefaultInstance").getBeanDefinition();
    testBeanDefinitionMethodInCurrentFile(DocumentBuilderFactory.class, beanDefinition);
  }

  @Test
  void generateBeanDefinitionMethodWhenBeanIsOfPrimitiveType() {
    RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
            .rootBeanDefinition(Boolean.class).setFactoryMethod("parseBoolean").addConstructorArgValue("true").getBeanDefinition();
    testBeanDefinitionMethodInCurrentFile(Boolean.class, beanDefinition);
  }

  @Test
  void generateBeanDefinitionMethodWhenInstanceSupplierWithNoCustomization() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class, TestBean::new));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null, List.of());
    assertThatIllegalStateException().isThrownBy(() -> generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode)).withMessageStartingWith(
            "Default code generation is not supported for bean definitions declaring an instance supplier callback");
  }

  @Test
  void generateBeanDefinitionMethodWhenInstanceSupplierWithOnlyCustomTarget() {
    BeanRegistrationAotContribution aotContribution = BeanRegistrationAotContribution.withCustomCodeFragments(
            defaultCodeFragments -> new BeanRegistrationCodeFragmentsDecorator(defaultCodeFragments) {
              @Override
              public ClassName getTarget(RegisteredBean registeredBean) {
                return ClassName.get(TestBean.class);
              }
            });
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class, TestBean::new));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            List.of(aotContribution));
    assertThatIllegalStateException().isThrownBy(() -> generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode)).withMessageStartingWith(
            "Default code generation is not supported for bean definitions declaring an instance supplier callback");
  }

  @Test
  void generateBeanDefinitionMethodWhenInstanceSupplierWithOnlyCustomInstanceSupplier() {
    BeanRegistrationAotContribution aotContribution = BeanRegistrationAotContribution.withCustomCodeFragments(
            defaultCodeFragments -> new BeanRegistrationCodeFragmentsDecorator(defaultCodeFragments) {
              @Override
              public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
                      BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
                return CodeBlock.of("// custom");
              }
            });
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class, TestBean::new));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            List.of(aotContribution));
    assertThatIllegalStateException().isThrownBy(() -> generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode)).withMessageStartingWith(
            "Default code generation is not supported for bean definitions declaring an instance supplier callback");
  }

  @Test
  void generateBeanDefinitionMethodWhenInstanceSupplierWithCustomInstanceSupplierAndCustomTarget() {
    BeanRegistrationAotContribution aotContribution = BeanRegistrationAotContribution.withCustomCodeFragments(
            defaultCodeFragments -> new BeanRegistrationCodeFragmentsDecorator(defaultCodeFragments) {

              @Override
              public ClassName getTarget(RegisteredBean registeredBean) {
                return ClassName.get(TestBean.class);
              }

              @Override
              public CodeBlock generateInstanceSupplierCode(GenerationContext generationContext,
                      BeanRegistrationCode beanRegistrationCode, boolean allowDirectSupplierShortcut) {
                return CodeBlock.of("$T::new", TestBean.class);
              }
            });
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class, TestBean::new));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            List.of(aotContribution));
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile).contains("Get the bean definition for 'testBean'");
      assertThat(sourceFile).contains("setInstanceSupplier(TestBean::new)");
      assertThat(actual).isInstanceOf(RootBeanDefinition.class);
    });
  }

  @Nested
  @SuppressWarnings("deprecation")
  class DeprecationTests {

    private static final TestCompiler TEST_COMPILER = TestCompiler.forSystem()
            .withCompilerOptions("-Xlint:all", "-Xlint:-rawtypes", "-Werror");

    @Test
    void generateBeanDefinitionMethodWithDeprecatedTargetClass() {
      RootBeanDefinition beanDefinition = new RootBeanDefinition(DeprecatedBean.class);
      RegisteredBean registeredBean = registerBean(beanDefinition);
      BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
              methodGeneratorFactory, registeredBean, null,
              Collections.emptyList());
      MethodReference method = generator.generateBeanDefinitionMethod(
              generationContext, beanRegistrationsCode);
      compileAndCheckWarnings(method);
    }

    private void compileAndCheckWarnings(MethodReference methodReference) {
      assertThatNoException().isThrownBy(() -> compile(TEST_COMPILER, methodReference,
              ((instanceSupplier, compiled) -> { })));
    }

  }

  private void testBeanDefinitionMethodInCurrentFile(Class<?> targetType, RootBeanDefinition beanDefinition) {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(beanDefinition));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(
            this.methodGeneratorFactory, registeredBean, null,
            Collections.emptyList());
    MethodReference method = generator.generateBeanDefinitionMethod(
            this.generationContext, this.beanRegistrationsCode);
    compile(method, (actual, compiled) -> {
      StandardBeanFactory freshBeanFactory = new StandardBeanFactory();
      freshBeanFactory.registerBeanDefinition("test", actual);
      Object bean = freshBeanFactory.getBean("test");
      assertThat(bean).isInstanceOf(targetType);
      assertThat(compiled.getSourceFiles().stream().filter(sourceFile ->
              sourceFile.getClassName().startsWith(targetType.getPackageName()))).isEmpty();
    });
  }

  private RegisteredBean registerBean(RootBeanDefinition beanDefinition) {
    String beanName = "testBean";
    this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
    return RegisteredBean.of(this.beanFactory, beanName);
  }

  private void compile(MethodReference method, BiConsumer<RootBeanDefinition, Compiled> result) {
    compile(TestCompiler.forSystem(), method, result);
  }

  private void compile(TestCompiler testCompiler, MethodReference method, BiConsumer<RootBeanDefinition, Compiled> result) {
    this.beanRegistrationsCode.getTypeBuilder().set(type -> {
      CodeBlock methodInvocation = method.toInvokeCodeBlock(ArgumentCodeGenerator.none(),
              this.beanRegistrationsCode.getClassName());
      type.addModifiers(Modifier.PUBLIC);
      type.addSuperinterface(ParameterizedTypeName.get(Supplier.class, BeanDefinition.class));
      type.addMethod(MethodSpec.methodBuilder("get")
              .addModifiers(Modifier.PUBLIC)
              .returns(BeanDefinition.class)
              .addCode("return $L;", methodInvocation).build());
    });
    this.generationContext.writeGeneratedContent();
    testCompiler.with(this.generationContext).compile(compiled ->
            result.accept((RootBeanDefinition) compiled.getInstance(Supplier.class).get(), compiled));
  }

}
