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

package infra.beans.factory.aot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import infra.aot.generate.ClassNameGenerator;
import infra.aot.generate.GenerationContext;
import infra.aot.generate.MethodReference;
import infra.aot.generate.MethodReference.ArgumentCodeGenerator;
import infra.aot.generate.ValueCodeGenerationException;
import infra.aot.hint.MemberCategory;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanRegistrationsAotContribution.Registration;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.AgeHolder;
import infra.beans.testfixture.beans.Employee;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.NestedTestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.beans.testfixture.beans.factory.aot.MockBeanFactoryInitializationCode;
import infra.core.test.io.support.MockTodayStrategies;
import infra.core.test.tools.Compiled;
import infra.core.test.tools.SourceFile;
import infra.core.test.tools.TestCompiler;
import infra.javapoet.ClassName;
import infra.javapoet.CodeBlock;
import infra.javapoet.MethodSpec;
import infra.javapoet.ParameterizedTypeName;

import static infra.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
    BeanRegistrationsAotContribution contribution = createContribution(registeredBean, generator);
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
    BeanRegistrationsAotContribution contribution = createContribution(registeredBean, generator, "testAlias");
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
    BeanRegistrationsAotContribution contribution = createContribution(registeredBean, generator);
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
    BeanRegistrationsAotContribution contribution = createContribution(registeredBean, generator);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    assertThat(beanRegistrationsCodes).hasSize(1);
    BeanRegistrationsCode actual = beanRegistrationsCodes.get(0);
    assertThat(actual.getMethods()).isNotNull();
  }

  @Test
  void applyToRegisterReflectionHints() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(Employee.class));
    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(registeredBean, generator);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    assertThat(reflection().onType(Employee.class)
            .withMemberCategories(MemberCategory.INTROSPECT_PUBLIC_METHODS, MemberCategory.INTROSPECT_DECLARED_METHODS))
            .accepts(this.generationContext.getRuntimeHints());
    assertThat(reflection().onType(ITestBean.class)
            .withMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS))
            .accepts(this.generationContext.getRuntimeHints());
    assertThat(reflection().onType(AgeHolder.class)
            .withMemberCategory(MemberCategory.INTROSPECT_PUBLIC_METHODS))
            .accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void applyToFailingDoesNotWrapAotException() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    beanDefinition.setInstanceSupplier(TestBean::new);
    RegisteredBean registeredBean = registerBean(beanDefinition);

    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(registeredBean, generator, "testAlias");
    assertThatExceptionOfType(AotProcessingException.class)
            .isThrownBy(() -> contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode))
            .withMessage("Error processing bean with name 'testBean': instance supplier is not supported")
            .withNoCause();
  }

  @Test
  void applyToFailingWrapsValueCodeGeneration() {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
    beanDefinition.getPropertyValues().add("doctor", new NestedTestBean());
    RegisteredBean registeredBean = registerBean(beanDefinition);

    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of());
    BeanRegistrationsAotContribution contribution = createContribution(registeredBean, generator, "testAlias");
    assertThatExceptionOfType(AotProcessingException.class)
            .isThrownBy(() -> contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode))
            .withMessage("Error processing bean with name 'testBean': failed to generate code for bean definition")
            .havingCause().isInstanceOf(ValueCodeGenerationException.class)
            .withMessageContaining("Failed to generate code for")
            .withMessageContaining(NestedTestBean.class.getName());
  }

  @Test
  void applyToFailingProvidesDedicatedException() {
    RegisteredBean registeredBean = registerBean(new RootBeanDefinition(TestBean.class));

    BeanDefinitionMethodGenerator generator = new BeanDefinitionMethodGenerator(this.methodGeneratorFactory,
            registeredBean, null, List.of()) {
      @Override
      MethodReference generateBeanDefinitionMethod(GenerationContext generationContext,
              BeanRegistrationsCode beanRegistrationsCode) {
        throw new IllegalStateException("Test exception");
      }
    };
    BeanRegistrationsAotContribution contribution = createContribution(registeredBean, generator, "testAlias");
    assertThatExceptionOfType(AotProcessingException.class)
            .isThrownBy(() -> contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode))
            .withMessage("Error processing bean with name 'testBean': failed to generate code for bean definition")
            .havingCause().isInstanceOf(IllegalStateException.class).withMessage("Test exception");
  }

  @Test
  void applyToWithLessThanAThousandBeanDefinitionsDoesNotCreateSlices() {
    BeanRegistrationsAotContribution contribution = createContribution(999, i -> "testBean" + i);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    compile((consumer, compiled) -> {
      assertThat(compiled.getSourceFile(".*BeanFactoryRegistrations"))
              .doesNotContain("Register the bean definitions from 0 to 999.",
                      "// Registration is sliced to avoid exceeding size limit");
      StandardBeanFactory freshBeanFactory = new StandardBeanFactory();
      consumer.accept(freshBeanFactory);
      for (int i = 0; i < 999; i++) {
        String beanName = "testBean" + i;
        assertThat(freshBeanFactory.containsBeanDefinition(beanName)).isTrue();
        assertThat(freshBeanFactory.getBean(beanName)).isInstanceOf(TestBean.class);
      }
      assertThat(freshBeanFactory.getBeansOfType(TestBean.class)).hasSize(999);
    });
  }

  @Test
  void applyToWithLargeBeanDefinitionsCreatesSlices() {
    BeanRegistrationsAotContribution contribution = createContribution(1001, i -> "testBean" + i);
    contribution.applyTo(this.generationContext, this.beanFactoryInitializationCode);
    compile((consumer, compiled) -> {
      assertThat(compiled.getSourceFile(".*BeanFactoryRegistrations"))
              .contains("Register the bean definitions from 0 to 999.",
                      "Register the bean definitions from 1000 to 1000.",
                      "// Registration is sliced to avoid exceeding size limit");
      StandardBeanFactory freshBeanFactory = new StandardBeanFactory();
      consumer.accept(freshBeanFactory);
      for (int i = 0; i < 1001; i++) {
        String beanName = "testBean" + i;
        assertThat(freshBeanFactory.containsBeanDefinition(beanName)).isTrue();
        assertThat(freshBeanFactory.getBean(beanName)).isInstanceOf(TestBean.class);
      }
      assertThat(freshBeanFactory.getBeansOfType(TestBean.class)).hasSize(1001);
    });
  }

  private BeanRegistrationsAotContribution createContribution(int size, Function<Integer, String> beanNameFactory) {
    List<Registration> registrations = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      String beanName = beanNameFactory.apply(i);
      RootBeanDefinition beanDefinition = new RootBeanDefinition(TestBean.class);
      this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
      RegisteredBean registeredBean = RegisteredBean.of(this.beanFactory, beanName);
      BeanDefinitionMethodGenerator methodGenerator = new BeanDefinitionMethodGenerator(
              this.methodGeneratorFactory, registeredBean, null, List.of());
      registrations.add(new Registration(registeredBean, methodGenerator, new String[0]));
    }
    return new BeanRegistrationsAotContribution(registrations);
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

  private BeanRegistrationsAotContribution createContribution(RegisteredBean registeredBean,
          BeanDefinitionMethodGenerator methodGenerator, String... aliases) {
    return new BeanRegistrationsAotContribution(
            List.of(new Registration(registeredBean, methodGenerator, aliases)));
  }

}
