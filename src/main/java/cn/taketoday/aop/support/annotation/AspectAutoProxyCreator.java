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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.proxy.DefaultAutoProxyCreator;
import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.SuppliedMethodInterceptor;
import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.AutowireCapableBeanFactory;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.ObjectSupplier;
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

  private final List<BeanDefinition> aspectDefs = new ArrayList<>();

  public void sortAspects() {
    OrderUtils.reversedSort(aspectDefs);
  }

  public boolean isAspectsLoaded() {
    return aspectsLoaded;
  }

  public void setAspectsLoaded(boolean aspectsLoaded) {
    this.aspectsLoaded = aspectsLoaded;
  }

  public void loadAspects(final BeanFactory beanFactory) {
    log.info("Loading aspect bean definitions");
    setAspectsLoaded(true);

    for (final BeanDefinition beanDefinition : beanFactory.getBeanDefinitions().values()) {
      if (beanDefinition.isAnnotationPresent(Aspect.class)) {
        // fix use beanDefinition.getName()
        final String aspectName = beanDefinition.getName();
        log.debug("Found Aspect: [{}]", aspectName);

        aspectDefs.add(beanDefinition);
      }
    }

    sortAspects();
  }

  @Override
  public void onApplicationEvent(ContextCloseEvent event) {
    log.info("Removing aspects");

    aspectDefs.clear();
    setAspectsLoaded(false);
  }

  //
  private void loadAspects() {
    if (!aspectsLoaded) {
      loadAspects(getBeanFactory());
    }
  }

  @Override
  protected void addCandidateAdvisors(List<Advisor> candidateAdvisors) {
    super.addCandidateAdvisors(candidateAdvisors);
    loadAspects();

    for (final BeanDefinition aspectDef : aspectDefs) {
      final Class<?> aspectClass = aspectDef.getBeanClass();
      // around
      if (MethodInterceptor.class.isAssignableFrom(aspectClass)) {
        final AnnotationAttributes[] adviceAttributes = getAdviceAttributes(aspectDef);
        addCandidateAdvisors(candidateAdvisors, aspectDef, null, adviceAttributes);
      }
      // annotations: @AfterReturning @Around @Before @After @AfterThrowing
      final Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(aspectClass);
      for (final Method aspectMethod : declaredMethods) {
        final AnnotationAttributes[] adviceAttributes = getAdviceAttributes(aspectMethod);
        addCandidateAdvisors(candidateAdvisors, aspectDef, aspectMethod, adviceAttributes);
      }
    }
  }

  private void addCandidateAdvisors(List<Advisor> candidateAdvisors, BeanDefinition aspectDef,
                                    Method aspectMethod, AnnotationAttributes[] adviceAttributes) {
    // fix Standard Bean def
    if (ObjectUtils.isNotEmpty(adviceAttributes)) {
      for (final AnnotationAttributes advice : adviceAttributes) {
        MethodInterceptor interceptor = getInterceptor(aspectDef, aspectMethod, advice);
        if (log.isTraceEnabled()) {
          log.trace("Found Interceptor: [{}]", interceptor);
        }

        // Annotations
        final Class<? extends Annotation>[] annotations = advice.getClassArray(Constant.VALUE);
        if (ObjectUtils.isNotEmpty(annotations)) {
          for (final Class<? extends Annotation> annotation : annotations) {
            final AnnotationMatchingPointcut matchingPointcut
                    = AnnotationMatchingPointcut.forMethodAnnotation(annotation);

            final DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor(matchingPointcut, interceptor);
            candidateAdvisors.add(pointcutAdvisor);
          }
        }
      }
    }
  }

  private MethodInterceptor getInterceptor(BeanDefinition aspectDef, Method aspectMethod, AnnotationAttributes advice) {
    final BeanFactory beanFactory = getBeanFactory();

    if (aspectMethod == null) { // method interceptor
      if (!(MethodInterceptor.class.isAssignableFrom(aspectDef.getBeanClass()))) {
        throw new ConfigurationException(
                '[' + aspectDef.getBeanClass().getName() +
                        "] must be implement: [" + MethodInterceptor.class.getName() + ']');
      }
      // aspect is a method interceptor
      return getMethodInterceptor(beanFactory, aspectDef);
    }

    // -----------------
    // invoke advice method that annotated: @AfterReturning @Around @Before @After @AfterThrowing
    final Class<? extends MethodInterceptor> interceptor = advice.getClass("interceptor");
    if (interceptor == AbstractAnnotationMethodInterceptor.class
            || !MethodInterceptor.class.isAssignableFrom(interceptor)) {
      throw new ConfigurationException(
              "You must be implement: [" + AbstractAnnotationMethodInterceptor.class.getName() +
                      "] or [" + MethodInterceptor.class.getName() + "]");
    }

    // exist in bean factory ?
    if (ClassUtils.isAnnotationPresent(interceptor, Component.class)) {
      if (beanFactory instanceof BeanDefinitionRegistry) {
        final BeanDefinition interceptorDef = ((BeanDefinitionRegistry) beanFactory).getBeanDefinition(interceptor);
        if (interceptorDef != null) {
          // exist in bean factory
          return getMethodInterceptor(beanFactory, interceptorDef);
        }
      }
      else {
        // just get bean
        MethodInterceptor ret = beanFactory.getBean(interceptor);
        if (ret != null) {
          return ret;
        }
      }
    }

    // dynamic parameters -> aspectMethod, def, beanFactory
    final MethodInterceptor ret = ClassUtils.newInstance(
            interceptor, beanFactory, new Object[] { aspectMethod, aspectDef, beanFactory });

    if (beanFactory instanceof AutowireCapableBeanFactory) {
      ((AutowireCapableBeanFactory) beanFactory).autowireBean(ret);
    }
    return ret;
  }

  private MethodInterceptor getMethodInterceptor(BeanFactory beanFactory, BeanDefinition interceptorDef) {
    if (interceptorDef.isSingleton() && !interceptorDef.isLazyInit()) {
      return (MethodInterceptor) beanFactory.getBean(interceptorDef);
    }
    else {
      ObjectSupplier<MethodInterceptor> supplier = beanFactory.getBeanSupplier(interceptorDef);
      return new SuppliedMethodInterceptor(supplier); // lazy load or prototype
    }
  }

  private AnnotationAttributes[] getAdviceAttributes(AnnotatedElement annotated) {
    return ClassUtils.getAnnotationAttributesArray(annotated, Advice.class);
  }

}
