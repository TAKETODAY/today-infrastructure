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

package infra.app.test.json;

import org.assertj.core.api.AssertProvider;

import infra.core.ResolvableType;
import infra.lang.Assert;

/**
 * Object content usually created from {@link AbstractJsonMarshalTester}. Generally used
 * only to {@link AssertProvider provide} {@link ObjectContentAssert} to AssertJ
 * {@code assertThat} calls.
 *
 * @param <T> the content type
 * @author Phillip Webb
 * @since 4.0
 */
public final class ObjectContent<T> implements AssertProvider<ObjectContentAssert<T>> {

  private final ResolvableType type;

  private final T object;

  /**
   * Create a new {@link ObjectContent} instance.
   *
   * @param type the type under test (or {@code null} if not known)
   * @param object the actual object content
   */
  public ObjectContent(ResolvableType type, T object) {
    Assert.notNull(object, "Object is required");
    this.type = type;
    this.object = object;
  }

  @Override
  public ObjectContentAssert<T> assertThat() {
    return new ObjectContentAssert<>(this.object);
  }

  /**
   * Return the actual object content.
   *
   * @return the object content
   */
  public T getObject() {
    return this.object;
  }

  @Override
  public String toString() {
    String createdFrom = (this.type != null) ? " created from " + this.type : "";
    return "ObjectContent " + this.object + createdFrom;
  }

}
