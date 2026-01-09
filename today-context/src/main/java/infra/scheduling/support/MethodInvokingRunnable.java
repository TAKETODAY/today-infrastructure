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

package infra.scheduling.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.InitializingBean;
import infra.beans.support.ArgumentConvertingMethodInvoker;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.ReflectiveMethodInvoker;

/**
 * Adapter that implements the {@link Runnable} interface as a configurable
 * method invocation based on Framework's MethodInvoker.
 *
 * <p>Inherits common configuration properties from
 * {@link ReflectiveMethodInvoker}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.util.concurrent.Executor#execute(Runnable)
 * @since 4.0 2022/2/18 11:18
 */
public class MethodInvokingRunnable extends ArgumentConvertingMethodInvoker
        implements Runnable, BeanClassLoaderAware, InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(MethodInvokingRunnable.class);

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
    return ClassUtils.forName(className, this.beanClassLoader);
  }

  @Override
  public void afterPropertiesSet() throws ClassNotFoundException, NoSuchMethodException {
    prepare();
  }

  @Override
  public void run() {
    try {
      invoke();
    }
    catch (InvocationTargetException ex) {
      log.error(getInvocationFailureMessage(), ex.getTargetException());
      // Do not throw exception, else the main loop of the scheduler might stop!
    }
    catch (Throwable ex) {
      log.error(getInvocationFailureMessage(), ex);
      // Do not throw exception, else the main loop of the scheduler might stop!
    }
  }

  /**
   * Build a message for an invocation failure exception.
   *
   * @return the error message, including the target method name etc
   */
  protected String getInvocationFailureMessage() {
    return "Invocation of method '" + getTargetMethod() +
            "' on target class [" + getTargetClass() + "] failed";
  }

}
