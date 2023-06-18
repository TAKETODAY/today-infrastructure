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

package cn.taketoday.context.properties.bind;

import java.io.Serial;

import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginProvider;

/**
 * Exception thrown when binding fails.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public class BindException extends RuntimeException implements OriginProvider {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Bindable<?> target;

  private final ConfigurationProperty property;

  private final ConfigurationPropertyName name;

  BindException(ConfigurationPropertyName name, Bindable<?> target, ConfigurationProperty property, Throwable cause) {
    super(buildMessage(name, target), cause);
    this.name = name;
    this.target = target;
    this.property = property;
  }

  /**
   * Return the name of the configuration property being bound.
   *
   * @return the configuration property name
   */
  public ConfigurationPropertyName getName() {
    return this.name;
  }

  /**
   * Return the target being bound.
   *
   * @return the bind target
   */
  public Bindable<?> getTarget() {
    return this.target;
  }

  /**
   * Return the configuration property name of the item that was being bound.
   *
   * @return the configuration property name
   */
  public ConfigurationProperty getProperty() {
    return this.property;
  }

  @Override
  public Origin getOrigin() {
    return Origin.from(this.name);
  }

  private static String buildMessage(ConfigurationPropertyName name, Bindable<?> target) {
    StringBuilder message = new StringBuilder();
    message.append("Failed to bind properties");
    message.append((name != null) ? " under '" + name + "'" : "");
    message.append(" to ").append(target.getType());
    return message.toString();
  }

}
