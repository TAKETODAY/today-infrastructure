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

import java.util.concurrent.RejectedExecutionHandler;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.util.StringUtils;

/**
 * {@link FactoryBean} for creating {@link ThreadPoolTaskExecutor} instances,
 * primarily used behind the XML task namespace.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 4.0
 */
public class TaskExecutorFactoryBean
        implements FactoryBean<TaskExecutor>, BeanNameAware, InitializingBean, DisposableBean {

  @Nullable
  private String poolSize;

  @Nullable
  private Integer queueCapacity;

  @Nullable
  private RejectedExecutionHandler rejectedExecutionHandler;

  @Nullable
  private Integer keepAliveSeconds;

  @Nullable
  private String beanName;

  @Nullable
  private ThreadPoolTaskExecutor target;

  public void setPoolSize(String poolSize) {
    this.poolSize = poolSize;
  }

  public void setQueueCapacity(int queueCapacity) {
    this.queueCapacity = queueCapacity;
  }

  public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
    this.rejectedExecutionHandler = rejectedExecutionHandler;
  }

  public void setKeepAliveSeconds(int keepAliveSeconds) {
    this.keepAliveSeconds = keepAliveSeconds;
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  @Override
  public void afterPropertiesSet() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    determinePoolSizeRange(executor);
    if (this.queueCapacity != null) {
      executor.setQueueCapacity(this.queueCapacity);
    }
    if (this.keepAliveSeconds != null) {
      executor.setKeepAliveSeconds(this.keepAliveSeconds);
    }
    if (this.rejectedExecutionHandler != null) {
      executor.setRejectedExecutionHandler(this.rejectedExecutionHandler);
    }
    if (this.beanName != null) {
      executor.setThreadNamePrefix(this.beanName + "-");
    }
    executor.afterPropertiesSet();
    this.target = executor;
  }

  private void determinePoolSizeRange(ThreadPoolTaskExecutor executor) {
    if (StringUtils.hasText(this.poolSize)) {
      try {
        int corePoolSize;
        int maxPoolSize;
        int separatorIndex = this.poolSize.indexOf('-');
        if (separatorIndex != -1) {
          corePoolSize = Integer.parseInt(this.poolSize, 0, separatorIndex, 10);
          maxPoolSize = Integer.parseInt(this.poolSize, separatorIndex + 1, this.poolSize.length(), 10);
          if (corePoolSize > maxPoolSize) {
            throw new IllegalArgumentException(
                    "Lower bound of pool-size range must not exceed the upper bound");
          }
          if (this.queueCapacity == null) {
            // No queue-capacity provided, so unbounded
            if (corePoolSize == 0) {
              // Actually set 'corePoolSize' to the upper bound of the range
              // but allow core threads to timeout...
              executor.setAllowCoreThreadTimeOut(true);
              corePoolSize = maxPoolSize;
            }
            else {
              // Non-zero lower bound implies a core-max size range...
              throw new IllegalArgumentException(
                      "A non-zero lower bound for the size range requires a queue-capacity value");
            }
          }
        }
        else {
          int value = Integer.parseInt(this.poolSize);
          corePoolSize = value;
          maxPoolSize = value;
        }
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
      }
      catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Invalid pool-size value [" + this.poolSize + "]: only single " +
                "maximum integer (e.g. \"5\") and minimum-maximum range (e.g. \"3-5\") are supported", ex);
      }
    }
  }

  @Override
  @Nullable
  public TaskExecutor getObject() {
    return this.target;
  }

  @Override
  public Class<? extends TaskExecutor> getObjectType() {
    return this.target != null ? this.target.getClass() : ThreadPoolTaskExecutor.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void destroy() {
    if (this.target != null) {
      this.target.destroy();
    }
  }

}
