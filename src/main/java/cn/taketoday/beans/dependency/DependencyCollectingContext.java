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

package cn.taketoday.beans.dependency;

import java.util.LinkedHashSet;

import cn.taketoday.beans.factory.BeanDefinition;

/**
 * Dependency resolving context
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 20:46</a>
 * @since 4.0
 */
public class DependencyCollectingContext {

  private final Object bean;
  private final String beanName;
  private final BeanDefinition definition;

  private LinkedHashSet<DependencySetter> dependencies;

  public DependencyCollectingContext(Object bean, BeanDefinition definition) {
    this.bean = bean;
    this.definition = definition;
    this.beanName = definition.getName();
  }

  public void addDependency(DependencySetter dependency) {
    dependencies().add(dependency);
  }

  public LinkedHashSet<DependencySetter> dependencies() {
    if (dependencies == null) {
      dependencies = new LinkedHashSet<>();
    }
    return dependencies;
  }

  public LinkedHashSet<DependencySetter> getDependencies() {
    return dependencies;
  }

  public BeanDefinition getDefinition() {
    return definition;
  }

  public String getBeanName() {
    return beanName;
  }

  public Class<?> getBeanClass() {
    return bean.getClass();
  }

  public Object getBean() {
    return bean;
  }

}
