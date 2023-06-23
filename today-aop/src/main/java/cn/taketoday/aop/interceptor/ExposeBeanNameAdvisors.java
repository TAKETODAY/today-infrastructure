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

package cn.taketoday.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serial;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.aop.support.DefaultIntroductionAdvisor;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.DelegatingIntroductionInterceptor;
import cn.taketoday.beans.factory.NamedBean;
import cn.taketoday.lang.Nullable;

/**
 * Convenient methods for creating advisors that may be used when autoproxying beans
 * created with the IoC container, binding the bean name to the current
 * invocation. May support a {@code bean()} pointcut designator with AspectJ.
 *
 * <p>Typically used in auto-proxying, where the bean name is known
 * at proxy creation time.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NamedBean
 * @since 4.0 2022/3/9 22:22
 */
public abstract class ExposeBeanNameAdvisors {

  /**
   * Binding for the bean name of the bean which is currently being invoked
   * in the ReflectiveMethodInvocation userAttributes Map.
   */
  private static final String BEAN_NAME_ATTRIBUTE = ExposeBeanNameAdvisors.class.getName() + ".BEAN_NAME";

  /**
   * Find the bean name for the current invocation. Assumes that an ExposeBeanNameAdvisor
   * has been included in the interceptor chain, and that the invocation is exposed
   * with ExposeInvocationInterceptor.
   *
   * @return the bean name (never {@code null})
   * @throws IllegalStateException if the bean name has not been exposed
   */
  public static String getBeanName() throws IllegalStateException {
    return getBeanName(ExposeInvocationInterceptor.currentInvocation());
  }

  /**
   * Find the bean name for the given invocation. Assumes that an ExposeBeanNameAdvisor
   * has been included in the interceptor chain.
   *
   * @param mi the MethodInvocation that should contain the bean name as an attribute
   * @return the bean name (never {@code null})
   * @throws IllegalStateException if the bean name has not been exposed
   */
  public static String getBeanName(MethodInvocation mi) throws IllegalStateException {
    if (!(mi instanceof ProxyMethodInvocation pmi)) {
      throw new IllegalArgumentException("MethodInvocation is not a ProxyMethodInvocation: " + mi);
    }
    String beanName = (String) pmi.getAttribute(BEAN_NAME_ATTRIBUTE);
    if (beanName == null) {
      throw new IllegalStateException("Cannot get bean name; not set on MethodInvocation: " + mi);
    }
    return beanName;
  }

  /**
   * Create a new advisor that will expose the given bean name,
   * with no introduction.
   *
   * @param beanName bean name to expose
   */
  public static Advisor createAdvisorWithoutIntroduction(String beanName) {
    return new DefaultPointcutAdvisor(new ExposeBeanNameInterceptor(beanName));
  }

  /**
   * Create a new advisor that will expose the given bean name, introducing
   * the NamedBean interface to make the bean name accessible without forcing
   * the target object to be aware of this IoC concept.
   *
   * @param beanName the bean name to expose
   */
  public static Advisor createAdvisorIntroducingNamedBean(String beanName) {
    return new DefaultIntroductionAdvisor(new ExposeBeanNameIntroduction(beanName));
  }

  /**
   * Interceptor that exposes the specified bean name as invocation attribute.
   */
  private record ExposeBeanNameInterceptor(String beanName) implements MethodInterceptor {

    @Override
    @Nullable
    public Object invoke(MethodInvocation mi) throws Throwable {
      if (!(mi instanceof ProxyMethodInvocation pmi)) {
        throw new IllegalStateException("MethodInvocation is not a ProxyMethodInvocation: " + mi);
      }
      pmi.setAttribute(BEAN_NAME_ATTRIBUTE, this.beanName);
      return mi.proceed();
    }
  }

  /**
   * Introduction that exposes the specified bean name as invocation attribute.
   */
  private static class ExposeBeanNameIntroduction extends DelegatingIntroductionInterceptor implements NamedBean {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String beanName;

    public ExposeBeanNameIntroduction(String beanName) {
      this.beanName = beanName;
    }

    @Override
    @Nullable
    public Object invoke(MethodInvocation mi) throws Throwable {
      if (!(mi instanceof ProxyMethodInvocation pmi)) {
        throw new IllegalStateException("MethodInvocation is not a ProxyMethodInvocation: " + mi);
      }
      pmi.setAttribute(BEAN_NAME_ATTRIBUTE, this.beanName);
      return super.invoke(mi);
    }

    @Override
    public String getBeanName() {
      return this.beanName;
    }
  }

}
