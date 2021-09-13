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

package cn.taketoday.aop;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;

public class TestBean implements ITestBean, BeanNameAware, BeanFactoryAware, IOther, Comparable<Object> {
  private int age;
  private String name;
  private ITestBean spouse;

  @Override
  public void exceptional(Throwable t) throws Throwable {
    if (t != null) {
      throw t;
    }
  }

  @Override
  public int haveBirthday() {
    return age++;
  }

  public TestBean() { }

  public TestBean(String name) {
    this.name = name;
  }

  public void setSpouse(ITestBean spouse) {
    this.spouse = spouse;
  }

  public void absquatulate() {

  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  @Override
  public ITestBean getSpouse() {
    return spouse;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  private BeanFactory beanFactory;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  private String beanName;

  public String getBeanName() {
    return beanName;
  }

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  @Override
  public int compareTo(Object other) {
    if (this.name != null && other instanceof TestBean) {
      return this.name.compareTo(((TestBean) other).getName());
    }
    else {
      return 1;
    }
  }

  @Override
  public String toString() {
    return name;
  }

  private boolean destroyed;

  public void destroy() {
    this.destroyed = true;
  }

  public boolean wasDestroyed() {
    return destroyed;
  }
}
