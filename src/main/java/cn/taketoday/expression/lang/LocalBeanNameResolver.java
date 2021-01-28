/**
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
package cn.taketoday.expression.lang;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.expression.BeanNameResolver;

/**
 * @author TODAY <br>
 * 2019-02-19 17:43
 */
public class LocalBeanNameResolver extends BeanNameResolver {

  private final Map<String, Object> beans;

  public LocalBeanNameResolver() {
    this(new HashMap<String, Object>(8, 1.0f));
  }

  public LocalBeanNameResolver(Map<String, Object> beans) {
    this.beans = beans;
  }

  @Override
  public boolean isNameResolved(String beanName) {
    return beans.containsKey(beanName);
  }

  @Override
  public Object getBean(String beanName) {
    return beans.get(beanName);
  }

  @Override
  public void setBeanValue(String beanName, Object value) {
    beans.put(beanName, value);
  }

  @Override
  public boolean isReadOnly(String beanName) {
    return false;
  }

  @Override
  public boolean canCreateBean(String beanName) {
    return true;
  }
}
