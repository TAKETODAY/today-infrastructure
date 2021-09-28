/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.core;

import java.util.function.Supplier;

import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;

/**
 * Configuration exception
 *
 * @author TODAY <br>
 * 2018-08-08 09:55
 */
public class ConfigurationException extends NestedRuntimeException {
  private static final long serialVersionUID = 1L;
  private static final Logger log = LoggerFactory.getLogger(ConfigurationException.class);

  public ConfigurationException() { }

  public ConfigurationException(String message) {
    this(message, null);
  }

  public ConfigurationException(Throwable cause) {
    this(null, cause);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
    log.error("Configuration Exception Message: [" + message + "]", this);
  }

  public static <T> T nonNull(final T obj) {
    return nonNull(obj, "object must not be null");
  }

  public static <T> T nonNull(final T obj, final String msg) {
    if (obj == null) {
      throw new ConfigurationException(msg);
    }
    return obj;
  }

  public static <T> T nonNull(final T obj, final Supplier<String> msg) {
    if (obj == null) {
      throw new ConfigurationException(msg.get());
    }
    return obj;
  }

}
