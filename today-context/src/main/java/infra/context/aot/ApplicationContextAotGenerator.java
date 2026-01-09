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

import java.util.function.Supplier;

import infra.aot.generate.GenerationContext;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.bytecode.BytecodeCompiler;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.support.GenericApplicationContext;
import infra.javapoet.ClassName;

/**
 * Process an {@link ApplicationContext} and its {@link BeanFactory} to generate
 * code that represents the state of the bean factory, as well as the necessary
 * hints that can be used at runtime in a constrained environment.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ApplicationContextAotGenerator {

  /**
   * Process the specified {@link GenericApplicationContext} ahead-of-time using
   * the specified {@link GenerationContext}.
   * <p>Return the {@link ClassName} of the {@link ApplicationContextInitializer}
   * to use to restore an optimized state of the application context.
   *
   * @param applicationContext the non-refreshed application context to process
   * @param generationContext the generation context to use
   * @return the {@code ClassName} of the {@code ApplicationContextInitializer}
   * entry point
   */
  public ClassName processAheadOfTime(GenericApplicationContext applicationContext,
          GenerationContext generationContext) {
    return withCglibClassHandler(new CglibClassHandler(generationContext), () -> {
      applicationContext.refreshForAotProcessing(generationContext.getRuntimeHints());
      var codeGenerator = new ApplicationContextInitializationCodeGenerator(applicationContext, generationContext);
      StandardBeanFactory beanFactory = applicationContext.getBeanFactory();
      new BeanFactoryInitializationAotContributions(beanFactory).applyTo(generationContext, codeGenerator);
      return codeGenerator.getClassName();
    });
  }

  private <T> T withCglibClassHandler(CglibClassHandler cglibClassHandler, Supplier<T> task) {
    try {
      BytecodeCompiler.setLoadedClassHandler(cglibClassHandler::handleLoadedClass);
      BytecodeCompiler.setGeneratedClassHandler(cglibClassHandler::handleGeneratedClass);
      return task.get();
    }
    finally {
      BytecodeCompiler.setLoadedClassHandler(null);
      BytecodeCompiler.setGeneratedClassHandler(null);
    }
  }

}
