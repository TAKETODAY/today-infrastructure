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

package cn.taketoday.mock.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.mock.SessionCookieConfig;

/**
 * Mock implementation of the {@link SessionCookieConfig} interface.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.web.mock.ServletContext#getSessionCookieConfig()
 * @since 4.0
 */
public class MockSessionCookieConfig implements SessionCookieConfig {

  @Nullable
  private String name;

  @Nullable
  private String domain;

  @Nullable
  private String path;

  @Nullable
  private String comment;

  private boolean httpOnly;

  private boolean secure;

  private int maxAge = -1;

  private Map<String, String> attributes = new LinkedHashMap<>();

  @Override
  public void setName(@Nullable String name) {
    this.name = name;
  }

  @Override
  @Nullable
  public String getName() {
    return this.name;
  }

  @Override
  public void setDomain(@Nullable String domain) {
    this.domain = domain;
  }

  @Override
  @Nullable
  public String getDomain() {
    return this.domain;
  }

  @Override
  public void setPath(@Nullable String path) {
    this.path = path;
  }

  @Override
  @Nullable
  public String getPath() {
    return this.path;
  }

  @SuppressWarnings("removal")
  @Override
  public void setComment(@Nullable String comment) {
    this.comment = comment;
  }

  @SuppressWarnings("removal")
  @Override
  @Nullable
  public String getComment() {
    return this.comment;
  }

  @Override
  public void setHttpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
  }

  @Override
  public boolean isHttpOnly() {
    return this.httpOnly;
  }

  @Override
  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  @Override
  public boolean isSecure() {
    return this.secure;
  }

  @Override
  public void setMaxAge(int maxAge) {
    this.maxAge = maxAge;
  }

  @Override
  public int getMaxAge() {
    return this.maxAge;
  }

  @Override
  public void setAttribute(String name, String value) {
    this.attributes.put(name, value);
  }

  @Override
  public String getAttribute(String name) {
    return this.attributes.get(name);
  }

  @Override
  public Map<String, String> getAttributes() {
    return Collections.unmodifiableMap(this.attributes);
  }

}
