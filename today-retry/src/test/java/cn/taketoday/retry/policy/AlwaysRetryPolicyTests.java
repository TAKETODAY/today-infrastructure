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

package cn.taketoday.retry.policy;

import org.junit.jupiter.api.Test;

import cn.taketoday.retry.RetryContext;

import static org.assertj.core.api.Assertions.assertThat;

public class AlwaysRetryPolicyTests {

  @Test
  public void testSimpleOperations() {
    AlwaysRetryPolicy policy = new AlwaysRetryPolicy();
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    assertThat(policy.canRetry(context)).isTrue();
    policy.registerThrowable(context, null);
    assertThat(policy.canRetry(context)).isTrue();
    policy.close(context);
    assertThat(policy.canRetry(context)).isTrue();
  }

  @Test
  public void testRetryCount() {
    AlwaysRetryPolicy policy = new AlwaysRetryPolicy();
    RetryContext context = policy.open(null);
    assertThat(context).isNotNull();
    policy.registerThrowable(context, null);
    assertThat(context.getRetryCount()).isEqualTo(0);
    policy.registerThrowable(context, new RuntimeException("foo"));
    assertThat(context.getRetryCount()).isEqualTo(1);
    assertThat(context.getLastThrowable().getMessage()).isEqualTo("foo");
  }

  @Test
  public void testParent() {
    AlwaysRetryPolicy policy = new AlwaysRetryPolicy();
    RetryContext context = policy.open(null);
    RetryContext child = policy.open(context);
    assertThat(context).isNotSameAs(child);
    assertThat(child.getParent()).isSameAs(context);
  }

}
