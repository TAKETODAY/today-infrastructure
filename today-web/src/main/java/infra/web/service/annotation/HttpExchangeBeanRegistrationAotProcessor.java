/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.web.service.annotation;

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
import infra.lang.Nullable;
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
