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

package cn.taketoday.scheduling.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cn.taketoday.beans.factory.parsing.BeanComponentDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.ManagedList;
import cn.taketoday.beans.factory.support.RuntimeBeanReference;
import cn.taketoday.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import cn.taketoday.beans.factory.xml.ParserContext;
import cn.taketoday.scheduling.support.ScheduledMethodRunnable;
import cn.taketoday.util.StringUtils;

/**
 * Parser for the 'scheduled-tasks' element of the scheduling namespace.
 *
 * @author Mark Fisher
 * @author Chris Beams
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 22:02
 */
public class ScheduledTasksBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

  private static final String ELEMENT_SCHEDULED = "scheduled";

  private static final long ZERO_INITIAL_DELAY = 0;

  @Override
  protected boolean shouldGenerateId() {
    return true;
  }

  @Override
  protected String getBeanClassName(Element element) {
    return "cn.taketoday.scheduling.config.ContextLifecycleScheduledTaskRegistrar";
  }

  @Override
  protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
    builder.setLazyInit(false); // lazy scheduled tasks are a contradiction in terms -> force to false
    ManagedList<RuntimeBeanReference> cronTaskList = new ManagedList<>();
    ManagedList<RuntimeBeanReference> fixedDelayTaskList = new ManagedList<>();
    ManagedList<RuntimeBeanReference> fixedRateTaskList = new ManagedList<>();
    ManagedList<RuntimeBeanReference> triggerTaskList = new ManagedList<>();
    NodeList childNodes = element.getChildNodes();
    int length = childNodes.getLength();
    for (int i = 0; i < length; i++) {
      Node child = childNodes.item(i);
      if (!isScheduledElement(child, parserContext)) {
        continue;
      }
      Element taskElement = (Element) child;
      String ref = taskElement.getAttribute("ref");
      String method = taskElement.getAttribute("method");

      // Check that 'ref' and 'method' are specified
      if (!StringUtils.hasText(ref) || !StringUtils.hasText(method)) {
        parserContext.getReaderContext().error("Both 'ref' and 'method' are required", taskElement);
        // Continue with the possible next task element
        continue;
      }

      String cronAttribute = taskElement.getAttribute("cron");
      String triggerAttribute = taskElement.getAttribute("trigger");
      String fixedRateAttribute = taskElement.getAttribute("fixed-rate");
      String fixedDelayAttribute = taskElement.getAttribute("fixed-delay");
      String initialDelayAttribute = taskElement.getAttribute("initial-delay");

      boolean hasCronAttribute = StringUtils.hasText(cronAttribute);
      boolean hasTriggerAttribute = StringUtils.hasText(triggerAttribute);
      boolean hasFixedRateAttribute = StringUtils.hasText(fixedRateAttribute);
      boolean hasFixedDelayAttribute = StringUtils.hasText(fixedDelayAttribute);
      boolean hasInitialDelayAttribute = StringUtils.hasText(initialDelayAttribute);

      if (!(hasCronAttribute || hasFixedDelayAttribute || hasFixedRateAttribute || hasTriggerAttribute)) {
        parserContext.getReaderContext().error(
                "one of the 'cron', 'fixed-delay', 'fixed-rate', or 'trigger' attributes is required", taskElement);
        continue; // with the possible next task element
      }

      if (hasInitialDelayAttribute && (hasCronAttribute || hasTriggerAttribute)) {
        parserContext.getReaderContext().error(
                "the 'initial-delay' attribute may not be used with cron and trigger tasks", taskElement);
        continue; // with the possible next task element
      }

      String runnableName =
              runnableReference(ref, method, taskElement, parserContext).getBeanName();

      if (hasFixedDelayAttribute) {
        fixedDelayTaskList.add(intervalTaskReference(runnableName,
                initialDelayAttribute, fixedDelayAttribute, taskElement, parserContext));
      }
      if (hasFixedRateAttribute) {
        fixedRateTaskList.add(intervalTaskReference(runnableName,
                initialDelayAttribute, fixedRateAttribute, taskElement, parserContext));
      }
      if (hasCronAttribute) {
        cronTaskList.add(cronTaskReference(runnableName, cronAttribute,
                taskElement, parserContext));
      }
      if (hasTriggerAttribute) {
        String triggerName = new RuntimeBeanReference(triggerAttribute).getBeanName();
        triggerTaskList.add(triggerTaskReference(runnableName, triggerName,
                taskElement, parserContext));
      }
    }
    String schedulerRef = element.getAttribute("scheduler");
    if (StringUtils.hasText(schedulerRef)) {
      builder.addPropertyReference("taskScheduler", schedulerRef);
    }
    builder.addPropertyValue("cronTasksList", cronTaskList);
    builder.addPropertyValue("triggerTasksList", triggerTaskList);
    builder.addPropertyValue("fixedRateTasksList", fixedRateTaskList);
    builder.addPropertyValue("fixedDelayTasksList", fixedDelayTaskList);
  }

  private boolean isScheduledElement(Node node, ParserContext parserContext) {
    return node.getNodeType() == Node.ELEMENT_NODE
            && ELEMENT_SCHEDULED.equals(parserContext.getDelegate().getLocalName(node));
  }

  private RuntimeBeanReference runnableReference(String ref, String method, Element taskElement, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ScheduledMethodRunnable.class);
    builder.addConstructorArgReference(ref);
    builder.addConstructorArgValue(method);
    return beanReference(taskElement, parserContext, builder);
  }

  private RuntimeBeanReference intervalTaskReference(String runnableBeanName,
          String initialDelay, String interval, Element taskElement, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
            "cn.taketoday.scheduling.config.IntervalTask");
    builder.addConstructorArgReference(runnableBeanName);
    builder.addConstructorArgValue(interval);
    builder.addConstructorArgValue(StringUtils.isNotEmpty(initialDelay) ? initialDelay : ZERO_INITIAL_DELAY);
    return beanReference(taskElement, parserContext, builder);
  }

  private RuntimeBeanReference cronTaskReference(String runnableBeanName,
          String cronExpression, Element taskElement, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
            "cn.taketoday.scheduling.config.CronTask");
    builder.addConstructorArgReference(runnableBeanName);
    builder.addConstructorArgValue(cronExpression);
    return beanReference(taskElement, parserContext, builder);
  }

  private RuntimeBeanReference triggerTaskReference(String runnableBeanName,
          String triggerBeanName, Element taskElement, ParserContext parserContext) {
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
            "cn.taketoday.scheduling.config.TriggerTask");
    builder.addConstructorArgReference(runnableBeanName);
    builder.addConstructorArgReference(triggerBeanName);
    return beanReference(taskElement, parserContext, builder);
  }

  private RuntimeBeanReference beanReference(Element taskElement,
          ParserContext parserContext, BeanDefinitionBuilder builder) {
    // Extract the source of the current task
    builder.getRawBeanDefinition().setSource(parserContext.extractSource(taskElement));
    String generatedName = parserContext.getReaderContext().generateBeanName(builder.getRawBeanDefinition());
    parserContext.registerBeanComponent(new BeanComponentDefinition(builder.getBeanDefinition(), generatedName));
    return new RuntimeBeanReference(generatedName);
  }

}
