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

package infra.scripting.support;

import infra.aop.target.BeanFactoryRefreshableTargetSource;
import infra.beans.factory.BeanFactory;
import infra.lang.Assert;
import infra.scripting.ScriptFactory;
import infra.scripting.ScriptSource;

/**
 * Subclass of {@link BeanFactoryRefreshableTargetSource} that determines whether
 * a refresh is required through the given {@link ScriptFactory}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public class RefreshableScriptTargetSource extends BeanFactoryRefreshableTargetSource {

  private final ScriptFactory scriptFactory;

  private final ScriptSource scriptSource;

  private final boolean isFactoryBean;

  /**
   * Create a new RefreshableScriptTargetSource.
   *
   * @param beanFactory the BeanFactory to fetch the scripted bean from
   * @param beanName the name of the target bean
   * @param scriptFactory the ScriptFactory to delegate to for determining
   * whether a refresh is required
   * @param scriptSource the ScriptSource for the script definition
   * @param isFactoryBean whether the target script defines a FactoryBean
   */
  public RefreshableScriptTargetSource(BeanFactory beanFactory, String beanName,
          ScriptFactory scriptFactory, ScriptSource scriptSource, boolean isFactoryBean) {

    super(beanFactory, beanName);
    Assert.notNull(scriptFactory, "ScriptFactory is required");
    Assert.notNull(scriptSource, "ScriptSource is required");
    this.scriptFactory = scriptFactory;
    this.scriptSource = scriptSource;
    this.isFactoryBean = isFactoryBean;
  }

  /**
   * Determine whether a refresh is required through calling
   * ScriptFactory's {@code requiresScriptedObjectRefresh} method.
   *
   * @see ScriptFactory#requiresScriptedObjectRefresh(ScriptSource)
   */
  @Override
  protected boolean requiresRefresh() {
    return this.scriptFactory.requiresScriptedObjectRefresh(this.scriptSource);
  }

  /**
   * Obtain a fresh target object, retrieving a FactoryBean if necessary.
   */
  @Override
  protected Object obtainFreshBean(BeanFactory beanFactory, String beanName) {
    return super.obtainFreshBean(beanFactory,
            this.isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
  }

}
