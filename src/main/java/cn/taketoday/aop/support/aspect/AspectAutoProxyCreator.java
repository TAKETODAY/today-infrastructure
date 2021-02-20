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

package cn.taketoday.aop.support.aspect;

import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.annotation.Advice;
import cn.taketoday.aop.annotation.Aspect;
import cn.taketoday.aop.proxy.DefaultAutoProxyCreator;
import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.ConfigurableBeanFactory;
import cn.taketoday.context.listener.ApplicationListener;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.context.utils.OrderUtils;
import cn.taketoday.context.utils.ReflectionUtils;

/**
 * @author TODAY 2021/2/19 23:55
 */
public class AspectAutoProxyCreator
        extends DefaultAutoProxyCreator implements ApplicationListener<ContextCloseEvent> {
  private static final long serialVersionUID = 1L;

  private boolean aspectsLoaded;
  private final List<Object> aspects = new ArrayList<>();

  public void addAspect(Object aspect) {
    aspects.add(aspect);
  }

  public void sortAspects() {
    OrderUtils.reversedSort(aspects);
  }

  public boolean isAspectsLoaded() {
    return aspectsLoaded;
  }

  public void setAspectsLoaded(boolean aspectsLoaded) {
    this.aspectsLoaded = aspectsLoaded;
  }

  void loadAspects() {
    if (!aspectsLoaded) {
      loadAspects((ConfigurableBeanFactory) getBeanFactory());
    }
  }

  public void loadAspects(final ConfigurableBeanFactory applicationContext) {
    final Logger log = LoggerFactory.getLogger(getClass());
    log.debug("Loading Aspect Objects");

    setAspectsLoaded(true);
    try {
      for (final BeanDefinition beanDefinition : applicationContext.getBeanDefinitions().values()) {

        if (beanDefinition.isAnnotationPresent(Aspect.class)) {
          // fix use beanDefinition.getName()
          final String aspectName = beanDefinition.getName();
          log.debug("Found Aspect: [{}]", aspectName);

          Object aspectInstance = applicationContext.getSingleton(aspectName);
          if (aspectInstance == null) {
            aspectInstance = ClassUtils.newInstance(beanDefinition, applicationContext);
            applicationContext.registerSingleton(aspectName, aspectInstance);
          }
          addAspect(aspectInstance);
        }
      }
      sortAspects();
    }
    catch (Throwable e) {
      throw new ConfigurationException(e);
    }
  }

  public List<Object> getAspects() {
    return aspects;
  }

  @Override
  public void onApplicationEvent(ContextCloseEvent event) {
    log.info("Destroying Aspects Objects");

    aspects.clear();
    setAspectsLoaded(false);
  }

  //

  @Override
  protected void postCandidateAdvisors(List<Advisor> candidateAdvisors) {
    super.postCandidateAdvisors(candidateAdvisors);
    loadAspects();

    for (final Object aspect : aspects) {
      final Class<?> aspectClass = aspect.getClass();
      if (aspect instanceof MethodInterceptor) {
        final Advice[] advices = ClassUtils.getAnnotationArray(aspectClass, Advice.class);
        addAdvidors(candidateAdvisors, aspect, null, advices);
      }

      final Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(aspectClass);
      for (final Method aspectMethod : declaredMethods) {
        final Advice[] advices = ClassUtils.getAnnotationArray(aspectMethod, Advice.class);
        addAdvidors(candidateAdvisors, aspect, aspectMethod, advices);
      }
    }

  }

  private void addAdvidors(List<Advisor> candidateAdvisors, Object aspect, Method aspectMethod, Advice[] advices) {
    if (ObjectUtils.isNotEmpty(advices)) {
      for (final Advice advice : advices) {
        final Class<? extends Annotation>[] annotations = advice.value();
        final Class<? extends MethodInterceptor> interceptor = advice.interceptor();
        MethodInterceptor methodInterceptor;
        if (aspectMethod == null) { // method interceptor
          if (!(aspect instanceof MethodInterceptor)) {
            throw new ConfigurationException(
                    '[' + aspect.getClass().getName() +
                            "] must be implement: [" + MethodInterceptor.class.getName() + ']');
          }
          methodInterceptor = (MethodInterceptor) aspect;
        }
        else {
          methodInterceptor = getInterceptor(aspect, aspectMethod, interceptor);
        }

        if (log.isTraceEnabled()) {
          log.trace("Found Interceptor: [{}]", methodInterceptor);
        }

        for (final Class<? extends Annotation> annotation : annotations) {
          final AnnotationMatchingPointcut matchingPointcut = AnnotationMatchingPointcut.forMethodAnnotation(annotation);
          final DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor(matchingPointcut, methodInterceptor);
          candidateAdvisors.add(pointcutAdvisor);
        }
      }
    }
  }

  /**
   * Get an advice instance
   *
   * @param aspect
   *         aspect instance
   * @param aspectMethod
   *         current aspect method
   * @param interceptor
   *         interceptor type
   */
  public MethodInterceptor getInterceptor(final Object aspect,
                                          final Method aspectMethod,
                                          final Class<? extends MethodInterceptor> interceptor) //
  {
    if (interceptor == AbstractAspectAdvice.class || !MethodInterceptor.class.isAssignableFrom(interceptor)) {
      throw new ConfigurationException(
              "You must be implement: [" + AbstractAspectAdvice.class.getName() +
                      "] or [" + MethodInterceptor.class.getName() + "]");
    }

    if (AbstractAspectAdvice.class.isAssignableFrom(interceptor)) {
      final Constructor<? extends MethodInterceptor> constructor = ClassUtils.obtainConstructor(interceptor);
      return ClassUtils.newInstance(constructor, new Object[] { aspectMethod, aspect });
    }

    // fix
    if (ClassUtils.isAnnotationPresent(interceptor, Component.class)) {
      MethodInterceptor ret = getBeanFactory().getBean(interceptor);
      if (ret != null) {
        return ret;
      }
    }
    return ClassUtils.newInstance(interceptor, getBeanFactory());
  }
}
