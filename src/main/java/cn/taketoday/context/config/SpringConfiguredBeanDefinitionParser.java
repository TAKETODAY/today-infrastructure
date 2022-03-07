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

package cn.taketoday.context.config;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.xml.BeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * {@link BeanDefinitionParser} responsible for parsing the
 * {@code <context:spring-configured/>} tag.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
class SpringConfiguredBeanDefinitionParser implements BeanDefinitionParser {

	/**
	 * The bean name of the internally managed bean configurer aspect.
	 */
	public static final String BEAN_CONFIGURER_ASPECT_BEAN_NAME =
			"cn.taketoday.context.config.internalBeanConfigurerAspect";

	static final String BEAN_CONFIGURER_ASPECT_CLASS_NAME =
			"cn.taketoday.beans.factory.aspectj.AnnotationBeanConfigurerAspect";


	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		if (!parserContext.getRegistry().containsBeanDefinition(BEAN_CONFIGURER_ASPECT_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition();
			def.setBeanClassName(BEAN_CONFIGURER_ASPECT_CLASS_NAME);
			def.setFactoryMethodName("aspectOf");
			def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			def.setSource(parserContext.extractSource(element));
			parserContext.registerBeanComponent(new BeanComponentDefinition(def, BEAN_CONFIGURER_ASPECT_BEAN_NAME));
		}
		return null;
	}

}
