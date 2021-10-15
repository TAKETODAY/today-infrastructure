/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.proxy.DefaultAutoProxyCreator;
import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.SuppliedMethodInterceptor;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.context.event.ApplicationListener;
import cn.taketoday.context.event.ContextCloseEvent;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import org.aopalliance.intercept.MethodInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Aspect annotated aspect or MethodInterceptor ProxyCreator
 *
 * @author TODAY 2021/2/19 23:55
 * @see Aspect
 * @since 3.0
 */
public class AspectAutoProxyCreator
        extends DefaultAutoProxyCreator implements ApplicationListener<ContextCloseEvent> {
  private static final long serialVersionUID = 1L;

  private boolean aspectsLoaded;

  private final List<BeanDefinition> aspectDefs = new ArrayList<>();

  public void sortAspects() {
    AnnotationAwareOrderComparator.sort(aspectDefs);
  }

  public boolean isAspectsLoaded() {
    return aspectsLoaded;
  }

  public void setAspectsLoaded(boolean aspectsLoaded) {
    this.aspectsLoaded = aspectsLoaded;
  }

  public void loadAspects(BeanFactory beanFactory) {
    log.info("Loading aspect bean definitions");
    setAspectsLoaded(true);

    for (BeanDefinition beanDefinition : beanFactory.getBeanDefinitions().values()) {
      if (beanDefinition.isAnnotationPresent(Aspect.class)) {
        // fix use beanDefinition.getName()
        String aspectName = beanDefinition.getName();
        log.info("Found Aspect: [{}]", aspectName);

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

    for (BeanDefinition aspectDef : aspectDefs) {
      Class<?> aspectClass = aspectDef.getBeanClass();
      // around
      if (MethodInterceptor.class.isAssignableFrom(aspectClass)) {
        AnnotationAttributes[] adviceAttributes = getAdviceAttributes(aspectDef);
        addCandidateAdvisors(candidateAdvisors, aspectDef, null, adviceAttributes);
      }
      // annotations: @AfterReturning @Around @Before @After @AfterThrowing
      Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(aspectClass);
      for (Method aspectMethod : declaredMethods) {
        AnnotationAttributes[] adviceAttributes = getAdviceAttributes(aspectMethod);
        addCandidateAdvisors(candidateAdvisors, aspectDef, aspectMethod, adviceAttributes);
      }
    }
  }

  private void addCandidateAdvisors(
          List<Advisor> candidateAdvisors, BeanDefinition aspectDef,
          @Nullable Method aspectMethod, AnnotationAttributes[] adviceAttributes) {
    // fix Standard Bean def
    if (ObjectUtils.isNotEmpty(adviceAttributes)) {
      for (AnnotationAttributes advice : adviceAttributes) {
        MethodInterceptor interceptor = getInterceptor(aspectDef, aspectMethod, advice);
        if (log.isTraceEnabled()) {
          log.trace("Found Interceptor: [{}]", interceptor);
        }

        // Annotations
        Class<? extends Annotation>[] annotations = advice.getClassArray(Constant.VALUE);
        if (ObjectUtils.isNotEmpty(annotations)) {
          for (Class<? extends Annotation> annotation : annotations) {
            AnnotationMatchingPointcut matchingPointcut
                    = AnnotationMatchingPointcut.forMethodAnnotation(annotation);

            DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor(matchingPointcut, interceptor);
            candidateAdvisors.add(pointcutAdvisor);
          }
        }
      }
    }
  }

  private MethodInterceptor getInterceptor(
          BeanDefinition aspectDef, @Nullable Method aspectMethod, AnnotationAttributes advice) {
    BeanFactory beanFactory = getBeanFactory();

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
    Class<? extends MethodInterceptor> interceptor = advice.getClass("interceptor");
    if (interceptor == AbstractAnnotationMethodInterceptor.class
            || !MethodInterceptor.class.isAssignableFrom(interceptor)) {
      throw new ConfigurationException(
              interceptor + " must be implement: [" + AbstractAnnotationMethodInterceptor.class.getName() +
                      "] or [" + MethodInterceptor.class.getName() + "]");
    }

    // exist in bean factory ?
    if (AnnotationUtils.isPresent(interceptor, Component.class)) {
      if (beanFactory instanceof BeanDefinitionRegistry) {
        BeanDefinition interceptorDef = ((BeanDefinitionRegistry) beanFactory).getBeanDefinition(interceptor);
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
    MethodInterceptor ret = BeanUtils.newInstance(
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
      ObjectSupplier<MethodInterceptor> supplier = beanFactory.getObjectSupplier(interceptorDef);
      return new SuppliedMethodInterceptor(supplier); // lazy load or prototype
    }
  }

  private AnnotationAttributes[] getAdviceAttributes(AnnotatedElement annotated) {
    return AnnotationUtils.getAttributesArray(annotated, Advice.class);
  }

}
