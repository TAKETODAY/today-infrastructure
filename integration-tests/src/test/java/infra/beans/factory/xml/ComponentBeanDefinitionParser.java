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

package infra.beans.factory.xml;

import org.w3c.dom.Element;

import java.util.List;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.ManagedList;
import infra.util.CollectionUtils;
import infra.util.xml.DomUtils;

public class ComponentBeanDefinitionParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		return parseComponentElement(element);
	}

	private static AbstractBeanDefinition parseComponentElement(Element element) {
		BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(ComponentFactoryBean.class);
		factory.addPropertyValue("parent", parseComponent(element));

		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "component");
		if (CollectionUtils.isNotEmpty(childElements)) {
			parseChildComponents(childElements, factory);
		}

		return factory.getBeanDefinition();
	}

	private static BeanDefinition parseComponent(Element element) {
		BeanDefinitionBuilder component = BeanDefinitionBuilder.rootBeanDefinition(Component.class);
		component.addPropertyValue("name", element.getAttribute("name"));
		return component.getBeanDefinition();
	}

	private static void parseChildComponents(List<Element> childElements, BeanDefinitionBuilder factory) {
		ManagedList<BeanDefinition> children = new ManagedList<>(childElements.size());
		for (Element element : childElements) {
			children.add(parseComponentElement(element));
		}
		factory.addPropertyValue("children", children);
	}

}
