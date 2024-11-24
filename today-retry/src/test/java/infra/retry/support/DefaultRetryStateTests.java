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
package infra.retry.support;

import org.junit.jupiter.api.Test;

import infra.classify.Classifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class DefaultRetryStateTests {

  /**
   * Test method for
   * {@link DefaultRetryState#DefaultRetryState(Object, boolean, Classifier)}.
   */
  @SuppressWarnings("serial")
  @Test
  public void testDefaultRetryStateObjectBooleanClassifierOfQsuperThrowableBoolean() {
    DefaultRetryState state = new DefaultRetryState("foo", true, classifiable -> false);
    assertThat(state.getKey()).isEqualTo("foo");
    assertThat(state.isForceRefresh()).isTrue();
    assertThat(state.rollbackFor(null)).isFalse();
  }

  /**
   * Test method for
   * {@link DefaultRetryState#DefaultRetryState(Object, Classifier)}.
   */
  @SuppressWarnings("serial")
  @Test
  public void testDefaultRetryStateObjectClassifierOfQsuperThrowableBoolean() {
    DefaultRetryState state = new DefaultRetryState("foo", classifiable -> false);
    assertThat(state.getKey()).isEqualTo("foo");
    assertThat(state.isForceRefresh()).isFalse();
    assertThat(state.rollbackFor(null)).isFalse();
  }

  /**
   * Test method for
   * {@link DefaultRetryState#DefaultRetryState(Object, boolean)}.
   */
  @Test
  public void testDefaultRetryStateObjectBoolean() {
    DefaultRetryState state = new DefaultRetryState("foo", true);
    assertThat(state.getKey()).isEqualTo("foo");
    assertThat(state.isForceRefresh()).isTrue();
    assertThat(state.rollbackFor(null)).isTrue();
  }

  /**
   * Test method for
   * {@link DefaultRetryState#DefaultRetryState(Object)}.
   */
  @Test
  public void testDefaultRetryStateObject() {
    DefaultRetryState state = new DefaultRetryState("foo");
    assertThat(state.getKey()).isEqualTo("foo");
    assertThat(state.isForceRefresh()).isFalse();
    assertThat(state.rollbackFor(null)).isTrue();
  }

}
