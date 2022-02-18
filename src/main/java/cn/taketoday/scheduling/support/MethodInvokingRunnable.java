/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.scheduling.support;

import java.lang.reflect.InvocationTargetException;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.support.ArgumentConvertingMethodInvoker;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;

/**
 * Adapter that implements the {@link Runnable} interface as a configurable
 * method invocation based on Spring's MethodInvoker.
 *
 * <p>Inherits common configuration properties from
 * {@link cn.taketoday.util.ReflectiveMethodInvoker}.
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
