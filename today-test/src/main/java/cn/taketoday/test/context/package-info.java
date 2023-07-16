/*
 * Copyright 2017 - 2023 the original author or authors.
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

/**
 * This package contains the <em>TestContext Framework</em> which
 * provides annotation-driven unit and integration testing support that is
 * agnostic of the actual testing framework in use. The same techniques and
 * annotation-based configuration used in, for example, a JUnit environment
 * can also be applied to tests written with TestNG, etc.
 *
 * <p>In addition to providing generic and extensible testing infrastructure,
 * the TestContext Framework provides out-of-the-box support for
 * Infra-specific integration testing functionality such as context management
 * and caching, dependency injection of test fixtures, and transactional test
 * management with default rollback semantics.
 */
@NonNullApi
@NonNullFields
package cn.taketoday.test.context;

import cn.taketoday.lang.NonNullApi;
import cn.taketoday.lang.NonNullFields;
