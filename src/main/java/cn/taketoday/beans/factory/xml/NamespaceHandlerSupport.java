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

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.lang.Nullable;

/**
 * Support class for implementing custom {@link NamespaceHandler NamespaceHandlers}.
 * Parsing and decorating of individual {@link Node Nodes} is done via {@link BeanDefinitionParser}
 * and {@link BeanDefinitionDecorator} strategy interfaces, respectively.
 *
 * <p>Provides the {@link #registerBeanDefinitionParser} and {@link #registerBeanDefinitionDecorator}
 * methods for registering a {@link BeanDefinitionParser} or {@link BeanDefinitionDecorator}
 * to handle a specific element.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #registerBeanDefinitionParser(String, BeanDefinitionParser)
 * @see #registerBeanDefinitionDecorator(String, BeanDefinitionDecorator)
 * @since 4.0
 */
public abstract class NamespaceHandlerSupport implements NamespaceHandler {

  /**
   * Stores the {@link BeanDefinitionParser} implementations keyed by the
   * local name of the {@link Element Elements} they handle.
   */
  private final Map<String, BeanDefinitionParser> parsers = new HashMap<>();

  /**
   * Stores the {@link BeanDefinitionDecorator} implementations keyed by the
   * local name of the {@link Element Elements} they handle.
   */
  private final Map<String, BeanDefinitionDecorator> decorators = new HashMap<>();

  /**
   * Stores the {@link BeanDefinitionDecorator} implementations keyed by the local
   * name of the {@link Attr Attrs} they handle.
   */
  private final Map<String, BeanDefinitionDecorator> attributeDecorators = new HashMap<>();

  /**
   * Parses the supplied {@link Element} by delegating to the {@link BeanDefinitionParser} that is
   * registered for that {@link Element}.
   */
  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    BeanDefinitionParser parser = findParserForElement(element, parserContext);
    return (parser != null ? parser.parse(element, parserContext) : null);
  }

  /**
   * Locates the {@link BeanDefinitionParser} from the register implementations using
   * the local name of the supplied {@link Element}.
   */
  @Nullable
  private BeanDefinitionParser findParserForElement(Element element, ParserContext parserContext) {
    String localName = parserContext.getDelegate().getLocalName(element);
    BeanDefinitionParser parser = this.parsers.get(localName);
    if (parser == null) {
      parserContext.getReaderContext().fatal(
              "Cannot locate BeanDefinitionParser for element [" + localName + "]", element);
    }
    return parser;
  }

  /**
   * Decorates the supplied {@link Node} by delegating to the {@link BeanDefinitionDecorator} that
   * is registered to handle that {@link Node}.
   */
  @Override
  @Nullable
  public BeanDefinition decorate(
          Node node, BeanDefinition definition, ParserContext parserContext) {

    BeanDefinitionDecorator decorator = findDecoratorForNode(node, parserContext);
    return (decorator != null ? decorator.decorate(node, definition, parserContext) : null);
  }

  /**
   * Locates the {@link BeanDefinitionParser} from the register implementations using
   * the local name of the supplied {@link Node}. Supports both {@link Element Elements}
   * and {@link Attr Attrs}.
   */
  @Nullable
  private BeanDefinitionDecorator findDecoratorForNode(Node node, ParserContext parserContext) {
    BeanDefinitionDecorator decorator = null;
    String localName = parserContext.getDelegate().getLocalName(node);
    if (node instanceof Element) {
      decorator = this.decorators.get(localName);
    }
    else if (node instanceof Attr) {
      decorator = this.attributeDecorators.get(localName);
    }
    else {
      parserContext.getReaderContext().fatal(
              "Cannot decorate based on Nodes of type [" + node.getClass().getName() + "]", node);
    }
    if (decorator == null) {
      parserContext.getReaderContext().fatal("Cannot locate BeanDefinitionDecorator for " +
              (node instanceof Element ? "element" : "attribute") + " [" + localName + "]", node);
    }
    return decorator;
  }

  /**
   * Subclasses can call this to register the supplied {@link BeanDefinitionParser} to
   * handle the specified element. The element name is the local (non-namespace qualified)
   * name.
   */
  protected final void registerBeanDefinitionParser(String elementName, BeanDefinitionParser parser) {
    this.parsers.put(elementName, parser);
  }

  /**
   * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to
   * handle the specified element. The element name is the local (non-namespace qualified)
   * name.
   */
  protected final void registerBeanDefinitionDecorator(String elementName, BeanDefinitionDecorator dec) {
    this.decorators.put(elementName, dec);
  }

  /**
   * Subclasses can call this to register the supplied {@link BeanDefinitionDecorator} to
   * handle the specified attribute. The attribute name is the local (non-namespace qualified)
   * name.
   */
  protected final void registerBeanDefinitionDecoratorForAttribute(String attrName, BeanDefinitionDecorator dec) {
    this.attributeDecorators.put(attrName, dec);
  }

}
