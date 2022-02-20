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

package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 21:24
 */
public class AnnotationConfigApplicationContext extends StandardApplicationContext {

  public AnnotationConfigApplicationContext() { }

  public AnnotationConfigApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  public AnnotationConfigApplicationContext(@Nullable ApplicationContext parent) {
    super(parent);
  }

  public AnnotationConfigApplicationContext(StandardBeanFactory beanFactory, ApplicationContext parent) {
    super(beanFactory, parent);
  }

  public AnnotationConfigApplicationContext(Class<?>... components) {
    super(components);
  }

  public AnnotationConfigApplicationContext(String... basePackages) {
    super(basePackages);
  }
}
