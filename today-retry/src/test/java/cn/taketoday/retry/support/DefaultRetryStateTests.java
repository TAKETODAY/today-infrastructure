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
package cn.taketoday.retry.support;

import org.junit.Test;

import cn.taketoday.classify.Classifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class DefaultRetryStateTests {

  /**
   * Test method for
   * {@link DefaultRetryState#DefaultRetryState(Object, boolean, Classifier)}.
   */
  @SuppressWarnings("serial")
  @Test
  public void testDefaultRetryStateObjectBooleanClassifierOfQsuperThrowableBoolean() {
    DefaultRetryState state = new DefaultRetryState("foo", true, new Classifier<Throwable, Boolean>() {
      public Boolean classify(Throwable classifiable) {
        return false;
      }
    });
    assertEquals("foo", state.getKey());
    assertTrue(state.isForceRefresh());
    assertFalse(state.rollbackFor(null));
  }

  /**
   * Test method for
   * {@link DefaultRetryState#DefaultRetryState(Object, Classifier)}.
   */
  @SuppressWarnings("serial")
  @Test
  public void testDefaultRetryStateObjectClassifierOfQsuperThrowableBoolean() {
    DefaultRetryState state = new DefaultRetryState("foo", new Classifier<Throwable, Boolean>() {
      public Boolean classify(Throwable classifiable) {
        return false;
      }
    });
    assertEquals("foo", state.getKey());
    assertFalse(state.isForceRefresh());
    assertFalse(state.rollbackFor(null));
  }

  /**
   * Test method for
   * {@link DefaultRetryState#DefaultRetryState(Object, boolean)}.
   */
  @Test
  public void testDefaultRetryStateObjectBoolean() {
    DefaultRetryState state = new DefaultRetryState("foo", true);
    assertEquals("foo", state.getKey());
    assertTrue(state.isForceRefresh());
    assertTrue(state.rollbackFor(null));
  }

  /**
   * Test method for
   * {@link DefaultRetryState#DefaultRetryState(Object)}.
   */
  @Test
  public void testDefaultRetryStateObject() {
    DefaultRetryState state = new DefaultRetryState("foo");
    assertEquals("foo", state.getKey());
    assertFalse(state.isForceRefresh());
    assertTrue(state.rollbackFor(null));
  }

}
