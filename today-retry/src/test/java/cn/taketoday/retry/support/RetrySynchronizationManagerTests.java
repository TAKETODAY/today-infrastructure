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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.context.RetryContextSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class RetrySynchronizationManagerTests {

  RetryTemplate template = new RetryTemplate();

  @BeforeEach
  public void setUp() {
    RetrySynchronizationManagerTests.clearAll();
    RetryContext status = RetrySynchronizationManager.getContext();
    assertThat(status).isNull();
  }

  @Test
  public void testStatusIsStoredByTemplate() {

    RetryContext status = RetrySynchronizationManager.getContext();
    assertThat(status).isNull();

    this.template.execute(retryContext -> {
      RetryContext global = RetrySynchronizationManager.getContext();
      assertThat(retryContext).isNotNull();
      assertThat(retryContext).isEqualTo(global);
      return null;
    });

    status = RetrySynchronizationManager.getContext();
    assertThat(status).isNull();
  }

  @Test
  public void testStatusRegistration() {
    RetryContext status = new RetryContextSupport(null);
    RetryContext value = RetrySynchronizationManager.register(status);
    assertThat(value).isNull();
    value = RetrySynchronizationManager.register(status);
    assertThat(value).isEqualTo(status);
  }

  @Test
  public void testClear() {
    RetryContext status = new RetryContextSupport(null);
    RetryContext value = RetrySynchronizationManager.register(status);
    assertThat(value).isNull();
    RetrySynchronizationManager.clear();
    value = RetrySynchronizationManager.register(status);
    assertThat(value).isNull();
  }

  @Test
  public void testParent() {
    RetryContext parent = new RetryContextSupport(null);
    RetryContext child = new RetryContextSupport(parent);
    assertThat(child.getParent()).isSameAs(parent);
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
