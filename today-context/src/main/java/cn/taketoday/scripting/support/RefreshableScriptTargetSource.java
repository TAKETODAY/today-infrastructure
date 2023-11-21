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
package cn.taketoday.scripting.support;

import cn.taketoday.aop.target.BeanFactoryRefreshableTargetSource;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.scripting.ScriptFactory;
import cn.taketoday.scripting.ScriptSource;

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
