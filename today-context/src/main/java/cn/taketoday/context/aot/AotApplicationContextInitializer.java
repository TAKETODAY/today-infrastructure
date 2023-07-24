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

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

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
