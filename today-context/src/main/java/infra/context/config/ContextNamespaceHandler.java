/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.config;

import infra.beans.factory.xml.NamespaceHandler;
import infra.beans.factory.xml.NamespaceHandlerSupport;
import infra.context.annotation.AnnotationConfigBeanDefinitionParser;
import infra.context.annotation.ComponentScanBeanDefinitionParser;

/**
 * {@link NamespaceHandler}
 * for the '{@code context}' namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 4.0
 */
public class ContextNamespaceHandler extends NamespaceHandlerSupport {

  @Override
  public void init() {
    registerBeanDefinitionParser("property-placeholder", new PropertyPlaceholderBeanDefinitionParser());
    registerBeanDefinitionParser("property-override", new PropertyOverrideBeanDefinitionParser());
    registerBeanDefinitionParser("annotation-config", new AnnotationConfigBeanDefinitionParser());
    registerBeanDefinitionParser("component-scan", new ComponentScanBeanDefinitionParser());
    registerBeanDefinitionParser("load-time-weaver", new LoadTimeWeaverBeanDefinitionParser());
    registerBeanDefinitionParser("mbean-export", new MBeanExportBeanDefinitionParser());
    registerBeanDefinitionParser("mbean-server", new MBeanServerBeanDefinitionParser());
  }

}
