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

package cn.taketoday.scheduling.concurrent;

import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ThreadPoolExecutorFactoryBean}.
 *
 * @author Juergen Hoeller
 */
class ThreadPoolExecutorFactoryBeanTests {

  @Test
  void defaultExecutor() throws Exception {
    ConfigurableApplicationContext context = new StandardApplicationContext(ExecutorConfig.class);
    ExecutorService executor = context.getBean(ExecutorService.class);

    FutureTask<String> task = new FutureTask<>(() -> "foo");
    executor.execute(task);
    assertThat(task.get()).isEqualTo("foo");
    context.close();
  }

  @Test
  void executorWithDefaultSettingsDoesNotPrestartAllCoreThreads() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean("taskExecutor", ThreadPoolExecutorFactoryBean.class, TestThreadPoolExecutorFactoryBean::new);
    context.refresh();
    ThreadPoolExecutor threadPoolExecutor = context.getBean(ThreadPoolExecutor.class);
    verify(threadPoolExecutor, never()).prestartAllCoreThreads();
  }

  @Test
  void executorWithPrestartAllCoreThreads() {
    GenericApplicationContext context = new GenericApplicationContext();
    context.registerBean("taskExecutor", ThreadPoolExecutorFactoryBean.class, () -> {
      TestThreadPoolExecutorFactoryBean factoryBean = new TestThreadPoolExecutorFactoryBean();
      factoryBean.setPrestartAllCoreThreads(true);
      return factoryBean;
    });
    context.refresh();
    ThreadPoolExecutor threadPoolExecutor = context.getBean(ThreadPoolExecutor.class);
    verify(threadPoolExecutor).prestartAllCoreThreads();
  }

  @Configuration
  static class ExecutorConfig {

    @Bean
    ThreadPoolExecutorFactoryBean executor() {
      return new ThreadPoolExecutorFactoryBean();
    }

  }

  @SuppressWarnings("serial")
  private static class TestThreadPoolExecutorFactoryBean extends ThreadPoolExecutorFactoryBean {

    @Override
    protected ThreadPoolExecutor createExecutor(
            int corePoolSize, int maxPoolSize, int keepAliveSeconds, BlockingQueue<Runnable> queue,
            ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler) {

      return mock(ThreadPoolExecutor.class);
    }
  }

}
