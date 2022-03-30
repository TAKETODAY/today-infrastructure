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

import org.assertj.core.api.AbstractMapAssert;
import org.assertj.core.api.AbstractObjectArrayAssert;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assert;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.Objects;

import java.util.Map;

/**
 * AssertJ {@link Assert} for {@link ObjectContent}.
 *
 * @param <A> the actual type
 * @author Phillip Webb
 * @since 4.0
 */
public class ObjectContentAssert<A> extends AbstractObjectAssert<ObjectContentAssert<A>, A> {

  protected ObjectContentAssert(A actual) {
    super(actual, ObjectContentAssert.class);
  }

  /**
   * Verifies that the actual value is an array, and returns an array assertion, to
   * allow chaining of array-specific assertions from this call.
   *
   * @return an array assertion object
   */
  public AbstractObjectArrayAssert<?, Object> asArray() {
    Objects.instance().assertIsInstanceOf(this.info, this.actual, Object[].class);
    return Assertions.assertThat((Object[]) this.actual);
  }

  /**
   * Verifies that the actual value is a map, and returns a map assertion, to allow
   * chaining of map-specific assertions from this call.
   *
   * @return a map assertion object
   */
  @SuppressWarnings("unchecked")
  public AbstractMapAssert<?, ?, Object, Object> asMap() {
    Objects.instance().assertIsInstanceOf(this.info, this.actual, Map.class);
    return Assertions.assertThat((Map<Object, Object>) this.actual);
  }

}
