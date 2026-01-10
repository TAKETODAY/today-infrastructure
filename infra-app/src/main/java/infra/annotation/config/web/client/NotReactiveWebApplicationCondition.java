/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
