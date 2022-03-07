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

import java.util.ArrayDeque;
import java.util.Deque;

import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.parsing.ComponentDefinition;
import cn.taketoday.beans.factory.parsing.CompositeComponentDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionReaderUtils;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.lang.Nullable;

/**
 * Context that gets passed along a bean definition parsing process,
 * encapsulating all relevant configuration as well as state.
 * Nested inside an {@link XmlReaderContext}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see XmlReaderContext
 * @see BeanDefinitionParserDelegate
 * @since 4.0
 */
public final class ParserContext {

  private final XmlReaderContext readerContext;

  private final BeanDefinitionParserDelegate delegate;

  @Nullable
  private BeanDefinition containingBeanDefinition;

  private final Deque<CompositeComponentDefinition> containingComponents = new ArrayDeque<>();

  public ParserContext(XmlReaderContext readerContext, BeanDefinitionParserDelegate delegate) {
    this.readerContext = readerContext;
    this.delegate = delegate;
  }

  public ParserContext(XmlReaderContext readerContext, BeanDefinitionParserDelegate delegate,
          @Nullable BeanDefinition containingBeanDefinition) {

    this.readerContext = readerContext;
    this.delegate = delegate;
    this.containingBeanDefinition = containingBeanDefinition;
  }

  public XmlReaderContext getReaderContext() {
    return this.readerContext;
  }

  public BeanDefinitionRegistry getRegistry() {
    return this.readerContext.getRegistry();
  }

  public BeanDefinitionParserDelegate getDelegate() {
    return this.delegate;
  }

  @Nullable
  public BeanDefinition getContainingBeanDefinition() {
    return this.containingBeanDefinition;
  }

  public boolean isNested() {
    return (this.containingBeanDefinition != null);
  }

  public boolean isDefaultLazyInit() {
    return BeanDefinitionParserDelegate.TRUE_VALUE.equals(this.delegate.getDefaults().getLazyInit());
  }

  @Nullable
  public Object extractSource(Object sourceCandidate) {
    return this.readerContext.extractSource(sourceCandidate);
  }

  @Nullable
  public CompositeComponentDefinition getContainingComponent() {
    return this.containingComponents.peek();
  }

  public void pushContainingComponent(CompositeComponentDefinition containingComponent) {
    this.containingComponents.push(containingComponent);
  }

  public CompositeComponentDefinition popContainingComponent() {
    return this.containingComponents.pop();
  }

  public void popAndRegisterContainingComponent() {
    registerComponent(popContainingComponent());
  }

  public void registerComponent(ComponentDefinition component) {
    CompositeComponentDefinition containingComponent = getContainingComponent();
    if (containingComponent != null) {
      containingComponent.addNestedComponent(component);
    }
    else {
      this.readerContext.fireComponentRegistered(component);
    }
  }

  public void registerBeanComponent(BeanComponentDefinition component) {
    BeanDefinitionReaderUtils.registerBeanDefinition(component, getRegistry());
    registerComponent(component);
  }

}
