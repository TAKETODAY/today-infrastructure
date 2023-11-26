/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.ejb.config;

import org.w3c.dom.Element;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.jndi.JndiObjectFactoryBean;

/**
 * {@link cn.taketoday.beans.factory.xml.BeanDefinitionParser}
 * implementation for parsing '{@code remote-slsb}' tags and
 * creating plain {@link JndiObjectFactoryBean} definitions.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class RemoteStatelessSessionBeanDefinitionParser extends AbstractJndiLocatingBeanDefinitionParser {

  @Override
  protected Class<?> getBeanClass(Element element) {
    return JndiObjectFactoryBean.class;
  }

  @Override
  protected boolean isEligibleAttribute(String attributeName) {
    return (super.isEligibleAttribute(attributeName) &&
            BeanUtils.getPropertyDescriptor(JndiObjectFactoryBean.class, extractPropertyName(attributeName)) != null);
  }

}
