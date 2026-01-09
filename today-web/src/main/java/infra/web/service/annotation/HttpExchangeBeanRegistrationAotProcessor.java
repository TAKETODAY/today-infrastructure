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

package infra.web.service.annotation;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
 * AOT {@code BeanRegistrationAotProcessor} that detects the presence of
 * {@link HttpExchange @HttpExchange} on methods and creates the required proxy
 * hints.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class HttpExchangeBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

  @Nullable
  @Override
  public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
    Class<?> beanClass = registeredBean.getBeanClass();
    List<Class<?>> exchangeInterfaces = new ArrayList<>();
    Search search = MergedAnnotations.search(TYPE_HIERARCHY);
    for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClass(beanClass)) {
      ReflectionUtils.doWithMethods(interfaceClass, method -> {
        if (!exchangeInterfaces.contains(interfaceClass) &&
                search.from(method).isPresent(HttpExchange.class)) {
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

    private final List<Class<?>> httpExchangeInterfaces;

    public AotContribution(List<Class<?>> httpExchangeInterfaces) {
      this.httpExchangeInterfaces = httpExchangeInterfaces;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanRegistrationCode beanRegistrationCode) {
      ProxyHints proxyHints = generationContext.getRuntimeHints().proxies();
      for (Class<?> httpExchangeInterface : this.httpExchangeInterfaces) {
        proxyHints.registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(httpExchangeInterface));
      }
    }

  }

}
