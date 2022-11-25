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

package cn.taketoday.framework.test.context.assertj;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.Supplier;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link InvocationHandler} used by {@link ApplicationContextAssertProvider} generated
 * proxies.
 *
 * @author Phillip Webb
 */
class AssertProviderApplicationContextInvocationHandler implements InvocationHandler {

  private final Class<?> applicationContextType;

  private final ApplicationContext applicationContext;

  private final RuntimeException startupFailure;

  AssertProviderApplicationContextInvocationHandler(Class<?> applicationContextType, Supplier<?> contextSupplier) {
    this.applicationContextType = applicationContextType;
    Object contextOrStartupFailure = getContextOrStartupFailure(contextSupplier);
    if (contextOrStartupFailure instanceof RuntimeException) {
      this.applicationContext = null;
      this.startupFailure = (RuntimeException) contextOrStartupFailure;
    }
    else {
      this.applicationContext = (ApplicationContext) contextOrStartupFailure;
      this.startupFailure = null;
    }
  }

  private Object getContextOrStartupFailure(Supplier<?> contextSupplier) {
    try {
      return contextSupplier.get();
    }
    catch (RuntimeException ex) {
      return ex;
    }
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (isToString(method)) {
      return toString();
    }
    if (isGetSourceContext(method)) {
      return getSourceContext(args);
    }
    if (isGetStartupFailure(method)) {
      return getStartupFailure();
    }
    if (isAssertThat(method)) {
      return getAssertThat(proxy);
    }
    if (isCloseMethod(method)) {
      return invokeClose();
    }
    return invokeApplicationContextMethod(method, args);
  }

  private boolean isToString(Method method) {
    return ("toString".equals(method.getName()) && method.getParameterCount() == 0);
  }

  @Override
  public String toString() {
    if (this.startupFailure != null) {
      return "Unstarted application context " + this.applicationContextType.getName() + "[startupFailure="
              + this.startupFailure.getClass().getName() + "]";
    }
    ToStringBuilder builder = new ToStringBuilder(this.applicationContext)
            .append("id", this.applicationContext.getId())
            .append("applicationName", this.applicationContext.getApplicationName())
            .append("beanDefinitionCount", this.applicationContext.getBeanDefinitionCount());
    return "Started application " + builder;
  }

  private boolean isGetSourceContext(Method method) {
    return "getSourceApplicationContext".equals(method.getName()) && ((method.getParameterCount() == 0)
            || Arrays.equals(new Class<?>[] { Class.class }, method.getParameterTypes()));
  }

  private Object getSourceContext(Object[] args) {
    ApplicationContext context = getStartedApplicationContext();
    if (ObjectUtils.isNotEmpty(args)) {
      Assert.isInstanceOf((Class<?>) args[0], context);
    }
    return context;
  }

  private boolean isGetStartupFailure(Method method) {
    return ("getStartupFailure".equals(method.getName()) && method.getParameterCount() == 0);
  }

  private Object getStartupFailure() {
    return this.startupFailure;
  }

  private boolean isAssertThat(Method method) {
    return ("assertThat".equals(method.getName()) && method.getParameterCount() == 0);
  }

  private Object getAssertThat(Object proxy) {
    return new ApplicationContextAssert<>((ApplicationContext) proxy, this.startupFailure);
  }

  private boolean isCloseMethod(Method method) {
    return ("close".equals(method.getName()) && method.getParameterCount() == 0);
  }

  private Object invokeClose() {
    applicationContext.close();
    return null;
  }

  private Object invokeApplicationContextMethod(Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(getStartedApplicationContext(), args);
    }
    catch (InvocationTargetException ex) {
      throw ex.getTargetException();
    }
  }

  private ApplicationContext getStartedApplicationContext() {
    if (this.startupFailure != null) {
      throw new IllegalStateException(this + " failed to start", this.startupFailure);
    }
    return this.applicationContext;
  }

}
