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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/7 21:58
 */
class AnnotationDrivenBeanDefinitionParserTests {

  private ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
          "annotationDrivenContext.xml", AnnotationDrivenBeanDefinitionParserTests.class);

  @AfterEach
  public void closeApplicationContext() {
    context.close();
  }

  @Test
  public void asyncPostProcessorRegistered() {
    assertThat(context.containsBean(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
  }

  @Test
  public void scheduledPostProcessorRegistered() {
    assertThat(context.containsBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)).isTrue();
  }

  @Test
  public void asyncPostProcessorExecutorReference() {
    Object executor = context.getBean("testExecutor");
    Object postProcessor = context.getBean(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
    assertThat(((Supplier<?>) new DirectFieldAccessor(postProcessor).getPropertyValue("executor")).get()).isSameAs(executor);
  }

  @Test
  public void scheduledPostProcessorSchedulerReference() {
    Object scheduler = context.getBean("testScheduler");
    Object postProcessor = context.getBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
    assertThat(new DirectFieldAccessor(postProcessor).getPropertyValue("scheduler")).isSameAs(scheduler);
  }

  @Test
  public void asyncPostProcessorExceptionHandlerReference() {
    Object exceptionHandler = context.getBean("testExceptionHandler");
    Object postProcessor = context.getBean(TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME);
    assertThat(((Supplier<?>) new DirectFieldAccessor(postProcessor).getPropertyValue("exceptionHandler")).get()).isSameAs(exceptionHandler);
  }

}
