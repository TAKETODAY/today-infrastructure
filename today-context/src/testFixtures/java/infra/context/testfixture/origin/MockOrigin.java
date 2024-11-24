/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.context.testfixture.origin;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.origin.Origin;

/**
 * Mock {@link Origin} implementation used for testing.
 *
 * @author Phillip Webb
 */
public final class MockOrigin implements Origin {

  private final String value;
  @Nullable
  private final Origin parent;

  private MockOrigin(String value, @Nullable Origin parent) {
    Assert.notNull(value, "Value is required");
    this.value = value;
    this.parent = parent;
  }

  @Nullable
  @Override
  public Origin getParent() {
    return this.parent;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.value.equals(((MockOrigin) obj).value);
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public String toString() {
    return this.value;
  }

  @Nullable
  public static Origin of(@Nullable String value) {
    return of(value, null);
  }

  @Nullable
  public static Origin of(@Nullable String value, @Nullable Origin parent) {
    return (value != null) ? new MockOrigin(value, parent) : null;
  }

}
