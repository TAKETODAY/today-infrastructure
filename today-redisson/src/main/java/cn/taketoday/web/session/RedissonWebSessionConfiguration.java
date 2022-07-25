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

package cn.taketoday.web.session;

import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ImportAware;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.session.SessionEventDispatcher;
import cn.taketoday.session.SessionIdGenerator;
import cn.taketoday.stereotype.Component;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/6 14:27
 */
@Configuration(proxyBeanMethods = false)
public class RedissonWebSessionConfiguration implements ImportAware {

  @Nullable
  private String keyPrefix;

  @Nullable
  private Integer maxIdleTime;

  @Nullable
  private TimeUnit timeUnit;

  @Component
  public RedissonSessionRepository redissonSessionRepository(RedissonClient client,
          SessionIdGenerator idGenerator, SessionEventDispatcher eventDispatcher) {
    var repository = new RedissonSessionRepository(
            client, keyPrefix, idGenerator, eventDispatcher);

    if (maxIdleTime != null && timeUnit != null) {
      Duration duration = Duration.of(maxIdleTime, timeUnit.toChronoUnit());
      repository.setDefaultMaxInactiveInterval(duration);
    }
    return repository;
  }

  public void setMaxIdleTime(@Nullable Integer maxIdleTime) {
    this.maxIdleTime = maxIdleTime;
  }

  public void setKeyPrefix(@Nullable String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  public void setTimeUnit(@Nullable TimeUnit timeUnit) {
    this.timeUnit = timeUnit;
  }

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    var enableRedissonWebSession = importMetadata.getAnnotation(EnableRedissonWebSession.class);
    this.keyPrefix = enableRedissonWebSession.getString("keyPrefix");
    this.maxIdleTime = enableRedissonWebSession.getInt("maxIdleTime");
    this.timeUnit = enableRedissonWebSession.getEnum("timeUnit", TimeUnit.class);
  }

}
