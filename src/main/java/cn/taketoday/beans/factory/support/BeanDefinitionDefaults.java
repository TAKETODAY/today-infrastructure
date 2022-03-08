/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * A simple holder for {@code BeanDefinition} property defaults.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see AbstractBeanDefinition#applyDefaults
 * @since 4.0 2021/12/22 21:43
 */
public class BeanDefinitionDefaults {

  @Nullable
  private Boolean lazyInit;

  private int autowireMode = BeanDefinition.AUTOWIRE_NO;
  private int dependencyCheck = AbstractBeanDefinition.DEPENDENCY_CHECK_NONE;

  @Nullable
  private String initMethodName;

  @Nullable
  private String destroyMethodName;

  /**
   * Set the autowire mode. This determines whether any automagical detection
   * and setting of bean references will happen. Default is AUTOWIRE_NO
   * which means there won't be convention-based autowiring by name or type
   * (however, there may still be explicit annotation-driven autowiring).
   *
   * @param autowireMode the autowire mode to set.
   * Must be one of the constants defined in {@link BeanDefinition}.
   * @see AbstractBeanDefinition#setAutowireMode
   */
  public void setAutowireMode(int autowireMode) {
    this.autowireMode = autowireMode;
  }

  /**
   * Return the default autowire mode.
   */
  public int getAutowireMode() {
    return this.autowireMode;
  }

  /**
   * Set whether beans should be lazily initialized by default.
   * <p>If {@code false}, the bean will get instantiated on startup by bean
   * factories that perform eager initialization of singletons.
   *
   * @see BeanDefinition#setLazyInit
   */
  public void setLazyInit(boolean lazyInit) {
    this.lazyInit = lazyInit;
  }

  /**
   * Return whether beans should be lazily initialized by default, i.e. not
   * eagerly instantiated on startup. Only applicable to singleton beans.
   *
   * @return whether to apply lazy-init semantics ({@code false} by default)
   */
  public boolean isLazyInit() {
    return this.lazyInit != null && this.lazyInit;
  }

  /**
   * Return whether beans should be lazily initialized by default, i.e. not
   * eagerly instantiated on startup. Only applicable to singleton beans.
   *
   * @return the lazy-init flag if explicitly set, or {@code null} otherwise
   */
  @Nullable
  public Boolean getLazyInit() {
    return this.lazyInit;
  }

  /**
   * Set the name of the default initializer method.
   * <p>Note that this method is not enforced on all affected bean definitions
   * but rather taken as an optional callback, to be invoked if actually present.
   *
   * @see AbstractBeanDefinition#setInitMethodName
   * @see AbstractBeanDefinition#setEnforceInitMethod
   */
  public void setInitMethodName(@Nullable String initMethodName) {
    this.initMethodName = initMethodName;
  }

  /**
   * Return the name of the default initializer method.
   */
  @Nullable
  public String getInitMethodName() {
    return this.initMethodName;
  }

  /**
   * Set the name of the default destroy method.
   * <p>Note that this method is not enforced on all affected bean definitions
   * but rather taken as an optional callback, to be invoked if actually present.
   *
   * @see AbstractBeanDefinition#setDestroyMethodName
   * @see AbstractBeanDefinition#setEnforceDestroyMethod
   */
  public void setDestroyMethodName(@Nullable String destroyMethodName) {
    this.destroyMethodName = StringUtils.hasText(destroyMethodName) ? destroyMethodName : null;
  }

  /**
   * Return the name of the default destroy method.
   */
  @Nullable
  public String getDestroyMethodName() {
    return this.destroyMethodName;
  }

  /**
   * Set the dependency check code.
   *
   * @param dependencyCheck the code to set.
   * Must be one of the constants defined in {@link AbstractBeanDefinition}.
   * @see AbstractBeanDefinition#setDependencyCheck
   */
  public void setDependencyCheck(int dependencyCheck) {
    this.dependencyCheck = dependencyCheck;
  }

  /**
   * Return the default dependency check code.
   */
  public int getDependencyCheck() {
    return this.dependencyCheck;
  }

}
