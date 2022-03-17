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

package cn.taketoday.web.view;

import java.util.Locale;

import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Nullable;

/**
 * Abstract base class for URL-based views. Provides a consistent way of
 * holding the URL that a View wraps, in the form of a "url" bean property.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractUrlBasedView extends AbstractView implements InitializingBean {

  @Nullable
  private String url;

  /**
   * Constructor for use as a bean.
   */
  protected AbstractUrlBasedView() { }

  /**
   * Create a new AbstractUrlBasedView with the given URL.
   *
   * @param url the URL to forward to
   */
  protected AbstractUrlBasedView(String url) {
    this.url = url;
  }

  /**
   * Set the URL of the resource that this view wraps.
   * The URL must be appropriate for the concrete View implementation.
   */
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  /**
   * Return the URL of the resource that this view wraps.
   */
  @Nullable
  public String getUrl() {
    return this.url;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (isUrlRequired() && getUrl() == null) {
      throw new IllegalArgumentException("Property 'url' is required");
    }
  }

  /**
   * Return whether the 'url' property is required.
   * <p>The default implementation returns {@code true}.
   * This can be overridden in subclasses.
   */
  protected boolean isUrlRequired() {
    return true;
  }

  /**
   * Check whether the underlying resource that the configured URL points to
   * actually exists.
   *
   * @param locale the desired Locale that we're looking for
   * @return {@code true} if the resource exists (or is assumed to exist);
   * {@code false} if we know that it does not exist
   * @throws Exception if the resource exists but is invalid (e.g. could not be parsed)
   */
  public boolean checkResource(Locale locale) throws Exception {
    return true;
  }

  @Override
  public String toString() {
    return super.toString() + "; URL [" + getUrl() + "]";
  }

}
