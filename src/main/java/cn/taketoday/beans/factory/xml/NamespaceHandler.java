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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.lang.Nullable;

/**
 * Base interface used by the {@link DefaultBeanDefinitionDocumentReader}
 * for handling custom namespaces in a Framework XML configuration file.
 *
 * <p>Implementations are expected to return implementations of the
 * {@link BeanDefinitionParser} interface for custom top-level tags and
 * implementations of the {@link BeanDefinitionDecorator} interface for
 * custom nested tags.
 *
 * <p>The parser will call {@link #parse} when it encounters a custom tag
 * directly under the {@code <beans>} tags and {@link #decorate} when
 * it encounters a custom tag directly under a {@code <bean>} tag.
 *
 * <p>Developers writing their own custom element extensions typically will
 * not implement this interface directly, but rather make use of the provided
 * {@link NamespaceHandlerSupport} class.
 *
 * @author Rob Harrop
 * @author Erik Wiersma
 * @see DefaultBeanDefinitionDocumentReader
 * @see NamespaceHandlerResolver
 * @since 4.0
 */
public interface NamespaceHandler {

  /**
   * Invoked by the {@link DefaultBeanDefinitionDocumentReader} after
   * construction but before any custom elements are parsed.
   *
   * @see NamespaceHandlerSupport#registerBeanDefinitionParser(String, BeanDefinitionParser)
   */
  default void init() { }

  /**
   * Parse the specified {@link Element} and register any resulting
   * {@link BeanDefinition BeanDefinitions} with the
   * {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}
   * that is embedded in the supplied {@link ParserContext}.
   * <p>Implementations should return the primary {@code BeanDefinition}
   * that results from the parse phase if they wish to be used nested
   * inside (for example) a {@code <property>} tag.
   * <p>Implementations may return {@code null} if they will
   * <strong>not</strong> be used in a nested scenario.
   *
   * @param element the element that is to be parsed into one or more {@code BeanDefinitions}
   * @param parserContext the object encapsulating the current state of the parsing process
   * @return the primary {@code BeanDefinition} (can be {@code null} as explained above)
   */
  @Nullable
  BeanDefinition parse(Element element, ParserContext parserContext);

  /**
   * Parse the specified {@link Node} and decorate the supplied
   * {@link BeanDefinition}, returning the decorated definition.
   * <p>The {@link Node} may be either an {@link org.w3c.dom.Attr} or an
   * {@link Element}, depending on whether a custom attribute or element
   * is being parsed.
   * <p>Implementations may choose to return a completely new definition,
   * which will replace the original definition in the resulting
   * {@link cn.taketoday.beans.factory.BeanFactory}.
   * <p>The supplied {@link ParserContext} can be used to register any
   * additional beans needed to support the main definition.
   *
   * @param source the source element or attribute that is to be parsed
   * @param definition the current bean definition
   * @param parserContext the object encapsulating the current state of the parsing process
   * @return the decorated definition (to be registered in the BeanFactory),
   * or simply the original bean definition if no decoration is required.
   * A {@code null} value is strictly speaking invalid, but will be leniently
   * treated like the case where the original bean definition gets returned.
   */
  @Nullable
  BeanDefinition decorate(Node source, BeanDefinition definition, ParserContext parserContext);

}
