/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.redisson.web.session;

import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import infra.context.annotation.Configuration;
import infra.context.annotation.ImportAware;
import infra.core.type.AnnotationMetadata;
import infra.lang.Nullable;
import infra.session.SessionEventDispatcher;
import infra.session.SessionIdGenerator;
import infra.stereotype.Component;

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
    var annotation = importMetadata.getAnnotation(EnableRedissonWebSession.class);
    this.keyPrefix = annotation.getString("keyPrefix");
    this.maxIdleTime = annotation.getInt("maxIdleTime");
    this.timeUnit = annotation.getEnum("timeUnit", TimeUnit.class);
  }

}
