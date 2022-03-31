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

package cn.taketoday.test.context.junit4.annotation;

import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.SpringJUnit4ClassRunnerAppCtxTests;

/**
 * Integration tests that verify support for configuration classes in
 * the Spring TestContext Framework.
 *
 * <p>Furthermore, by extending {@link SpringJUnit4ClassRunnerAppCtxTests},
 * this class also verifies support for several basic features of the
 * Spring TestContext Framework. See JavaDoc in
 * {@code SpringJUnit4ClassRunnerAppCtxTests} for details.
 *
 * <p>Configuration will be loaded from {@link PojoAndStringConfig}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration(classes = PojoAndStringConfig.class, inheritLocations = false)
public class AnnotationConfigSpringJUnit4ClassRunnerAppCtxTests extends SpringJUnit4ClassRunnerAppCtxTests {
	/* all tests are in the parent class. */
}
