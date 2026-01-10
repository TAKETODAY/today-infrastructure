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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import infra.beans.DirectFieldAccessor;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.ClassPathXmlApplicationContext;

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
