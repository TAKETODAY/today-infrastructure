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

package cn.taketoday.beans.factory.support;

import java.lang.reflect.Executable;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/16 22:42</a>
 * @since 4.0
 */
public class DependencyResolvingContext {

  @Nullable
  private final Executable executable;

  @Nullable
  private final BeanFactory beanFactory;

  // dependency instance
  @Nullable
  private Object dependency;

  @Nullable
  private String beanName;

  private boolean dependencyResolved;

  private Set<String> dependentBeans;

  private TypeConverter typeConverter;

  public DependencyResolvingContext(
          @Nullable Executable executable, @Nullable BeanFactory beanFactory) {
    this.executable = executable;
    this.beanFactory = beanFactory;
  }

  public DependencyResolvingContext(
          @Nullable Executable executable,
          @Nullable BeanFactory beanFactory,
          @Nullable String beanName) {
    this.beanName = beanName;
    this.executable = executable;
    this.beanFactory = beanFactory;
  }

  public void setDependencyResolved(Object dependency) {
    this.dependency = dependency;
    setDependencyResolved(true);
  }

  public void setDependencyResolved(boolean resolved) {
    this.dependencyResolved = resolved;
  }

  public boolean isDependencyResolved() {
    return dependencyResolved;
  }

  public void setBeanName(@Nullable String beanName) {
    this.beanName = beanName;
  }

  @Nullable
  public String getBeanName() {
    return beanName;
  }

  @Nullable
  public Executable getExecutable() {
    return executable;
  }

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public boolean hasBeanFactory() {
    return beanFactory != null;
  }

  @Nullable
  public Object getDependency() {
    return dependency;
  }

  public void setDependency(@Nullable Object dependency) {
    this.dependency = dependency;
  }

  public boolean hasDependency() {
    return dependency != null;
  }

  public void setTypeConverter(TypeConverter typeConverter) {
    this.typeConverter = typeConverter;
  }

  public TypeConverter getTypeConverter() {
    return typeConverter;
  }
// dependentBeans

  void setDependentBeans(Set<String> dependentBeans) {
    this.dependentBeans = dependentBeans;
  }

  // record
  @Nullable
  public Set<String> getDependentBeans() {
    return dependentBeans;
  }

  public Set<String> dependentBeans() {
    if (dependentBeans == null) {
      dependentBeans = new LinkedHashSet<>();
    }
    return dependentBeans;
  }

  public void addDependentBean(String beanName) {
    dependentBeans().add(beanName);
  }

  @Override
  public String toString() {
    return "DependencyResolvingContext{" +
            "executable=" + executable +
            ", beanFactory=" + beanFactory +
            ", dependency=" + dependency +
            '}';
  }
}
