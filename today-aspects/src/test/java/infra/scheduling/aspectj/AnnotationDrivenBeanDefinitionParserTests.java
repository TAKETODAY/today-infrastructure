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

package infra.scheduling.aspectj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import infra.beans.DirectFieldAccessor;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.scheduling.config.TaskManagementConfigUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class AnnotationDrivenBeanDefinitionParserTests {

  private ConfigurableApplicationContext context;

  @BeforeEach
  public void setup() {
    this.context = new ClassPathXmlApplicationContext(
            "annotationDrivenContext.xml", AnnotationDrivenBeanDefinitionParserTests.class);
  }

  @AfterEach
  public void after() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  public void asyncAspectRegistered() {
    assertThat(context.containsBean(TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)).isTrue();
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void asyncPostProcessorExecutorReference() {
    Object executor = context.getBean("testExecutor");
    Object aspect = context.getBean(TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME);
    assertThat(((Supplier) new DirectFieldAccessor(aspect).getPropertyValue("defaultExecutor")).get()).isSameAs(executor);
  }

  @Test
  @SuppressWarnings("rawtypes")
  public void asyncPostProcessorExceptionHandlerReference() {
    Object exceptionHandler = context.getBean("testExceptionHandler");
    Object aspect = context.getBean(TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME);
    assertThat(((Supplier) new DirectFieldAccessor(aspect).getPropertyValue("exceptionHandler")).get()).isSameAs(exceptionHandler);
  }

}
