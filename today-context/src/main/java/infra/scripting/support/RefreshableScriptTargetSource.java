/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
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
  public RefreshableScriptTargetSource(
          BeanFactory beanFactory, String beanName,
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
