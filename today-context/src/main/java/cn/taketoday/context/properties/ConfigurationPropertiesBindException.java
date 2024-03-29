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

package cn.taketoday.context.properties;

import java.io.Serial;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Exception thrown when {@link ConfigurationProperties @ConfigurationProperties} binding
 * fails.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConfigurationPropertiesBindException extends BeanCreationException {

  @Serial
  private static final long serialVersionUID = 1L;

  private final ConfigurationPropertiesBean bean;

  ConfigurationPropertiesBindException(ConfigurationPropertiesBean bean, Exception cause) {
    super(bean.getName(), getMessage(bean), cause);
    this.bean = bean;
  }

  /**
   * Return the bean type that was being bound.
   *
   * @return the bean type
   */
  @Nullable
  public Class<?> getBeanType() {
    return this.bean.getType();
  }

  /**
   * Return the configuration properties annotation that triggered the binding.
   *
   * @return the configuration properties annotation
   */
  public ConfigurationProperties getAnnotation() {
    return this.bean.getAnnotation();
  }

  private static String getMessage(ConfigurationPropertiesBean bean) {
    ConfigurationProperties annotation = bean.getAnnotation();
    StringBuilder message = new StringBuilder();
    message.append("Could not bind properties to '");
    message.append(ClassUtils.getShortName(bean.getType())).append("' : ");
    message.append("prefix=").append(annotation.prefix());
    message.append(", ignoreInvalidFields=").append(annotation.ignoreInvalidFields());
    message.append(", ignoreUnknownFields=").append(annotation.ignoreUnknownFields());
    return message.toString();
  }

}
