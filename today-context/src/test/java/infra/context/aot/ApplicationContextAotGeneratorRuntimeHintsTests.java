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

package infra.context.aot;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;

import infra.aot.hint.RuntimeHints;
import infra.aot.test.agent.EnabledIfRuntimeHintsAgent;
import infra.aot.test.agent.RuntimeHintsInvocations;
import infra.aot.test.agent.RuntimeHintsRecorder;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.ApplicationContextInitializer;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.context.testfixture.context.annotation.AutowiredComponent;
import infra.context.testfixture.context.annotation.InitDestroyComponent;
import infra.context.testfixture.context.generator.SimpleComponent;
import infra.core.test.tools.TestCompiler;
import jakarta.annotation.PreDestroy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link RuntimeHints} generation in {@link ApplicationContextAotGenerator}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/6 11:30
 */
@EnabledIfRuntimeHintsAgent
class ApplicationContextAotGeneratorRuntimeHintsTests {

  @Test
  void generateApplicationContextWithSimpleBean() {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBeanDefinition("test", new RootBeanDefinition(SimpleComponent.class));
    compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
  }

  @Test
  @Disabled("-parameters apply failed")
  void generateApplicationContextWithAutowiring() {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBeanDefinition("autowiredComponent", new RootBeanDefinition(AutowiredComponent.class));
    context.registerBeanDefinition("number", BeanDefinitionBuilder.rootBeanDefinition(
            Integer.class, "valueOf").addConstructorArgValue("42").getBeanDefinition());
    compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
  }

  @Test
  void generateApplicationContextWithInitDestroyMethods() {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBeanDefinition("initDestroyComponent", new RootBeanDefinition(InitDestroyComponent.class));
    compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
  }

  @Test
  void generateApplicationContextWithMultipleInitDestroyMethods() {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(InitDestroyComponent.class);
    beanDefinition.setInitMethodName("customInit");
    beanDefinition.setDestroyMethodName("customDestroy");
    context.registerBeanDefinition("initDestroyComponent", beanDefinition);
    compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
  }

  @Test
  void generateApplicationContextWithInheritedDestroyMethods() {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(InheritedDestroy.class);
    context.registerBeanDefinition("initDestroyComponent", beanDefinition);
    compile(context, (hints, invocations) -> assertThat(invocations).match(hints));
  }

  @SuppressWarnings("deprecation")
  private void compile(GenericApplicationContext applicationContext,
          BiConsumer<RuntimeHints, RuntimeHintsInvocations> initializationResult) {
    ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
    TestGenerationContext generationContext = new TestGenerationContext();
    generator.processAheadOfTime(applicationContext, generationContext);
    generationContext.writeGeneratedContent();
    TestCompiler.forSystem().withCompilerOptions("-parameters").with(generationContext).compile(compiled -> {
      ApplicationContextInitializer instance = compiled.getInstance(ApplicationContextInitializer.class);
      GenericApplicationContext freshContext = new GenericApplicationContext();
      RuntimeHintsInvocations recordedInvocations = RuntimeHintsRecorder.record(() -> {
        instance.initialize(freshContext);
        freshContext.refresh();
        freshContext.close();
      });
      initializationResult.accept(generationContext.getRuntimeHints(), recordedInvocations);
    });
  }

  public interface Destroyable {

    @PreDestroy
    default void destroy() {
    }
  }

  public static class InheritedDestroy implements Destroyable {
  }

}