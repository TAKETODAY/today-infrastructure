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

package infra.test.context;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ReflectionUtils;

/**
 * Default implementation of the {@link MethodInvoker} API.
 *
 * <p>This implementation never provides arguments to a {@link Method}.
 *
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class DefaultMethodInvoker implements MethodInvoker {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMethodInvoker.class);

  @Override
  public @Nullable Object invoke(Method method, @Nullable Object target) throws Exception {
    Assert.notNull(method, "Method is required");

    try {
      ReflectionUtils.makeAccessible(method);
      return method.invoke(target);
    }
    catch (InvocationTargetException ex) {
      if (logger.isErrorEnabled()) {
        logger.error("Exception encountered while invoking method [%s] on target [%s]"
                .formatted(method, target), ex.getTargetException());
      }
      ReflectionUtils.rethrowException(ex.getTargetException());
      // appease the compiler
      return null;
    }
  }

}
