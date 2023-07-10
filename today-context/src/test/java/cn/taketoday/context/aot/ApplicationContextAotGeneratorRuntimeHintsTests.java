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

package cn.taketoday.context.aot;

import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.test.agent.EnabledIfRuntimeHintsAgent;
import cn.taketoday.aot.test.agent.RuntimeHintsInvocations;
import cn.taketoday.aot.test.agent.RuntimeHintsRecorder;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.testfixture.context.annotation.AutowiredComponent;
import cn.taketoday.context.testfixture.context.annotation.InitDestroyComponent;
import cn.taketoday.context.testfixture.context.generator.SimpleComponent;
import cn.taketoday.core.test.tools.TestCompiler;
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
  void generateApplicationContextWithAutowiring() {
    GenericApplicationContext context = new AnnotationConfigApplicationContext();
    context.registerBeanDefinition("autowiredComponent", new RootBeanDefinition(AutowiredComponent.class));
    context.registerBeanDefinition("number", BeanDefinitionBuilder.rootBeanDefinition(Integer.class, "valueOf")
            .addConstructorArgValue("42").getBeanDefinition());
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

  private void compile(GenericApplicationContext applicationContext,
          BiConsumer<RuntimeHints, RuntimeHintsInvocations> initializationResult) {
    ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
    TestGenerationContext generationContext = new TestGenerationContext();
    generator.processAheadOfTime(applicationContext, generationContext);
    generationContext.writeGeneratedContent();
    TestCompiler.forSystem().with(generationContext).compile(compiled -> {
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