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

package cn.taketoday.test.context.util;

import java.lang.reflect.InvocationTargetException;

import cn.taketoday.core.NativeDetector;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Infra factories {@link TodayStrategies.FailureHandler} used
 * within the <em>Infra TestContext Framework</em>.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class TestContextFailureHandler implements TodayStrategies.FailureHandler {

  private final Logger logger = LoggerFactory.getLogger(TestContextFactoriesUtils.class);

  @Override
  public void handleFailure(Class<?> factoryType, String factoryImplementationName, Throwable failure) {
    Throwable ex = (failure instanceof InvocationTargetException ite ? ite.getTargetException() : failure);
    if (ex instanceof ClassNotFoundException || ex instanceof NoClassDefFoundError) {
      if (logger.isDebugEnabled()) {
        logger.debug("""
                Skipping candidate %1$s [%2$s] due to a missing dependency. \
                Specify custom %1$s classes or make the default %1$s classes \
                and their required dependencies available. Offending class: [%3$s]"""
                .formatted(factoryType.getSimpleName(), factoryImplementationName, ex.getMessage()));
      }
    }
    else if (ex instanceof LinkageError) {
      if (logger.isDebugEnabled()) {
        logger.debug("""
                Could not load %1$s [%2$s]. Specify custom %1$s classes or make the default %1$s classes \
                available.""".formatted(factoryType.getSimpleName(), factoryImplementationName), ex);
      }
    }
    // Workaround for https://github.com/oracle/graal/issues/6691
    else if (NativeDetector.inNativeImage() && ex instanceof IllegalStateException) {
      if (logger.isDebugEnabled()) {
        logger.debug("Skipping candidate %1$s [%2$s] due to an error when loading it in a native image."
                .formatted(factoryType.getSimpleName(), factoryImplementationName));
      }
    }
    else {
      if (ex instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (ex instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException(
              "Failed to load %s [%s]".formatted(factoryType.getSimpleName(), factoryImplementationName), ex);
    }
  }

}
