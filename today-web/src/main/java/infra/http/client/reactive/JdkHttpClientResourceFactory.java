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

package infra.http.client.reactive;

import java.net.http.HttpClient;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializingBean;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Factory to manage JDK HttpClient resources such as a shared {@link Executor}
 * within the lifecycle of a Infra {@code ApplicationContext}.
 *
 * <p>This factory implements {@link InitializingBean} and {@link DisposableBean}
 * and is expected typically to be declared as a Infra-managed bean.
 *
 * @author Rossen Stoyanchev
 * @see JdkClientHttpConnector#JdkClientHttpConnector(HttpClient.Builder, JdkHttpClientResourceFactory)
 * @since 4.0
 */
public class JdkHttpClientResourceFactory implements InitializingBean, DisposableBean {

  @Nullable
  private Executor executor;

  private String threadPrefix = "jdk-http";

  /**
   * Configure the {@link Executor} to use for {@link HttpClient} exchanges.
   * The given executor is started and stopped via {@link InitializingBean}
   * and {@link DisposableBean}.
   * <p>By default, this is set to {@link Executors#newCachedThreadPool(ThreadFactory)},
   * which mirrors {@link HttpClient.Builder#executor(Executor)}.
   *
   * @param executor the executor to use
   */
  public void setExecutor(@Nullable Executor executor) {
    this.executor = executor;
  }

  /**
   * Return the configured {@link Executor}.
   */
  @Nullable
  public Executor getExecutor() {
    return this.executor;
  }

  /**
   * Configure the thread prefix to initialize the executor with. This
   * is used only when a {@link Executor} instance isn't
   * {@link #setExecutor(Executor) provided}.
   * <p>By default set to "jdk-http".
   *
   * @param threadPrefix the thread prefix to use
   */
  public void setThreadPrefix(String threadPrefix) {
    Assert.notNull(threadPrefix, "Thread prefix is required");
    this.threadPrefix = threadPrefix;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.executor == null) {
      String name = this.threadPrefix + "@" + Integer.toHexString(hashCode());
      this.executor = Executors.newCachedThreadPool(new CustomizableThreadFactory(name));
    }
  }

  @Override
  public void destroy() throws Exception {
    if (this.executor instanceof ExecutorService executorService) {
      executorService.shutdown();
    }
  }

}
