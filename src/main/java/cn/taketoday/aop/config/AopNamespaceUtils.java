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

package cn.taketoday.aop.config;

import org.w3c.dom.Element;

import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.context.annotation.AutoProxyRegistrar;
import cn.taketoday.lang.Nullable;

/**
 * Utility class for handling registration of auto-proxy creators used internally
 * by the '{@code aop}' namespace tags.
 *
 * <p>Only a single auto-proxy creator should be registered and multiple configuration
 * elements may wish to register different concrete implementations. As such this class
 * delegates to {@link AutoProxyRegistrar} which provides a simple escalation protocol.
 * Callers may request a particular auto-proxy creator and know that creator,
 * <i>or a more capable variant thereof</i>, will be registered as a post-processor.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:53
 */
public abstract class AopNamespaceUtils {

  /**
   * The {@code proxy-target-class} attribute as found on AOP-related XML tags.
   */
  public static final String PROXY_TARGET_CLASS_ATTRIBUTE = "proxy-target-class";

  /**
   * The {@code expose-proxy} attribute as found on AOP-related XML tags.
   */
  private static final String EXPOSE_PROXY_ATTRIBUTE = "expose-proxy";

  public static void registerAutoProxyCreatorIfNecessary(
          ParserContext parserContext, Element sourceElement) {

    BeanDefinition beanDefinition = AutoProxyRegistrar.registerAutoProxyCreatorIfNecessary(
            parserContext.getRegistry(), parserContext.extractSource(sourceElement));
    useClassProxyingIfNecessary(parserContext.getRegistry(), sourceElement);
    registerComponentIfNecessary(beanDefinition, parserContext);
  }

  private static void useClassProxyingIfNecessary(BeanDefinitionRegistry registry, @Nullable Element sourceElement) {
    if (sourceElement != null) {
      boolean proxyTargetClass = Boolean.parseBoolean(sourceElement.getAttribute(PROXY_TARGET_CLASS_ATTRIBUTE));
      if (proxyTargetClass) {
        AutoProxyRegistrar.forceAutoProxyCreatorToUseClassProxying(registry);
      }
      boolean exposeProxy = Boolean.parseBoolean(sourceElement.getAttribute(EXPOSE_PROXY_ATTRIBUTE));
      if (exposeProxy) {
        AutoProxyRegistrar.forceAutoProxyCreatorToExposeProxy(registry);
      }
    }
  }

  private static void registerComponentIfNecessary(@Nullable BeanDefinition beanDefinition, ParserContext parserContext) {
    if (beanDefinition != null) {
      parserContext.registerComponent(
              new BeanComponentDefinition(beanDefinition, AutoProxyRegistrar.AUTO_PROXY_CREATOR_BEAN_NAME));
    }
  }

}
