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

import org.w3c.dom.Element;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.xml.AbstractBeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;
import infra.jmx.export.annotation.AnnotationMBeanExporter;
import infra.jmx.support.MBeanServerFactoryBean;
import infra.util.StringUtils;

/**
 * Parser for the &lt;context:mbean-server/&gt; element.
 *
 * <p>Registers an instance of
 * {@link AnnotationMBeanExporter}
 * within the context.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AnnotationMBeanExporter
 * @since 4.0
 */
class MBeanServerBeanDefinitionParser extends AbstractBeanDefinitionParser {

  private static final String MBEAN_SERVER_BEAN_NAME = "mbeanServer";

  private static final String AGENT_ID_ATTRIBUTE = "agent-id";

  @Override
  protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
    String id = element.getAttribute(ID_ATTRIBUTE);
    return (StringUtils.hasText(id) ? id : MBEAN_SERVER_BEAN_NAME);
  }

  @Override
  protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
    String agentId = element.getAttribute(AGENT_ID_ATTRIBUTE);
    if (StringUtils.hasText(agentId)) {
      RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
      bd.setEnableDependencyInjection(false);
      bd.getPropertyValues().add("agentId", agentId);
      return bd;
    }

    RootBeanDefinition bd = new RootBeanDefinition(MBeanServerFactoryBean.class);
    bd.getPropertyValues().add("locateExistingServerIfPossible", Boolean.TRUE);

    bd.setEnableDependencyInjection(false);
    // Mark as infrastructure bean and attach source location.
    bd.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    bd.setSource(parserContext.extractSource(element));
    return bd;
  }

}
