/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
