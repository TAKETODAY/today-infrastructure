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
package infra.orm.mybatis.annotation.factory;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.stereotype.Component;

@Component
public class SimpleFactoryBean implements FactoryBean<Object> {

  private static boolean isInitializedEarly = false;

  public SimpleFactoryBean() {
    isInitializedEarly = true;
    throw new RuntimeException();
  }

  @Autowired
  public SimpleFactoryBean(ApplicationContext context) {
    if (isInitializedEarly) {
      throw new RuntimeException();
    }
  }

  public Object getObject() {
    return new Object();
  }

  public Class<?> getObjectType() {
    return Object.class;
  }

}
