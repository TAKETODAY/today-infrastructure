/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.testfixture.security;

import java.security.Principal;

import org.jspecify.annotations.Nullable;

/**
 * An implementation of {@link Principal} for testing.
 *
 * @author Rossen Stoyanchev
 */
public class TestPrincipal implements Principal {

  private final String name;

  public TestPrincipal(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof TestPrincipal p)) {
      return false;
    }
    return this.name.equals(p.name);
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }

}
