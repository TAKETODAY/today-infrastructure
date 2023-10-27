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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.aot;

import java.util.function.Supplier;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.bytecode.BytecodeCompiler;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.javapoet.ClassName;

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
      return codeGenerator.generatedClass.getName();
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
