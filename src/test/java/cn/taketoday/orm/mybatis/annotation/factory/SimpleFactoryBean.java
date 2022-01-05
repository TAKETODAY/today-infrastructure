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
package cn.taketoday.orm.mybatis.annotation.factory;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Component;

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
