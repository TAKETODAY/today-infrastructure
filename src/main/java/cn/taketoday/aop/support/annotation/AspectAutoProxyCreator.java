/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.aop.support.annotation;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.annotation.Advice;
import cn.taketoday.aop.annotation.Aspect;
import cn.taketoday.aop.proxy.DefaultAutoProxyCreator;
import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.SuppliedMethodInterceptor;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.AutowireCapableBeanFactory;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * @author TODAY 2021/2/19 23:55
 * @since 3.0
 */
public class AspectAutoProxyCreator
        extends DefaultAutoProxyCreator implements ApplicationListener<ContextCloseEvent> {
  private static final long serialVersionUID = 1L;

  private boolean aspectsLoaded;

  private final List<BeanDefinition> aspectsDef = new ArrayList<>();

  public void sortAspects() {
    OrderUtils.reversedSort(aspectsDef);
  }

  public boolean isAspectsLoaded() {
    return aspectsLoaded;
  }

  public void setAspectsLoaded(boolean aspectsLoaded) {
    this.aspectsLoaded = aspectsLoaded;
  }

  void loadAspects() {
    if (!aspectsLoaded) {
      loadAspects(getBeanFactory());
    }
  }

  public void loadAspects(final BeanFactory beanFactory) {
    log.debug("Loading Aspect Bean Definition");

    setAspectsLoaded(true);

    for (final BeanDefinition beanDefinition : beanFactory.getBeanDefinitions().values()) {
      if (beanDefinition.isAnnotationPresent(Aspect.class)) {
        // fix use beanDefinition.getName()
        final String aspectName = beanDefinition.getName();
        log.debug("Found Aspect: [{}]", aspectName);

        aspectsDef.add(beanDefinition);
      }
    }

    sortAspects();
  }

  @Override
  public void onApplicationEvent(ContextCloseEvent event) {
    log.info("Removing Aspects");

    aspectsDef.clear();
    setAspectsLoaded(false);
  }

  //

  @Override
  protected void addCandidateAdvisors(List<Advisor> candidateAdvisors) {
    super.addCandidateAdvisors(candidateAdvisors);
    loadAspects();

    for (final BeanDefinition def : aspectsDef) {
      final Class<?> aspectClass = def.getBeanClass();
      // around
      if (MethodInterceptor.class.isAssignableFrom(aspectClass)) {
        final Advice[] advices = ClassUtils.getAnnotationArray(aspectClass, Advice.class);
        addCandidateAdvisors(candidateAdvisors, def, null, advices);
      }
      // annotations: @AfterReturning @Around @Before @After @AfterThrowing
      final Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(aspectClass);
      for (final Method aspectMethod : declaredMethods) {
        final Advice[] advices = ClassUtils.getAnnotationArray(aspectMethod, Advice.class);
        addCandidateAdvisors(candidateAdvisors, def, aspectMethod, advices);
      }
    }

  }

  private void addCandidateAdvisors(List<Advisor> candidateAdvisors, BeanDefinition aspectDef, Method aspectMethod, Advice[] advices) {
    if (ObjectUtils.isNotEmpty(advices)) {
      for (final Advice advice : advices) {
        final Class<? extends MethodInterceptor> interceptor = advice.interceptor();
        MethodInterceptor methodInterceptor;
        if (aspectMethod == null) { // method interceptor
          if (!(MethodInterceptor.class.isAssignableFrom(aspectDef.getBeanClass()))) {
            throw new ConfigurationException(
                    '[' + aspectDef.getBeanClass().getName() +
                            "] must be implement: [" + MethodInterceptor.class.getName() + ']');
          }

          final BeanFactory beanFactory = getBeanFactory();
          Supplier<MethodInterceptor> beanSupplier = beanFactory.getBeanSupplier(aspectDef);
          methodInterceptor = new SuppliedMethodInterceptor(beanSupplier);
        }
        else {
          methodInterceptor = getInterceptor(aspectDef, aspectMethod, interceptor);
        }

        if (log.isTraceEnabled()) {
          log.trace("Found Interceptor: [{}]", methodInterceptor);
        }

        // Annotations
        final Class<? extends Annotation>[] annotations = advice.value();
        if (ObjectUtils.isNotEmpty(annotations)) {
          for (final Class<? extends Annotation> annotation : annotations) {
            final AnnotationMatchingPointcut matchingPointcut = AnnotationMatchingPointcut.forMethodAnnotation(annotation);
            final DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor(matchingPointcut, methodInterceptor);
            candidateAdvisors.add(pointcutAdvisor);
          }
        }
      }
    }
  }

  /**
   * Get an advice instance
   *
   * @param def
   *         aspect {@link BeanDefinition}
   * @param aspectMethod
   *         current aspect method
   * @param interceptor
   *         interceptor type
   */
  public MethodInterceptor getInterceptor(final BeanDefinition def,
                                          final Method aspectMethod,
                                          final Class<? extends MethodInterceptor> interceptor) //
  {
    if (interceptor == AbstractAnnotationMethodInterceptor.class || !MethodInterceptor.class.isAssignableFrom(interceptor)) {
      throw new ConfigurationException(
              "You must be implement: [" + AbstractAnnotationMethodInterceptor.class.getName() +
                      "] or [" + MethodInterceptor.class.getName() + "]");
    }

    final BeanFactory beanFactory = getBeanFactory();
    if (ClassUtils.isAnnotationPresent(interceptor, Component.class)) {
      MethodInterceptor ret = beanFactory.getBean(interceptor);
      if (ret != null) {
        return ret;
      }
    }

    // dynamic parameters -> aspectMethod, def
    final MethodInterceptor ret = ClassUtils.newInstance(interceptor, beanFactory, new Object[] { aspectMethod, def });
    if (beanFactory instanceof AutowireCapableBeanFactory) {
      ((AutowireCapableBeanFactory) beanFactory).autowireBean(ret);
    }
    return ret;
  }
}
