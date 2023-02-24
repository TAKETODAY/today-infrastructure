/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.lang.Assert;

/**
 * Describes scope characteristics
 *
 * <p>The default scope is "singleton"
 *
 * @author TODAY 2021/10/26 15:58
 * @see ScopeMetadataResolver
 * @since 4.0
 */
public class ScopeMetadata {

  private String scopeName = BeanDefinition.SCOPE_SINGLETON;

  private ScopedProxyMode scopedProxyMode = ScopedProxyMode.NO;

  /**
   * Set the name of the scope.
   */
  public void setScopeName(String scopeName) {
    Assert.notNull(scopeName, "'scopeName' must not be null");
    this.scopeName = scopeName;
  }

  /**
   * Get the name of the scope.
   */
  public String getScopeName() {
    return this.scopeName;
  }

  /**
   * Set the proxy-mode to be applied to the scoped instance.
   */
  public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
    Assert.notNull(scopedProxyMode, "'scopedProxyMode' must not be null");
    this.scopedProxyMode = scopedProxyMode;
  }

  /**
   * Get the proxy-mode to be applied to the scoped instance.
   */
  public ScopedProxyMode getScopedProxyMode() {
    return this.scopedProxyMode;
  }

}
