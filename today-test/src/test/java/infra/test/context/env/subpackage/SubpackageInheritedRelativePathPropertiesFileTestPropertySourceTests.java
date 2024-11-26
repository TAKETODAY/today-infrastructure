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

package infra.test.context.env.subpackage;

import infra.test.context.TestPropertySource;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.env.ExplicitPropertiesFileInClasspathTestPropertySourceTests;

/**
 * Integration tests for {@link TestPropertySource @TestPropertySource}
 * support with an inherited explicitly named properties file that is
 * referenced using a relative path within a parent package.
 *
 * @author Sam Brannen
 * @since 4.0
 */
// Since ExplicitPropertiesFileTestPropertySourceTests is disabled in AOT mode, this class must be also.
@DisabledInAotMode
class SubpackageInheritedRelativePathPropertiesFileTestPropertySourceTests extends
        ExplicitPropertiesFileInClasspathTestPropertySourceTests {

  /* all tests are in superclass */

}
