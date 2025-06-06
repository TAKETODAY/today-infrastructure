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

package infra.test.context.support;

import infra.core.Ordered;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;

/**
 * Abstract {@linkplain Ordered ordered} implementation of the
 * {@link TestExecutionListener} API.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @see #getOrder()
 * @since 4.0
 */
public abstract class AbstractTestExecutionListener implements TestExecutionListener, Ordered {

  /**
   * The default implementation returns {@link Ordered#LOWEST_PRECEDENCE},
   * thereby ensuring that custom listeners are ordered after default
   * listeners supplied by the framework. Can be overridden by subclasses
   * as necessary.
   */
  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  /**
   * The default implementation is <em>empty</em>. Can be overridden by
   * subclasses as necessary.
   */
  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    /* no-op */
  }

  /**
   * The default implementation is <em>empty</em>. Can be overridden by
   * subclasses as necessary.
   */
  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {
    /* no-op */
  }

  /**
   * The default implementation is <em>empty</em>. Can be overridden by
   * subclasses as necessary.
   */
  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    /* no-op */
  }

  /**
   * The default implementation is <em>empty</em>. Can be overridden by
   * subclasses as necessary.
   */
  @Override
  public void beforeTestExecution(TestContext testContext) throws Exception {
    /* no-op */
  }

  /**
   * The default implementation is <em>empty</em>. Can be overridden by
   * subclasses as necessary.
   */
  @Override
  public void afterTestExecution(TestContext testContext) throws Exception {
    /* no-op */
  }

  /**
   * The default implementation is <em>empty</em>. Can be overridden by
   * subclasses as necessary.
   */
  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    /* no-op */
  }

  /**
   * The default implementation is <em>empty</em>. Can be overridden by
   * subclasses as necessary.
   */
  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    /* no-op */
  }

}
