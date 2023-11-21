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

package cn.taketoday.context.support;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * {@link AbstractRefreshableApplicationContext} subclass that adds common handling
 * of specified config locations.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #setConfigLocation
 * @see #setConfigLocations
 * @see #getDefaultConfigLocations
 * @since 4.0 2022/2/20 17:39
 */
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
        implements BeanNameAware, InitializingBean {

  @Nullable
  private String[] configLocations;

  private boolean setIdCalled = false;

  /**
   * Create a new AbstractRefreshableConfigApplicationContext with no parent.
   */
  public AbstractRefreshableConfigApplicationContext() { }

  /**
   * Create a new AbstractRefreshableConfigApplicationContext with the given parent context.
   *
   * @param parent the parent context
   */
  public AbstractRefreshableConfigApplicationContext(@Nullable ApplicationContext parent) {
    super(parent);
  }

  /**
   * Set the config locations for this application context in init-param style,
   * i.e. with distinct locations separated by commas, semicolons or whitespace.
   * <p>If not set, the implementation may use a default as appropriate.
   */
  public void setConfigLocation(String location) {
    setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
  }

  /**
   * Set the config locations for this application context.
   * <p>If not set, the implementation may use a default as appropriate.
   */
  public void setConfigLocations(@Nullable String... locations) {
    if (locations != null) {
      Assert.noNullElements(locations, "Config locations is required");
      this.configLocations = new String[locations.length];
      for (int i = 0; i < locations.length; i++) {
        this.configLocations[i] = resolvePath(locations[i]).trim();
      }
    }
    else {
      this.configLocations = null;
    }
  }

  /**
   * Return an array of resource locations, referring to the XML bean definition
   * files that this context should be built with. Can also include location
   * patterns, which will get resolved via a ResourcePatternResolver.
   * <p>The default implementation returns {@code null}. Subclasses can override
   * this to provide a set of resource locations to load bean definitions from.
   *
   * @return an array of resource locations, or {@code null} if none
   * @see #getResources
   */
  @Nullable
  protected String[] getConfigLocations() {
    return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
  }

  /**
   * Return the default config locations to use, for the case where no
   * explicit config locations have been specified.
   * <p>The default implementation returns {@code null},
   * requiring explicit config locations.
   *
   * @return an array of default config locations, if any
   * @see #setConfigLocations
   */
  @Nullable
  protected String[] getDefaultConfigLocations() {
    return null;
  }

  /**
   * Resolve the given path, replacing placeholders with corresponding
   * environment property values if necessary. Applied to config locations.
   *
   * @param path the original file path
   * @return the resolved file path
   * @see cn.taketoday.core.env.Environment#resolveRequiredPlaceholders(String)
   */
  protected String resolvePath(String path) {
    return getEnvironment().resolveRequiredPlaceholders(path);
  }

  @Override
  public void setId(String id) {
    super.setId(id);
    this.setIdCalled = true;
  }

  /**
   * Sets the id of this context to the bean name by default,
   * for cases where the context instance is itself defined as a bean.
   */
  @Override
  public void setBeanName(String name) {
    if (!this.setIdCalled) {
      super.setId(name);
      setDisplayName("ApplicationContext '" + name + "'");
    }
  }

  /**
   * Triggers {@link #refresh()} if not refreshed in the concrete context's
   * constructor already.
   */
  @Override
  public void afterPropertiesSet() {
    if (!isActive()) {
      refresh();
    }
  }

}

