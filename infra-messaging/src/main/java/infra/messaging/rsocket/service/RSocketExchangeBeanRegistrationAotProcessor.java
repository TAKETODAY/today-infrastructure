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

package infra.messaging.rsocket.service;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import infra.aop.framework.AopProxyUtils;
import infra.aot.generate.GenerationContext;
import infra.aot.hint.ProxyHints;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.aot.BeanRegistrationAotProcessor;
import infra.beans.factory.aot.BeanRegistrationCode;
import infra.beans.factory.support.RegisteredBean;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.Search;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

import static infra.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY;

/**
 * An AOT {@link BeanRegistrationAotProcessor} that detects the presence of
 * {@link RSocketExchange @RSocketExchange} on methods and creates the required
 * proxy hints.
 *
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @see infra.web.service.annotation.HttpExchangeBeanRegistrationAotProcessor
 * @since 5.0
 */
class RSocketExchangeBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  @Override
  public @Nullable BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Class<?> beanClass = registeredBean.getBeanClass();
    Set<Class<?>> exchangeInterfaces = new HashSet<>();
    Search search = MergedAnnotations.search(TYPE_HIERARCHY);
    for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClass(beanClass)) {
      ReflectionUtils.doWithMethods(interfaceClass, method -> {
        if (!exchangeInterfaces.contains(interfaceClass) &&
                search.from(method).isPresent(RSocketExchange.class)) {
          exchangeInterfaces.add(interfaceClass);
        }
      });
    }
    if (!exchangeInterfaces.isEmpty()) {
      return new AotContribution(exchangeInterfaces);
    }
    return null;
  }

  private static class AotContribution implements BeanRegistrationAotContribution {

    private final Set<Class<?>> rSocketExchangeInterfaces;

    public AotContribution(Set<Class<?>> rSocketExchangeInterfaces) {
      this.rSocketExchangeInterfaces = rSocketExchangeInterfaces;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
      ProxyHints proxyHints = generationContext.getRuntimeHints().proxies();
      for (Class<?> rSocketExchangeInterface : this.rSocketExchangeInterfaces) {
        proxyHints.registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(rSocketExchangeInterface));
      }
    }

  }

}
