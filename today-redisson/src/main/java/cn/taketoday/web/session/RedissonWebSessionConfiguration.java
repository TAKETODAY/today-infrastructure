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

import java.util.concurrent.TimeUnit;

import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.aware.ImportAware;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Component;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/6 14:27
 */
@Configuration(proxyBeanMethods = false)
public class RedissonWebSessionConfiguration implements ImportAware {

  private String keyPrefix;
  private Integer maxIdleTime;
  private TimeUnit timeUnit;

  @Component
  public RedissonSessionRepository redissonSessionRepository(
          RedissonClient redissonClient, ApplicationEventPublisher eventPublisher) {
    RedissonSessionRepository repository = new RedissonSessionRepository(redissonClient, eventPublisher, keyPrefix);
    timeUnit.toSeconds(maxIdleTime);
    repository.setDefaultMaxInactiveInterval(maxIdleTime);
    return repository;
  }

  public void setMaxIdleTime(Integer maxIdleTime) {
    this.maxIdleTime = maxIdleTime;
  }

  public void setKeyPrefix(String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  @Override
  public void setImportMetadata(AnnotationMetadata importMetadata) {
    var enableRedissonWebSession = importMetadata.getAnnotation(EnableRedissonWebSession.class);
    this.keyPrefix = enableRedissonWebSession.getString("keyPrefix");
    this.maxIdleTime = enableRedissonWebSession.getInt("maxIdleTime");
    this.timeUnit = enableRedissonWebSession.getEnum("timeUnit", TimeUnit.class);
  }

}
