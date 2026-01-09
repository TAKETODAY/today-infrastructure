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

import org.jspecify.annotations.Nullable;

import infra.beans.BeanInstantiationException;
import infra.beans.BeanUtils;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;

/**
 * Specialized {@link ApplicationContextInitializer} used to initialize a
 * {@link ConfigurableApplicationContext} using artifacts that were generated
 * ahead-of-time.
 * <p>
 * Instances of this initializer are usually created using
 * {@link #forInitializerClasses(String...)}, passing in the names of code
 * generated initializer classes.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface AotApplicationContextInitializer extends ApplicationContextInitializer {

  /**
   * Factory method to create a new {@link AotApplicationContextInitializer}
   * instance that delegates to other initializers loaded from the given set
   * of class names.
   *
   * @param initializerClassNames the class names of the initializers to load
   * @return a new {@link AotApplicationContextInitializer} instance
   */
  static AotApplicationContextInitializer forInitializerClasses(String... initializerClassNames) {
    Assert.noNullElements(initializerClassNames, "'initializerClassNames' must not contain null elements");
    return applicationContext -> initialize(applicationContext, initializerClassNames);
  }

  static <C extends ConfigurableApplicationContext> void initialize(
          C applicationContext, String... initializerClassNames) {
    Logger logger = LoggerFactory.getLogger(AotApplicationContextInitializer.class);
    ClassLoader classLoader = applicationContext.getClassLoader();
    logger.debug("Initializing ApplicationContext with AOT");
    for (String initializerClassName : initializerClassNames) {
      logger.trace("Applying {}", initializerClassName);
      instantiateInitializer(initializerClassName, classLoader)
              .initialize(applicationContext);
    }
  }

  static ApplicationContextInitializer instantiateInitializer(
          String initializerClassName, @Nullable ClassLoader classLoader) {
    try {
      var initializerClass = ClassUtils.<ApplicationContextInitializer>resolveClassName(initializerClassName, classLoader);
      Assert.isAssignable(ApplicationContextInitializer.class, initializerClass);
      return BeanUtils.newInstance(initializerClass);
    }
    catch (BeanInstantiationException ex) {
      throw new IllegalArgumentException(
              "Failed to instantiate ApplicationContextInitializer: " + initializerClassName, ex);
    }
  }

}
