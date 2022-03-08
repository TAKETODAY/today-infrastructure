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

package cn.taketoday.beans.factory.config;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Context object for evaluating an expression within a bean definition.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class BeanExpressionContext {

  @Nullable
  private final Scope scope;

  private final ConfigurableBeanFactory beanFactory;

  public BeanExpressionContext(ConfigurableBeanFactory beanFactory, @Nullable Scope scope) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanFactory = beanFactory;
    this.scope = scope;
  }

  public final ConfigurableBeanFactory getBeanFactory() {
    return this.beanFactory;
  }

  @Nullable
  public final Scope getScope() {
    return this.scope;
  }

  public boolean containsObject(String key) {
    return this.beanFactory.containsBean(key)
            || (this.scope != null && this.scope.resolveContextualObject(key) != null);
  }

  @Nullable
  public Object getObject(String key) {
    if (this.beanFactory.containsBean(key)) {
      return this.beanFactory.getBean(key);
    }
    else if (this.scope != null) {
      return this.scope.resolveContextualObject(key);
    }
    else {
      return null;
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof BeanExpressionContext otherContext)) {
      return false;
    }
    return (this.beanFactory == otherContext.beanFactory && this.scope == otherContext.scope);
  }

  @Override
  public int hashCode() {
    return this.beanFactory.hashCode();
  }

}
