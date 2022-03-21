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

package cn.taketoday.aop.framework;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;

/**
 * @author Oliver Gierke
 */
@Component
public class ClassWithComplexConstructor {

  private final Dependency dependency;

  ClassWithComplexConstructor selfReference;

  @Autowired
  public ClassWithComplexConstructor(Dependency dependency) {
    Assert.notNull(dependency, "No Dependency bean injected");
    this.dependency = dependency;
  }

  public Dependency getDependency() {
    return this.dependency;
  }

  @Autowired
  public void setSelfReference(ClassWithComplexConstructor selfReference) {
    this.selfReference = selfReference;
  }

  public void method() {
    Assert.state(this.selfReference != this && AopUtils.isCglibProxy(this.selfReference),
            "Self reference must be a CGLIB proxy");
    this.dependency.method();
  }

}
