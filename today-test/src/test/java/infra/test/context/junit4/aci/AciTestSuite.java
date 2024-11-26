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

package infra.test.context.junit4.aci;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import infra.context.ApplicationContextInitializer;
import infra.test.context.junit4.aci.annotation.InitializerWithoutConfigFilesOrClassesTests;
import infra.test.context.junit4.aci.annotation.MergedInitializersAnnotationConfigTests;
import infra.test.context.junit4.aci.annotation.MultipleInitializersAnnotationConfigTests;
import infra.test.context.junit4.aci.annotation.OrderedInitializersAnnotationConfigTests;
import infra.test.context.junit4.aci.annotation.OverriddenInitializersAnnotationConfigTests;
import infra.test.context.junit4.aci.annotation.SingleInitializerAnnotationConfigTests;
import infra.test.context.junit4.aci.xml.MultipleInitializersXmlConfigTests;

/**
 * Convenience test suite for integration tests that verify support for
 * {@link ApplicationContextInitializer ApplicationContextInitializers} (ACIs)
 * in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(Suite.class)
// Note: the following 'multi-line' layout is for enhanced code readability.
@SuiteClasses({//
        MultipleInitializersXmlConfigTests.class,//
        SingleInitializerAnnotationConfigTests.class,//
        MultipleInitializersAnnotationConfigTests.class,//
        MergedInitializersAnnotationConfigTests.class,//
        OverriddenInitializersAnnotationConfigTests.class,//
        OrderedInitializersAnnotationConfigTests.class,//
        InitializerWithoutConfigFilesOrClassesTests.class //
})
public class AciTestSuite {
}
