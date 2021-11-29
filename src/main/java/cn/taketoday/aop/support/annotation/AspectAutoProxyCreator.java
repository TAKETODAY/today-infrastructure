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

import org.aopalliance.intercept.MethodInterceptor;

import java.io.Serial;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.proxy.DefaultAutoProxyCreator;
import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.SuppliedMethodInterceptor;
import cn.taketoday.beans.factory.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.support.BeanUtils;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * Aspect annotated aspect or MethodInterceptor ProxyCreator
 *
 * @author TODAY 2021/2/19 23:55
 * @see Aspect
 * @since 3.0
 */
public class AspectAutoProxyCreator extends DefaultAutoProxyCreator {
  @Serial
  private static final long serialVersionUID = 1L;

  @Override
  protected void addCandidateAdvisors(List<Advisor> candidateAdvisors) {
    super.addCandidateAdvisors(candidateAdvisors);

    BeanFactory beanFactory = getBeanFactory();
    Set<String> aspectBeanNames = beanFactory.getBeanNamesForAnnotation(Aspect.class);
    for (String name : aspectBeanNames) {
      log.info("Found Aspect: [{}]", name);
      Class<?> aspectClass = beanFactory.getType(name);
      Assert.state(aspectClass != null, "Cannot determine bean type");
      BeanDefinition aspectDef = beanFactory.getBeanDefinition(name);

      // around
      if (MethodInterceptor.class.isAssignableFrom(aspectClass)) {
        Stream<MergedAnnotation<Advice>> adviceAttributes = getAdviceAttributes(aspectDef);
        addCandidateAdvisors(candidateAdvisors, aspectDef, null, adviceAttributes);
      }
      // annotations: @AfterReturning @Around @Before @After @AfterThrowing
      Method[] declaredMethods = ReflectionUtils.getDeclaredMethods(aspectClass);
      for (Method aspectMethod : declaredMethods) {
        Stream<MergedAnnotation<Advice>> adviceAttributes = getAdviceAttributes(aspectMethod);
        addCandidateAdvisors(candidateAdvisors, aspectDef, aspectMethod, adviceAttributes);
      }
    }
  }

  private void addCandidateAdvisors(
          List<Advisor> candidateAdvisors, BeanDefinition aspectDef,
          @Nullable Method aspectMethod, Stream<MergedAnnotation<Advice>> adviceAttributes) {
    // fix Standard Bean def

    adviceAttributes.forEach(advice -> {
      MethodInterceptor interceptor = getInterceptor(aspectDef, aspectMethod, advice);
      if (log.isTraceEnabled()) {
        log.trace("Found Interceptor: [{}]", interceptor);
      }

      // Annotations
      Class<? extends Annotation>[] annotations = advice.getClassArray(MergedAnnotation.VALUE);
      if (ObjectUtils.isNotEmpty(annotations)) {
        for (Class<? extends Annotation> annotation : annotations) {
          AnnotationMatchingPointcut matchingPointcut
                  = AnnotationMatchingPointcut.forMethodAnnotation(annotation);

          DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor(matchingPointcut, interceptor);
          candidateAdvisors.add(pointcutAdvisor);
        }
      }
    });

  }

  private MethodInterceptor getInterceptor(
          BeanDefinition aspectDef, @Nullable Method aspectMethod, MergedAnnotation<Advice> advice) {
    BeanFactory beanFactory = getBeanFactory();

    if (aspectMethod == null) { // method interceptor
      if (!beanFactory.isTypeMatch(aspectDef.getName(), MethodInterceptor.class)) {
        throw new ConfigurationException(
                '[' + aspectDef.getBeanClassName() +
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
    if (AnnotatedElementUtils.isAnnotated(interceptor, Component.class)) {
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

    // dynamic parameters -> aspectMethod, beanName, beanFactory
    MethodInterceptor ret = BeanUtils.newInstance(
            interceptor, beanFactory, new Object[] { aspectMethod, aspectDef.getName(), beanFactory });

    if (beanFactory instanceof AutowireCapableBeanFactory) {
      ((AutowireCapableBeanFactory) beanFactory).autowireBean(ret);
    }
    return ret;
  }

  private MethodInterceptor getMethodInterceptor(BeanFactory beanFactory, BeanDefinition interceptorDef) {
    if (interceptorDef.isSingleton() && !interceptorDef.isLazyInit()) {
      return beanFactory.getBean(interceptorDef.getName(), MethodInterceptor.class);
    }
    else {
      return new SuppliedMethodInterceptor(beanFactory, interceptorDef); // lazy load or prototype
    }
  }

  private Stream<MergedAnnotation<Advice>> getAdviceAttributes(Method aspectMethod) {
    MergedAnnotations annotations = MergedAnnotations.from(aspectMethod);
    return annotations.stream(Advice.class);
  }

  private Stream<MergedAnnotation<Advice>> getAdviceAttributes(BeanDefinition definition) {
    if (definition instanceof AnnotatedBeanDefinition) {
      AnnotationMetadata metadata = ((AnnotatedBeanDefinition) definition).getMetadata();
      return metadata.getAnnotations().stream(Advice.class);
    }

    BeanFactory beanFactory = getBeanFactory();

    //stream
    return Stream.of(beanFactory.getMergedAnnotation(definition.getName(), Advice.class));
  }

}
