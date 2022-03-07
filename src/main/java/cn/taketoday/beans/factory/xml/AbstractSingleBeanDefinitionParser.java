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

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.lang.Nullable;

/**
 * Base class for those {@link BeanDefinitionParser} implementations that
 * need to parse and define just a <i>single</i> {@code BeanDefinition}.
 *
 * <p>Extend this parser class when you want to create a single bean definition
 * from an arbitrarily complex XML element. You may wish to consider extending
 * the {@link AbstractSimpleBeanDefinitionParser} when you want to create a
 * single bean definition from a relatively simple custom XML element.
 *
 * <p>The resulting {@code BeanDefinition} will be automatically registered
 * with the {@link cn.taketoday.beans.factory.support.BeanDefinitionRegistry}.
 * Your job simply is to {@link #doParse parse} the custom XML {@link Element}
 * into a single {@code BeanDefinition}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @see #getBeanClass
 * @see #getBeanClassName
 * @see #doParse
 * @since 4.0
 */
public abstract class AbstractSingleBeanDefinitionParser extends AbstractBeanDefinitionParser {

  /**
   * Creates a {@link BeanDefinitionBuilder} instance for the
   * {@link #getBeanClass bean Class} and passes it to the
   * {@link #doParse} strategy method.
   *
   * @param element the element that is to be parsed into a single BeanDefinition
   * @param parserContext the object encapsulating the current state of the parsing process
   * @return the BeanDefinition resulting from the parsing of the supplied {@link Element}
   * @throws IllegalStateException if the bean {@link Class} returned from
   * {@link #getBeanClass(Element)} is {@code null}
   * @see #doParse
   */
  @Override
  protected final BeanDefinition parseInternal(Element element, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition();
    String parentName = getParentName(element);
    if (parentName != null) {
      builder.getRawBeanDefinition().setParentName(parentName);
    }
    Class<?> beanClass = getBeanClass(element);
    if (beanClass != null) {
      builder.getRawBeanDefinition().setBeanClass(beanClass);
    }
    else {
      String beanClassName = getBeanClassName(element);
      if (beanClassName != null) {
        builder.getRawBeanDefinition().setBeanClassName(beanClassName);
      }
    }
    builder.getRawBeanDefinition().setSource(parserContext.extractSource(element));
    BeanDefinition containingBd = parserContext.getContainingBeanDefinition();
    if (containingBd != null) {
      // Inner bean definition must receive same scope as containing bean.
      builder.setScope(containingBd.getScope());
    }
    if (parserContext.isDefaultLazyInit()) {
      // Default-lazy-init applies to custom bean definitions as well.
      builder.setLazyInit(true);
    }
    doParse(element, parserContext, builder);
    return builder.getBeanDefinition();
  }

  /**
   * Determine the name for the parent of the currently parsed bean,
   * in case of the current bean being defined as a child bean.
   * <p>The default implementation returns {@code null},
   * indicating a root bean definition.
   *
   * @param element the {@code Element} that is being parsed
   * @return the name of the parent bean for the currently parsed bean,
   * or {@code null} if none
   */
  @Nullable
  protected String getParentName(Element element) {
    return null;
  }

  /**
   * Determine the bean class corresponding to the supplied {@link Element}.
   * <p>Note that, for application classes, it is generally preferable to
   * override {@link #getBeanClassName} instead, in order to avoid a direct
   * dependence on the bean implementation class. The BeanDefinitionParser
   * and its NamespaceHandler can be used within an IDE plugin then, even
   * if the application classes are not available on the plugin's classpath.
   *
   * @param element the {@code Element} that is being parsed
   * @return the {@link Class} of the bean that is being defined via parsing
   * the supplied {@code Element}, or {@code null} if none
   * @see #getBeanClassName
   */
  @Nullable
  protected Class<?> getBeanClass(Element element) {
    return null;
  }

  /**
   * Determine the bean class name corresponding to the supplied {@link Element}.
   *
   * @param element the {@code Element} that is being parsed
   * @return the class name of the bean that is being defined via parsing
   * the supplied {@code Element}, or {@code null} if none
   * @see #getBeanClass
   */
  @Nullable
  protected String getBeanClassName(Element element) {
    return null;
  }

  /**
   * Parse the supplied {@link Element} and populate the supplied
   * {@link BeanDefinitionBuilder} as required.
   * <p>The default implementation delegates to the {@code doParse}
   * version without ParserContext argument.
   *
   * @param element the XML element being parsed
   * @param parserContext the object encapsulating the current state of the parsing process
   * @param builder used to define the {@code BeanDefinition}
   * @see #doParse(Element, BeanDefinitionBuilder)
   */
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    doParse(element, builder);
  }

  /**
   * Parse the supplied {@link Element} and populate the supplied
   * {@link BeanDefinitionBuilder} as required.
   * <p>The default implementation does nothing.
   *
   * @param element the XML element being parsed
   * @param builder used to define the {@code BeanDefinition}
   */
  protected void doParse(Element element, BeanDefinitionBuilder builder) {

  }

}
