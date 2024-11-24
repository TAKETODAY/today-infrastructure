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

package infra.app.test.mock.mockito;

import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.app.test.mock.mockito.example.ExampleServiceCaller;
import infra.app.test.mock.mockito.example.FailingExampleService;

/**
 * Config for {@link MockBeanOnTestFieldForExistingBeanIntegrationTests} and
 * {@link MockBeanOnTestFieldForExistingBeanCacheIntegrationTests}. Extracted to a shared
 * config to trigger caching.
 *
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@Import({ ExampleServiceCaller.class, FailingExampleService.class })
public class MockBeanOnTestFieldForExistingBeanConfig {

}
