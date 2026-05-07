/*
 * Copyright 2012-present the original author or authors.
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

package infra.testcontainers.service.connection;

import org.testcontainers.containers.Container;

import infra.context.annotation.Import;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.AutoConfigureOrder;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.core.Ordered;

/**
 * {@link EnableAutoConfiguration
 * Auto-configuration} for {@link ServiceConnection @ServiceConnection} annotated
 * {@link Container} beans.
 *
 * @author Phillip Webb
 * @since 5.0
 */
@AutoConfiguration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Import(ServiceConnectionAutoConfigurationRegistrar.class)
public final class ServiceConnectionAutoConfiguration {

}
