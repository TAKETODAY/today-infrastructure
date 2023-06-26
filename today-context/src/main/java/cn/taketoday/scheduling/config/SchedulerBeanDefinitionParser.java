/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.scheduling.config;

import org.w3c.dom.Element;

import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import cn.taketoday.util.StringUtils;

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
    return "cn.taketoday.scheduling.concurrent.ThreadPoolTaskScheduler";
  }

  @Override
  protected void doParse(Element element, BeanDefinitionBuilder builder) {
    String poolSize = element.getAttribute("pool-size");
    if (StringUtils.hasText(poolSize)) {
      builder.addPropertyValue("poolSize", poolSize);
    }

    builder.setEnableDependencyInjection(false);
  }

}
