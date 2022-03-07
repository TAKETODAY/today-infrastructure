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

package cn.taketoday.beans.factory.xml;

import cn.taketoday.beans.factory.BeanNameAware;

/**
 * @author Rob Harrop
 */
public class GeneratedNameBean implements BeanNameAware {

  private String beanName;

  private GeneratedNameBean child;

  @Override
  public void setBeanName(String beanName) {
    this.beanName = beanName;
  }

  public String getBeanName() {
    return beanName;
  }

  public void setChild(GeneratedNameBean child) {
    this.child = child;
  }

  public GeneratedNameBean getChild() {
    return child;
  }

}
