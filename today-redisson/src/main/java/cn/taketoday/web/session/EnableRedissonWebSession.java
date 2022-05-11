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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.web.session.EnableWebSession;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/6 14:26
 */
@EnableWebSession
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RedissonWebSessionConfiguration.class)
public @interface EnableRedissonWebSession {

  /**
   * Return the maximum time after the lastAccessTime before a session expires.
   * A negative time indicates the session doesn't expire.
   *
   * @see WebSession#getMaxIdleTime()
   */
  int maxIdleTime() default 1800;

  TimeUnit timeUnit() default TimeUnit.SECONDS;

  /**
   * session key prefix
   */
  String keyPrefix() default "";

}

