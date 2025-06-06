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

package infra.app.test.context.runner;

import infra.app.test.context.assertj.AssertableReactiveWebApplicationContext;
import infra.web.server.reactive.context.ConfigurableReactiveWebApplicationContext;

/**
 * Tests for {@link ReactiveWebApplicationContextRunner}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ReactiveWebApplicationContextRunnerTests extends
        AbstractApplicationContextRunnerTests<ReactiveWebApplicationContextRunner, ConfigurableReactiveWebApplicationContext, AssertableReactiveWebApplicationContext> {

  @Override
  protected ReactiveWebApplicationContextRunner get() {
    return new ReactiveWebApplicationContextRunner();
  }

}
