/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.annotation.config.web.client;

import infra.annotation.ConditionalOnWebApplication;
import infra.context.condition.InfraCondition;
import infra.context.condition.NoneNestedConditions;

/**
 * {@link InfraCondition} that applies only when running in a non-reactive web
 * application.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class NotReactiveWebApplicationCondition extends NoneNestedConditions {

  NotReactiveWebApplicationCondition() {
    super(ConfigurationPhase.PARSE_CONFIGURATION);
  }

  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
  private static final class ReactiveWebApplication {

  }

}
