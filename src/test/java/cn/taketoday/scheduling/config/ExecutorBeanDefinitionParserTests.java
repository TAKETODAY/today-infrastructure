/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.util.CustomizableThreadCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class ExecutorBeanDefinitionParserTests {

  private ApplicationContext context;

  @BeforeEach
  public void setup() {
    this.context = new ClassPathXmlApplicationContext(
            "executorContext.xml", ExecutorBeanDefinitionParserTests.class);
  }

  @Test
  public void defaultExecutor() throws Exception {
    ThreadPoolTaskExecutor executor = this.context.getBean("default", ThreadPoolTaskExecutor.class);
    assertThat(getCorePoolSize(executor)).isEqualTo(1);
    assertThat(getMaxPoolSize(executor)).isEqualTo(Integer.MAX_VALUE);
    assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
    assertThat(getKeepAliveSeconds(executor)).isEqualTo(60);
    assertThat(getAllowCoreThreadTimeOut(executor)).isFalse();

    FutureTask<String> task = new FutureTask<>(() -> "foo");
    executor.execute(task);
    assertThat(task.get()).isEqualTo("foo");
  }

  @Test
  public void singleSize() {
    Object executor = this.context.getBean("singleSize");
    assertThat(getCorePoolSize(executor)).isEqualTo(42);
    assertThat(getMaxPoolSize(executor)).isEqualTo(42);
  }

  @Test
  public void invalidPoolSize() {
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            this.context.getBean("invalidPoolSize"));
  }

  @Test
  public void rangeWithBoundedQueue() {
    Object executor = this.context.getBean("rangeWithBoundedQueue");
    assertThat(getCorePoolSize(executor)).isEqualTo(7);
    assertThat(getMaxPoolSize(executor)).isEqualTo(42);
    assertThat(getQueueCapacity(executor)).isEqualTo(11);
  }

  @Test
  public void rangeWithUnboundedQueue() {
    Object executor = this.context.getBean("rangeWithUnboundedQueue");
    assertThat(getCorePoolSize(executor)).isEqualTo(9);
    assertThat(getMaxPoolSize(executor)).isEqualTo(9);
    assertThat(getKeepAliveSeconds(executor)).isEqualTo(37);
    assertThat(getAllowCoreThreadTimeOut(executor)).isTrue();
    assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void propertyPlaceholderWithSingleSize() {
    Object executor = this.context.getBean("propertyPlaceholderWithSingleSize");
    assertThat(getCorePoolSize(executor)).isEqualTo(123);
    assertThat(getMaxPoolSize(executor)).isEqualTo(123);
    assertThat(getKeepAliveSeconds(executor)).isEqualTo(60);
    assertThat(getAllowCoreThreadTimeOut(executor)).isFalse();
    assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void propertyPlaceholderWithRange() {
    Object executor = this.context.getBean("propertyPlaceholderWithRange");
    assertThat(getCorePoolSize(executor)).isEqualTo(5);
    assertThat(getMaxPoolSize(executor)).isEqualTo(25);
    assertThat(getAllowCoreThreadTimeOut(executor)).isFalse();
    assertThat(getQueueCapacity(executor)).isEqualTo(10);
  }

  @Test
  public void propertyPlaceholderWithRangeAndCoreThreadTimeout() {
    Object executor = this.context.getBean("propertyPlaceholderWithRangeAndCoreThreadTimeout");
    assertThat(getCorePoolSize(executor)).isEqualTo(99);
    assertThat(getMaxPoolSize(executor)).isEqualTo(99);
    assertThat(getAllowCoreThreadTimeOut(executor)).isTrue();
  }

  @Test
  public void propertyPlaceholderWithInvalidPoolSize() {
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            this.context.getBean("propertyPlaceholderWithInvalidPoolSize"));
  }

  @Test
  public void threadNamePrefix() {
    CustomizableThreadCreator executor = this.context.getBean("default", CustomizableThreadCreator.class);
    assertThat(executor.getThreadNamePrefix()).isEqualTo("default-");
  }

  @Test
  public void typeCheck() {
    assertThat(this.context.isTypeMatch("default", Executor.class)).isTrue();
    assertThat(this.context.isTypeMatch("default", TaskExecutor.class)).isTrue();
    assertThat(this.context.isTypeMatch("default", ThreadPoolTaskExecutor.class)).isTrue();
  }

  private int getCorePoolSize(Object executor) {
    return (Integer) new DirectFieldAccessor(executor).getPropertyValue("corePoolSize");
  }

  private int getMaxPoolSize(Object executor) {
    return (Integer) new DirectFieldAccessor(executor).getPropertyValue("maxPoolSize");
  }

  private int getQueueCapacity(Object executor) {
    return (Integer) new DirectFieldAccessor(executor).getPropertyValue("queueCapacity");
  }

  private int getKeepAliveSeconds(Object executor) {
    return (Integer) new DirectFieldAccessor(executor).getPropertyValue("keepAliveSeconds");
  }

  private boolean getAllowCoreThreadTimeOut(Object executor) {
    return (Boolean) new DirectFieldAccessor(executor).getPropertyValue("allowCoreThreadTimeOut");
  }

}
