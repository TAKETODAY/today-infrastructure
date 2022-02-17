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

package cn.taketoday.format.datetime.standard;

import java.time.format.DateTimeFormatter;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Nullable;

/**
 * {@link FactoryBean} that creates a JSR-310 {@link DateTimeFormatter}.
 * See the {@link DateTimeFormatterFactory base class} for configuration details.
 *
 * @author Juergen Hoeller
 * @see #setPattern
 * @see #setIso
 * @see #setDateStyle
 * @see #setTimeStyle
 * @see DateTimeFormatterFactory
 * @since 4.0
 */
public class DateTimeFormatterFactoryBean extends DateTimeFormatterFactory
        implements FactoryBean<DateTimeFormatter>, InitializingBean {

  @Nullable
  private DateTimeFormatter dateTimeFormatter;

  @Override
  public void afterPropertiesSet() {
    this.dateTimeFormatter = createDateTimeFormatter();
  }

  @Override
  @Nullable
  public DateTimeFormatter getObject() {
    return this.dateTimeFormatter;
  }

  @Override
  public Class<?> getObjectType() {
    return DateTimeFormatter.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
