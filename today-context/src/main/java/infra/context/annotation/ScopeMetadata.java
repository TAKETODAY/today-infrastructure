/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.context.annotation;

import infra.beans.factory.config.BeanDefinition;
import infra.lang.Assert;

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
    Assert.notNull(scopeName, "'scopeName' is required");
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
    Assert.notNull(scopedProxyMode, "'scopedProxyMode' is required");
    this.scopedProxyMode = scopedProxyMode;
  }

  /**
   * Get the proxy-mode to be applied to the scoped instance.
   */
  public ScopedProxyMode getScopedProxyMode() {
    return this.scopedProxyMode;
  }

}
