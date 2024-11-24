/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.scheduling.support;

import java.lang.reflect.InvocationTargetException;

import infra.beans.factory.BeanClassLoaderAware;
import infra.beans.factory.InitializingBean;
import infra.beans.support.ArgumentConvertingMethodInvoker;
import infra.lang.Nullable;
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
