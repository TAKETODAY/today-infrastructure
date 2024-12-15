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

package infra.test.context.junit4.spr8849;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Test suite to investigate claims raised in
 *
 *
 * <h3>Work Around</h3>
 * <p>By using a SpEL expression to generate a random {@code database-name}
 * for the embedded database (see {@code datasource-config.xml}), we ensure
 * that each {@code ApplicationContext} that imports the common configuration
 * will create an embedded database with a unique name.
 *
 * <h3>Solution</h3>
 * <p>As of Spring 4.2, a proper solution is possible thanks to SPR-8849.
 * {@link TestClass3} and {@link TestClass4} both import
 * {@code datasource-config-with-auto-generated-db-name.xml} which makes
 * use of the new {@code generate-name} attribute of {@code <jdbc:embedded-database>}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@SuppressWarnings("javadoc")
@RunWith(Suite.class)
@SuiteClasses({ TestClass1.class, TestClass2.class, TestClass3.class, TestClass4.class })
public class Spr8849Tests {

}
