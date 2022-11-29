/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.web;

import java.util.ArrayList;
import java.util.Map;

import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.handler.HandlerExecutionChain;
import cn.taketoday.web.handler.function.support.RouterFunctionMapping;
import cn.taketoday.web.handler.method.RequestMappingHandlerMapping;
import cn.taketoday.web.registry.AbstractHandlerMapping;
import cn.taketoday.web.registry.BeanNameUrlHandlerMapping;
import cn.taketoday.web.registry.HandlerRegistries;

/**
 * Interface to be implemented by objects that define a mapping between
 * requests and handler objects.
 *
 * <p>This class can be implemented by application developers, although this is not
 * necessary, as {@link BeanNameUrlHandlerMapping} and {@link RequestMappingHandlerMapping}
 * are included in the framework. The former is the default if no HandlerMapping bean
 * is registered in the application context.
 *
 * <p>HandlerMapping implementations can support mapped interceptors but do not
 * have to. A handler will always be wrapped in a {@link HandlerExecutionChain}
 * instance, optionally accompanied by some {@link HandlerInterceptor} instances.
 * The DispatcherHandler will first call each HandlerInterceptor's
 * {@code preHandle} method in the given order, finally invoking the handler
 * itself if all {@code preHandle} methods have returned {@code true}.
 *
 * <p>The ability to parameterize this mapping is a powerful and unusual
 * capability of this MVC framework. For example, it is possible to write
 * a custom mapping based on session state, cookie state or many other
 * variables. No other MVC framework seems to be equally flexible.
 *
 * <p>Note: Implementations can implement the {@link Ordered}
 * interface to be able to specify a sorting order and thus a priority for getting
 * applied by DispatcherHandler. Non-Ordered instances get treated as lowest priority.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2019-12-08 23:06
 * @see Ordered
 * @see AbstractHandlerMapping
 * @see BeanNameUrlHandlerMapping
 * @see RequestMappingHandlerMapping
 */
@FunctionalInterface
public interface HandlerMapping {

  /**
   * Well-known name for the HandlerMapping object in the bean factory for this namespace.
   * Only used when "detectAllHandlerMappings" is turned off.
   *
   * @see cn.taketoday.web.handler.DispatcherHandler#setDetectAllHandlerMapping(boolean)
   */
  String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

  /**
   * Return a handler and any interceptors for this request. The choice may be made
   * on request URL, session state, or any factor the implementing class chooses.
   * <p>The returned HandlerExecutionChain contains a handler Object, rather than
   * even a tag interface, so that handlers are not constrained in any way.
   * For example, a HandlerAdapter could be written to allow another framework's
   * handler objects to be used.
   * <p>Returns {@code null} if no match was found. This is not an error.
   * The DispatcherServlet will query all registered HandlerMapping beans to find
   * a match, and only decide there is an error if none can find a handler.
   *
   * @param request Current request context
   * @return a fgA instance containing handler object and
   * any interceptors, or {@code null} if no mapping found
   * @throws Exception if there is an internal error
   */
  @Nullable
  Object getHandler(RequestContext request) throws Exception;

  // static factory method

  static HandlerMapping find(ApplicationContext context) {
    return find(context, true);
  }

  static HandlerMapping find(ApplicationContext context, boolean detectAllHandlerMapping) {
    if (detectAllHandlerMapping) {
      // Find all HandlerMappings in the ApplicationContext, including ancestor contexts.
      Map<String, HandlerMapping> matchingBeans =
              BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
      if (!matchingBeans.isEmpty()) {
        ArrayList<HandlerMapping> registries = new ArrayList<>(matchingBeans.values());
        // We keep HandlerRegistries in sorted order.
        AnnotationAwareOrderComparator.sort(registries);
        return registries.size() == 1
               ? registries.get(0)
               : new HandlerRegistries(registries);
      }
    }
    else {
      HandlerMapping handlerMapping = BeanFactoryUtils.find(context, HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
      if (handlerMapping != null) {
        return handlerMapping;
      }
    }

    AutowireCapableBeanFactory factory = context.getAutowireCapableBeanFactory();
    return new HandlerRegistries(
            factory.createBean(RequestMappingHandlerMapping.class),
            factory.createBean(RouterFunctionMapping.class),
            factory.createBean(BeanNameUrlHandlerMapping.class)
    );
  }

}
