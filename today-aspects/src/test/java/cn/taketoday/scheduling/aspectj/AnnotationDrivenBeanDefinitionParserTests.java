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

package cn.taketoday.scheduling.aspectj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.scheduling.config.TaskManagementConfigUtils;

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
