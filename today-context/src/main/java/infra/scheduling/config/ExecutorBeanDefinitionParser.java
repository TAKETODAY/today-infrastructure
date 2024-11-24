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

package infra.scheduling.config;

import org.w3c.dom.Element;

import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;
import infra.util.StringUtils;

/**
 * Parser for the 'executor' element of the 'task' namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:56
 */
public class ExecutorBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  @Override
  protected String getBeanClassName(Element element) {
    return "infra.scheduling.config.TaskExecutorFactoryBean";
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    String keepAliveSeconds = element.getAttribute("keep-alive");
    if (StringUtils.hasText(keepAliveSeconds)) {
      builder.addPropertyValue("keepAliveSeconds", keepAliveSeconds);
    }
    String queueCapacity = element.getAttribute("queue-capacity");
    if (StringUtils.hasText(queueCapacity)) {
      builder.addPropertyValue("queueCapacity", queueCapacity);
    }
    configureRejectionPolicy(element, builder);
    String poolSize = element.getAttribute("pool-size");
    if (StringUtils.hasText(poolSize)) {
      builder.addPropertyValue("poolSize", poolSize);
    }
  }

  private void configureRejectionPolicy(Element element, BeanDefinitionBuilder builder) {
    String rejectionPolicy = element.getAttribute("rejection-policy");
    if (StringUtils.isBlank(rejectionPolicy)) {
      return;
    }
    String prefix = "java.util.concurrent.ThreadPoolExecutor.";
    String policyClassName = switch (rejectionPolicy) {
      case "ABORT" -> prefix + "AbortPolicy";
      case "DISCARD" -> prefix + "DiscardPolicy";
      case "CALLER_RUNS" -> prefix + "CallerRunsPolicy";
      case "DISCARD_OLDEST" -> prefix + "DiscardOldestPolicy";
      default -> rejectionPolicy;
    };
    builder.addPropertyValue("rejectedExecutionHandler", new RootBeanDefinition(policyClassName));
  }

}
