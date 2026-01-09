/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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

