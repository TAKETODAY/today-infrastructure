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

package cn.taketoday.web.testfixture.beans;

import java.io.Serializable;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 21:59
 */
public class DerivedTestBean extends TestBean implements Serializable, BeanNameAware, DisposableBean {

  private String beanName;

  private boolean initialized;

  private boolean destroyed;
  private String name;

  public DerivedTestBean() {
  }

  public DerivedTestBean(String[] names) {
    if (names == null || names.length < 2) {
      throw new IllegalArgumentException("Invalid names array");
    }
    setName(names[0]);
    setBeanName(names[1]);
  }

  public static DerivedTestBean create(String[] names) {
    return new DerivedTestBean(names);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setBeanName(String beanName) {
    if (this.beanName == null || beanName == null) {
      this.beanName = beanName;
    }
  }

  @Override
  public String getBeanName() {
    return beanName;
  }

  public void setActualSpouse(TestBean spouse) {
    setSpouse(spouse);
  }

  public void setSpouseRef(String name) {
    setSpouse(new TestBean(name));
  }

  public TestBean getSpouse() {
    return (TestBean) super.getSpouse();
  }

  public void initialize() {
    this.initialized = true;
  }

  public boolean wasInitialized() {
    return initialized;
  }

  @Override
  public void destroy() {
    this.destroyed = true;
  }

  @Override
  public boolean wasDestroyed() {
    return destroyed;
  }

}
