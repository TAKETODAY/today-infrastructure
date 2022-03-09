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

package cn.taketoday.aop.framework.autoproxy;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.lang.Nullable;

/**
 * {@code BeanPostProcessor} implementation that creates AOP proxies based on all
 * candidate {@code Advisor}s in the current {@code BeanFactory}. This class is
 * completely generic; it contains no special code to handle any particular aspects,
 * such as pooling aspects.
 *
 * <p>It's possible to filter out advisors - for example, to use multiple post processors
 * of this type in the same factory - by setting the {@code usePrefix} property to true,
 * in which case only advisors beginning with the DefaultAdvisorAutoProxyCreator's bean
 * name followed by a dot (like "aapc.") will be used. This default prefix can be changed
 * from the bean name by setting the {@code advisorBeanNamePrefix} property.
 * The separator (.) will also be used in this case.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 4.0
 */
@SuppressWarnings("serial")
public class DefaultAdvisorAutoProxyCreator extends AbstractAdvisorAutoProxyCreator implements BeanNameAware {

  /** Separator between prefix and remainder of bean name. */
  public static final String SEPARATOR = ".";

  private boolean usePrefix = false;

  @Nullable
  private String advisorBeanNamePrefix;

  /**
   * Set whether to only include advisors with a certain prefix in the bean name.
   * <p>Default is {@code false}, including all beans of type {@code Advisor}.
   *
   * @see #setAdvisorBeanNamePrefix
   */
  public void setUsePrefix(boolean usePrefix) {
    this.usePrefix = usePrefix;
  }

  /**
   * Return whether to only include advisors with a certain prefix in the bean name.
   */
  public boolean isUsePrefix() {
    return this.usePrefix;
  }

  /**
   * Set the prefix for bean names that will cause them to be included for
   * auto-proxying by this object. This prefix should be set to avoid circular
   * references. Default value is the bean name of this object + a dot.
   *
   * @param advisorBeanNamePrefix the exclusion prefix
   */
  public void setAdvisorBeanNamePrefix(@Nullable String advisorBeanNamePrefix) {
    this.advisorBeanNamePrefix = advisorBeanNamePrefix;
  }

  /**
   * Return the prefix for bean names that will cause them to be included
   * for auto-proxying by this object.
   */
  @Nullable
  public String getAdvisorBeanNamePrefix() {
    return this.advisorBeanNamePrefix;
  }

  @Override
  public void setBeanName(String name) {
    // If no infrastructure bean name prefix has been set, override it.
    if (this.advisorBeanNamePrefix == null) {
      this.advisorBeanNamePrefix = name + SEPARATOR;
    }
  }

  /**
   * Consider {@code Advisor} beans with the specified prefix as eligible, if activated.
   *
   * @see #setUsePrefix
   * @see #setAdvisorBeanNamePrefix
   */
  @Override
  protected boolean isEligibleAdvisorBean(String beanName) {
    if (!isUsePrefix()) {
      return true;
    }
    String prefix = getAdvisorBeanNamePrefix();
    return prefix != null && beanName.startsWith(prefix);
  }

}
