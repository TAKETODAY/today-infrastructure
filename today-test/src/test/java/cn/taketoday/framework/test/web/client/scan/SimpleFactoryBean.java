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

package cn.taketoday.framework.test.web.client.scan;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.stereotype.Component;

/**
 * A simple factory bean with no generics. Used to test early initialization doesn't
 * occur.
 *
 * @author Madhura Bhave
 */
@Component
@SuppressWarnings("rawtypes")
public class SimpleFactoryBean implements FactoryBean {

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

  @Override
  public Object getObject() {
    return new Object();
  }

  @Override
  public Class<?> getObjectType() {
    return Object.class;
  }

}
