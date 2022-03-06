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

package cn.taketoday.beans.factory.xml;

import cn.taketoday.beans.factory.parsing.DefaultsDefinition;
import cn.taketoday.lang.Nullable;

/**
 * Simple JavaBean that holds the defaults specified at the {@code <beans>}
 * level in a standard Framework XML bean definition document:
 * {@code default-lazy-init}, {@code default-autowire}, etc.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class DocumentDefaultsDefinition implements DefaultsDefinition {

  @Nullable
  private String lazyInit;

  @Nullable
  private String merge;

  @Nullable
  private String autowire;

  @Nullable
  private String autowireCandidates;

  @Nullable
  private String initMethod;

  @Nullable
  private String destroyMethod;

  @Nullable
  private Object source;

  /**
   * Set the default lazy-init flag for the document that's currently parsed.
   */
  public void setLazyInit(@Nullable String lazyInit) {
    this.lazyInit = lazyInit;
  }

  /**
   * Return the default lazy-init flag for the document that's currently parsed.
   */
  @Nullable
  public String getLazyInit() {
    return this.lazyInit;
  }

  /**
   * Set the default merge setting for the document that's currently parsed.
   */
  public void setMerge(@Nullable String merge) {
    this.merge = merge;
  }

  /**
   * Return the default merge setting for the document that's currently parsed.
   */
  @Nullable
  public String getMerge() {
    return this.merge;
  }

  /**
   * Set the default autowire setting for the document that's currently parsed.
   */
  public void setAutowire(@Nullable String autowire) {
    this.autowire = autowire;
  }

  /**
   * Return the default autowire setting for the document that's currently parsed.
   */
  @Nullable
  public String getAutowire() {
    return this.autowire;
  }

  /**
   * Set the default autowire-candidate pattern for the document that's currently parsed.
   * Also accepts a comma-separated list of patterns.
   */
  public void setAutowireCandidates(@Nullable String autowireCandidates) {
    this.autowireCandidates = autowireCandidates;
  }

  /**
   * Return the default autowire-candidate pattern for the document that's currently parsed.
   * May also return a comma-separated list of patterns.
   */
  @Nullable
  public String getAutowireCandidates() {
    return this.autowireCandidates;
  }

  /**
   * Set the default init-method setting for the document that's currently parsed.
   */
  public void setInitMethod(@Nullable String initMethod) {
    this.initMethod = initMethod;
  }

  /**
   * Return the default init-method setting for the document that's currently parsed.
   */
  @Nullable
  public String getInitMethod() {
    return this.initMethod;
  }

  /**
   * Set the default destroy-method setting for the document that's currently parsed.
   */
  public void setDestroyMethod(@Nullable String destroyMethod) {
    this.destroyMethod = destroyMethod;
  }

  /**
   * Return the default destroy-method setting for the document that's currently parsed.
   */
  @Nullable
  public String getDestroyMethod() {
    return this.destroyMethod;
  }

  /**
   * Set the configuration source {@code Object} for this metadata element.
   * <p>The exact type of the object will depend on the configuration mechanism used.
   */
  public void setSource(@Nullable Object source) {
    this.source = source;
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.source;
  }

}
