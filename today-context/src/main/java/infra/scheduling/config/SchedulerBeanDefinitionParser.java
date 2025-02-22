/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.scheduling.config;

import org.w3c.dom.Element;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import infra.util.StringUtils;

/**
 * Parser for the 'scheduler' element of the 'task' namespace.
 *
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:05
 */
public class SchedulerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  @Override
  protected String getBeanClassName(Element element) {
    return "infra.scheduling.concurrent.ThreadPoolTaskScheduler";
  }

  @Override
  protected void doParse(Element element, BeanDefinitionBuilder builder) {
    String poolSize = element.getAttribute("pool-size");
    if (StringUtils.hasText(poolSize)) {
      builder.addPropertyValue("poolSize", poolSize);
    }

    builder.setEnableDependencyInjection(false);
    builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
  }

}
