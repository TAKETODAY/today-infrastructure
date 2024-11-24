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

package infra.aop.config;

import infra.beans.factory.parsing.ParseState;
import infra.util.StringUtils;

/**
 * {@link ParseState} entry representing an aspect.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 4.0
 */
public class AspectEntry implements ParseState.Entry {

  private final String id;

  private final String ref;

  /**
   * Create a new {@code AspectEntry} instance.
   *
   * @param id the id of the aspect element
   * @param ref the bean name referenced by this aspect element
   */
  public AspectEntry(String id, String ref) {
    this.id = id;
    this.ref = ref;
  }

  @Override
  public String toString() {
    return "Aspect: " + (StringUtils.isNotEmpty(this.id) ? "id='" + this.id + "'" : "ref='" + this.ref + "'");
  }

}
