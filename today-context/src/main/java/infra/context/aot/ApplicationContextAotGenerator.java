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
