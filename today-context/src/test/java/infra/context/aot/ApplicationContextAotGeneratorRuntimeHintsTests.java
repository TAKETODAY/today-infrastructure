/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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