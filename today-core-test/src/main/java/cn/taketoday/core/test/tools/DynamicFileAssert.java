/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core.test.tools;

import org.assertj.core.api.AbstractAssert;

import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertion methods for {@code DynamicFile} instances.
 *
 * @param <A> the assertion type
 * @param <F> the file type
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0
 */
public class DynamicFileAssert<A extends DynamicFileAssert<A, F>, F extends DynamicFile>
        extends AbstractAssert<A, F> {

  DynamicFileAssert(F actual, Class<?> selfType) {
    super(actual, selfType);
  }

  /**
   * Verify that the actual content is equal to the given one.
   *
   * @param content the expected content of the file
   * @return {@code this}, to facilitate method chaining
   */
  public A hasContent(@Nullable CharSequence content) {
    assertThat(this.actual.getContent()).isEqualTo(
            content != null ? content.toString() : null);
    return this.myself;
  }

  /**
   * Verify that the actual content contains all the given values.
   *
   * @param values the values to look for
   * @return {@code this}, to facilitate method chaining
   */
  public A contains(CharSequence... values) {
    assertThat(this.actual.getContent()).contains(values);
    return this.myself;
  }

  /**
   * Verify that the actual content does not contain any of the given values.
   *
   * @param values the values to look for
   * @return {@code this}, to facilitate method chaining
   */
  public A doesNotContain(CharSequence... values) {
    assertThat(this.actual.getContent()).doesNotContain(values);
    return this.myself;
  }

}
