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

package cn.taketoday.transaction.interceptor;

import cn.taketoday.beans.testfixture.beans.TestBean;

/**
 * Test for CGLIB proxying that implements no interfaces
 * and has one dependency.
 *
 * @author Rod Johnson
 */
public class ImplementsNoInterfaces {

  private TestBean testBean;

  public void setDependency(TestBean testBean) {
    this.testBean = testBean;
  }

  public String getName() {
    return testBean.getName();
  }

  public void setName(String name) {
    testBean.setName(name);
  }

}
