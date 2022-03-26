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

import org.junit.Before;
import org.junit.Test;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.context.RetryContextSupport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dave Syer
 */
public class RetrySynchronizationManagerTests {

  RetryTemplate template = new RetryTemplate();

  @Before
  public void setUp() throws Exception {
    RetrySynchronizationManagerTests.clearAll();
    RetryContext status = RetrySynchronizationManager.getContext();
    assertNull(status);
  }

  @Test
  public void testStatusIsStoredByTemplate() throws Throwable {

    RetryContext status = RetrySynchronizationManager.getContext();
    assertNull(status);

    this.template.execute(new RetryCallback<Object, Exception>() {
      @Override
      public Object doWithRetry(RetryContext status) throws Exception {
        RetryContext global = RetrySynchronizationManager.getContext();
        assertNotNull(status);
        assertEquals(global, status);
        return null;
      }
    });

    status = RetrySynchronizationManager.getContext();
    assertNull(status);
  }

  @Test
  public void testStatusRegistration() throws Exception {
    RetryContext status = new RetryContextSupport(null);
    RetryContext value = RetrySynchronizationManager.register(status);
    assertNull(value);
    value = RetrySynchronizationManager.register(status);
    assertEquals(status, value);
  }

  @Test
  public void testClear() throws Exception {
    RetryContext status = new RetryContextSupport(null);
    RetryContext value = RetrySynchronizationManager.register(status);
    assertNull(value);
    RetrySynchronizationManager.clear();
    value = RetrySynchronizationManager.register(status);
    assertNull(value);
  }

  @Test
  public void testParent() throws Exception {
    RetryContext parent = new RetryContextSupport(null);
    RetryContext child = new RetryContextSupport(parent);
    assertSame(parent, child.getParent());
  }

  /**
   * Clear all contexts starting with the current one and continuing until
   * {@link RetrySynchronizationManager#clear()} returns null.
   *
   * @return a retry context
   */
  public static RetryContext clearAll() {
    RetryContext result = null;
    RetryContext context = RetrySynchronizationManager.clear();
    while (context != null) {
      result = context;
      context = RetrySynchronizationManager.clear();
    }
    return result;
  }

}
