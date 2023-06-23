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

import org.junit.jupiter.api.Test;

import cn.taketoday.aot.generate.ClassNameGenerator;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.generate.MethodReference;
import cn.taketoday.aot.generate.MethodReference.ArgumentCodeGenerator;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.BeanRegistrationsAotContribution.Registration;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.GenericBeanWithBounds;
import cn.taketoday.beans.testfixture.beans.Person;
import cn.taketoday.beans.testfixture.beans.RecordBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import cn.taketoday.core.test.io.support.MockTodayStrategies;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.SourceFile;
import cn.taketoday.core.test.tools.TestCompiler;
import cn.taketoday.javapoet.ClassName;
import cn.taketoday.javapoet.CodeBlock;
import cn.taketoday.javapoet.MethodSpec;
import cn.taketoday.javapoet.ParameterizedTypeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.lang.model.element.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates.reflection;

/**
 * Tests for {@link BeanRegistrationsAotContribution}.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @author Brian Clozel
 */
class BeanRegistrationsAotContributionTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final BeanDefinitionMethodGeneratorFactory methodGeneratorFactory = new BeanDefinitionMethodGeneratorFactory(
          AotServices.factoriesAndBeans(new MockTodayStrategies(), this.beanFactory));

  private TestGenerationContext generationContext = new TestGenerationContext();

  private MockBeanFactoryInitializationCode beanFactoryInitializationCode = new MockBeanFactoryInitializationCode(this.generationContext);

  @Test
  void applyToAppliesContribution() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    compile((consumer, compiled) -> {
      StandardBeanFactory freshBeanFactory = new StandardBeanFactory();
      consumer.accept(freshBeanFactory);
      assertThat(freshBeanFactory.getBean(TestBean.class)).isNotNull();
    });
  }

  @Test
  void applyToAppliesContributionWithAliases() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator, "testAlias");
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    compile((consumer, compiled) -> {
      StandardBeanFactory freshBeanFactory = new StandardBeanFactory();
      consumer.accept(freshBeanFactory);
      assertThat(freshBeanFactory.getAliases("testBean")).containsExactly("testAlias");
    });
  }

  @Test
  void applyToWhenHasNameGeneratesPrefixedFeatureName() {
    this.generationContext = new TestGenerationContext(
            new ClassNameGenerator(TestGenerationContext.TEST_TARGET, "Management"));
    this.beanFactoryInitializationCode = new MockBeanFactoryInitializationCode(this.generationContext);
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    compile((consumer, compiled) -> {
      SourceFile sourceFile = compiled.getSourceFile(".*BeanDefinitions");
      assertThat(sourceFile.getClassName()).endsWith("__ManagementBeanDefinitions");
    });
  }

  @Test
  void applyToCallsRegistrationsWithBeanRegistrationsCode() {
    List<BeanRegistrationsCode> beanRegistrationsCodes = new ArrayList<>();
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of()) {

      @Override
      MethodReference generateBeanDefinitionMethod(GenerationContext generationContext,
              BeanRegistrationsCode beanRegistrationsCode) {
        beanRegistrationsCodes.add(beanRegistrationsCode);
        return super.generateBeanDefinitionMethod(generationContext, beanRegistrationsCode);
      }

    };
    BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    assertThat(beanRegistrationsCodes).hasSize(1);
    BeanRegistrationsCode actual = beanRegistrationsCodes.get(0);
    assertThat(actual.getMethods()).isNotNull();
  }

  @Test
  void applyToRegisterReflectionHints() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(TestBean.class, generator);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    assertThat(reflection().onType(TestBean.class)
            .withMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS))
            .accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void applyToRegisterReflectionHintsOnRecordBean() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(RecordBean.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(RecordBean.class, generator);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    assertThat(reflection().onType(RecordBean.class)
            .withMemberCategories(MemberCategory.INTROSPECT_DECLARED_METHODS, MemberCategory.INVOKE_DECLARED_METHODS))
            .accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void applyToRegisterReflectionHintsOnGenericBeanWithBounds() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(GenericBeanWithBounds.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(GenericBeanWithBounds.class, generator);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    assertThat(reflection().onType(Person[].class)).accepts(this.generationContext.getRuntimeHints());
  }

  private RegisteredBean registerBean(RootBeanDefinition rootBeanDefinition) {
    String beanName = "testBean";
    this.beanFactory.registerBeanDefinition(beanName, rootBeanDefinition);
    return RegisteredBean.of(this.beanFactory, beanName);
  }

  @SuppressWarnings({ "unchecked", "cast" })
  private void compile(BiConsumer<Consumer<StandardBeanFactory>, Compiled> result) {
    MethodReference beanRegistrationsMethodReference = this.beanFactoryInitializationCode.getInitializers().get(0);
    MethodReference aliasesMethodReference = this.beanFactoryInitializationCode.getInitializers().get(1);
    this.beanFactoryInitializationCode.getTypeBuilder().set(type -> {
      ArgumentCodeGenerator beanFactory = ArgumentCodeGenerator.of(StandardBeanFactory.class, "beanFactory");
      ClassName className = this.beanFactoryInitializationCode.getClassName();
      CodeBlock beanRegistrationsMethodInvocation = beanRegistrationsMethodReference.toInvokeCodeBlock(beanFactory, className);
      CodeBlock aliasesMethodInvocation = aliasesMethodReference.toInvokeCodeBlock(beanFactory, className);
      type.addModifiers(Modifier.PUBLIC);
      type.addSuperinterface(ParameterizedTypeName.get(Consumer.class, StandardBeanFactory.class));
      type.addMethod(MethodSpec.methodBuilder("accept")
              .addModifiers(Modifier.PUBLIC)
              .addParameter(StandardBeanFactory.class, "beanFactory")
              .addStatement(beanRegistrationsMethodInvocation)
              .addStatement(aliasesMethodInvocation)
              .build());
    });
    this.generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(this.generationContext).compile(compiled ->
            result.accept(compiled.getInstance(Consumer.class), compiled));
  }

  private BeanRegistrationsAotContribution createContribution(Class<?> beanClass,
          BeanDefinitionMethodGenerator methodGenerator, String... aliases) {
    return new BeanRegistrationsAotContribution(
            Map.of(new BeanRegistrationKey("testBean", beanClass), new Registration(methodGenerator, aliases)));
  }

}
