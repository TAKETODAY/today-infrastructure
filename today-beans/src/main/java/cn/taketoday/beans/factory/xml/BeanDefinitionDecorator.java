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

package cn.taketoday.beans.factory.xml;

import org.w3c.dom.Node;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;

/**
 * Interface used by the {@link DefaultBeanDefinitionDocumentReader}
 * to handle custom, nested (directly under a {@code <bean>}) tags.
 *
 * <p>Decoration may also occur based on custom attributes applied to the
 * {@code <bean>} tag. Implementations are free to turn the metadata in the
 * custom tag into as many
 * {@link BeanDefinition BeanDefinitions} as
 * required and to transform the
 * {@link BeanDefinition} of the enclosing
 * {@code <bean>} tag, potentially even returning a completely different
 * {@link BeanDefinition} to replace the
 * original.
 *
 * <p>{@link BeanDefinitionDecorator BeanDefinitionDecorators} should be aware that
 * they may be part of a chain. In particular, a {@link BeanDefinitionDecorator} should
 * be aware that a previous {@link BeanDefinitionDecorator} may have replaced the
 * original {@link BeanDefinition} with a
 * {@link cn.taketoday.aop.framework.ProxyFactoryBean} definition allowing for
 * custom {@link org.aopalliance.intercept.MethodInterceptor interceptors} to be added.
 *
 * <p>{@link BeanDefinitionDecorator BeanDefinitionDecorators} that wish to add an
 * interceptor to the enclosing bean should extend
 * {@link cn.taketoday.aop.config.AbstractInterceptorDrivenBeanDefinitionDecorator}
 * which handles the chaining ensuring that only one proxy is created and that it
 * contains all interceptors from the chain.
 *
 * <p>The parser locates a {@link BeanDefinitionDecorator} from the
 * {@link NamespaceHandler} for the namespace in which the custom tag resides.
 *
 * @author Rob Harrop
 * @see NamespaceHandler
 * @see BeanDefinitionParser
 * @since 4.0
 */
public interface BeanDefinitionDecorator {

  /**
   * Parse the specified {@link Node} (either an element or an attribute) and decorate
   * the supplied {@link BeanDefinition},
   * returning the decorated definition.
   * <p>Implementations may choose to return a completely new definition, which will
   * replace the original definition in the resulting
   * {@link cn.taketoday.beans.factory.BeanFactory}.
   * <p>The supplied {@link ParserContext} can be used to register any additional
   * beans needed to support the main definition.
   */
  BeanDefinitionHolder decorate(Node node, BeanDefinitionHolder definition, ParserContext parserContext);

}
