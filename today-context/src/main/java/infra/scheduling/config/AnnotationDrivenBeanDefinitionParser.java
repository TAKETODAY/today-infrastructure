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

import infra.aop.config.AopNamespaceUtils;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.parsing.BeanComponentDefinition;
import infra.beans.factory.parsing.CompositeComponentDefinition;
import infra.beans.factory.support.BeanDefinitionBuilder;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.xml.BeanDefinitionParser;
import infra.beans.factory.xml.ParserContext;
import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * Parser for the 'annotation-driven' element of the 'task' namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:52
 */
public class AnnotationDrivenBeanDefinitionParser implements BeanDefinitionParser {

  private static final String ASYNC_EXECUTION_ASPECT_CLASS_NAME =
          "infra.scheduling.aspectj.AnnotationAsyncExecutionAspect";

  @Override
  @Nullable
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    Object source = parserContext.extractSource(element);

    // Register component for the surrounding <task:annotation-driven> element.
    CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
    parserContext.pushContainingComponent(compDefinition);

    // Nest the concrete post-processor bean in the surrounding component.
    BeanDefinitionRegistry registry = parserContext.getRegistry();

    String mode = element.getAttribute("mode");
    if ("aspectj".equals(mode)) {
      // mode="aspectj"
      registerAsyncExecutionAspect(element, parserContext);
    }
    else {
      // mode="proxy"
      if (registry.containsBeanDefinition(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)) {
        parserContext.getReaderContext().error(
                "Only one AsyncAnnotationBeanPostProcessor may exist within the context.", source);
      }
      else {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
                "infra.scheduling.annotation.AsyncAnnotationBeanPostProcessor");
        builder.getRawBeanDefinition().setSource(source);
        String executor = element.getAttribute("executor");
        if (StringUtils.hasText(executor)) {
          builder.addPropertyReference("executor", executor);
        }
        String exceptionHandler = element.getAttribute("exception-handler");
        if (StringUtils.hasText(exceptionHandler)) {
          builder.addPropertyReference("exceptionHandler", exceptionHandler);
        }
        if (Boolean.parseBoolean(element.getAttribute(AopNamespaceUtils.PROXY_TARGET_CLASS_ATTRIBUTE))) {
          builder.addPropertyValue("proxyTargetClass", true);
        }
        registerPostProcessor(parserContext, builder, TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
      }
    }

    if (registry.containsBeanDefinition(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
      parserContext.getReaderContext().error(
              "Only one ScheduledAnnotationBeanPostProcessor may exist within the context.", source);
    }
    else {
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
              "infra.scheduling.annotation.ScheduledAnnotationBeanPostProcessor");
      builder.getRawBeanDefinition().setSource(source);
      String scheduler = element.getAttribute("scheduler");
      if (StringUtils.hasText(scheduler)) {
        builder.addPropertyReference("scheduler", scheduler);
      }
      registerPostProcessor(parserContext, builder, TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
    }

    // Finally register the composite component.
    parserContext.popAndRegisterContainingComponent();

    return null;
  }

  private void registerAsyncExecutionAspect(Element element, ParserContext parserContext) {
    if (!parserContext.getRegistry().containsBeanDefinition(TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)) {
      BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ASYNC_EXECUTION_ASPECT_CLASS_NAME);
      builder.setFactoryMethod("aspectOf");
      String executor = element.getAttribute("executor");
      if (StringUtils.hasText(executor)) {
        builder.addPropertyReference("executor", executor);
      }
      String exceptionHandler = element.getAttribute("exception-handler");
      if (StringUtils.hasText(exceptionHandler)) {
        builder.addPropertyReference("exceptionHandler", exceptionHandler);
      }
      parserContext.registerBeanComponent(new BeanComponentDefinition(builder.getBeanDefinition(),
              TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME));
    }
  }

  private static void registerPostProcessor(
          ParserContext parserContext, BeanDefinitionBuilder builder, String beanName) {
    builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    parserContext.getRegistry().registerBeanDefinition(beanName, builder.getBeanDefinition());

    BeanDefinitionHolder holder = new BeanDefinitionHolder(builder.getBeanDefinition(), beanName);
    parserContext.registerComponent(new BeanComponentDefinition(holder));
  }

}

