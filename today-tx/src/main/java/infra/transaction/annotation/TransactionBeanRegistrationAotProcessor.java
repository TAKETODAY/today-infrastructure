/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.transaction.annotation;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.aot.BeanRegistrationCode;
import infra.beans.factory.support.RegisteredBean;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.util.ClassUtils;
import infra.util.ObjectUtils;
import infra.util.ReflectionUtils;

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

  private static final String JAKARTA_TRANSACTIONAL_CLASS_NAME = "jakarta.transaction.Transactional";

  @Nullable
  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Class<?> beanClass = registeredBean.getBeanClass();
    if (isTransactional(beanClass)) {
      return new AotContribution(beanClass);
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

  private static final class AotContribution implements BeanRegistrationAotContribution {

    private final Class<?> beanClass;

    public AotContribution(Class<?> beanClass) {
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

