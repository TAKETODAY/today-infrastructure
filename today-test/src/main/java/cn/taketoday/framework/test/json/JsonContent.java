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

package cn.taketoday.framework.test.json;

import com.jayway.jsonpath.Configuration;

import org.assertj.core.api.AssertProvider;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;

/**
 * JSON content usually created from a JSON tester. Generally used only to
 * {@link AssertProvider provide} {@link JsonContentAssert} to AssertJ {@code assertThat}
 * calls.
 *
 * @param <T> the source type that created the content
 * @author Phillip Webb
 * @author Diego Berrueta
 * @since 4.0
 */
public final class JsonContent<T> implements AssertProvider<JsonContentAssert> {

  private final Class<?> resourceLoadClass;

  private final ResolvableType type;

  private final String json;

  private final Configuration configuration;

  /**
   * Create a new {@link JsonContent} instance.
   *
   * @param resourceLoadClass the source class used to load resources
   * @param type the type under test (or {@code null} if not known)
   * @param json the actual JSON content
   */
  public JsonContent(Class<?> resourceLoadClass, ResolvableType type, String json) {
    this(resourceLoadClass, type, json, Configuration.defaultConfiguration());
  }

  /**
   * Create a new {@link JsonContent} instance.
   *
   * @param resourceLoadClass the source class used to load resources
   * @param type the type under test (or {@code null} if not known)
   * @param json the actual JSON content
   * @param configuration the JsonPath configuration
   */
  JsonContent(Class<?> resourceLoadClass, ResolvableType type, String json, Configuration configuration) {
    Assert.notNull(resourceLoadClass, "ResourceLoadClass is required");
    Assert.notNull(json, "JSON is required");
    Assert.notNull(configuration, "Configuration is required");
    this.resourceLoadClass = resourceLoadClass;
    this.type = type;
    this.json = json;
    this.configuration = configuration;
  }

  /**
   * Use AssertJ's {@link org.assertj.core.api.Assertions#assertThat assertThat}
   * instead.
   *
   * @deprecated to prevent accidental use. Prefer standard AssertJ
   * {@code assertThat(context)...} calls instead.
   */
  @Override
  @Deprecated
  public JsonContentAssert assertThat() {
    return new JsonContentAssert(this.resourceLoadClass, null, this.json, this.configuration);
  }

  /**
   * Return the actual JSON content string.
   *
   * @return the JSON content
   */
  public String getJson() {
    return this.json;
  }

  @Override
  public String toString() {
    String createdFrom = (this.type != null) ? " created from " + this.type : "";
    return "JsonContent " + this.json + createdFrom;
  }

}
