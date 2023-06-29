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

package cn.taketoday.transaction.annotation;

import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.MemberCategory;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotProcessor;
import cn.taketoday.beans.factory.aot.BeanRegistrationCode;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * AOT {@code BeanRegistrationAotProcessor} that detects the presence of
 * {@link Transactional @Transactional} on annotated elements and creates
 * the required reflection hints.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionRuntimeHints
 * @since 4.0 2023/6/29 11:24
 */
class TransactionBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  private final static String JAKARTA_TRANSACTIONAL_CLASS_NAME = "jakarta.transaction.Transactional";

  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Class<?> beanClass = registeredBean.getBeanClass();
    if (isTransactional(beanClass)) {
      return new TransactionBeanRegistrationAotContribution(beanClass);
    }
    return null;
  }

  private boolean isTransactional(Class<?> beanClass) {
    Set<AnnotatedElement> elements = new LinkedHashSet<>();
    elements.add(beanClass);
    ReflectionUtils.doWithMethods(beanClass, elements::add);
    for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClass(beanClass)) {
      elements.add(interfaceClass);
      ReflectionUtils.doWithMethods(interfaceClass, elements::add);
    }
    return elements.stream().anyMatch(element -> {
      MergedAnnotations mergedAnnotations = MergedAnnotations.from(element, SearchStrategy.TYPE_HIERARCHY);
      return mergedAnnotations.isPresent(Transactional.class)
              || mergedAnnotations.isPresent(JAKARTA_TRANSACTIONAL_CLASS_NAME);
    });
  }

  private static class TransactionBeanRegistrationAotContribution implements BeanRegistrationAotContribution {

    private final Class<?> beanClass;

    public TransactionBeanRegistrationAotContribution(Class<?> beanClass) {
      this.beanClass = beanClass;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
      RuntimeHints runtimeHints = generationContext.getRuntimeHints();
      Class<?>[] proxyInterfaces = ClassUtils.getAllInterfacesForClass(this.beanClass);
      if (ObjectUtils.isNotEmpty(proxyInterfaces)) {
        for (Class<?> proxyInterface : proxyInterfaces) {
          runtimeHints.reflection().registerType(proxyInterface, MemberCategory.INVOKE_DECLARED_METHODS);
        }
      }
    }
  }

}

