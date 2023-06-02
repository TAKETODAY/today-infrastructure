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

package cn.taketoday.test.context.junit4;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import cn.taketoday.test.context.junit4.annotation.AnnotationConfigJUnit4ClassRunnerAppCtxTests;
import cn.taketoday.test.context.junit4.annotation.BeanOverridingDefaultConfigClassesInheritedTests;
import cn.taketoday.test.context.junit4.annotation.BeanOverridingExplicitConfigClassesInheritedTests;
import cn.taketoday.test.context.junit4.annotation.DefaultConfigClassesBaseTests;
import cn.taketoday.test.context.junit4.annotation.DefaultConfigClassesInheritedTests;
import cn.taketoday.test.context.junit4.annotation.DefaultLoaderBeanOverridingDefaultConfigClassesInheritedTests;
import cn.taketoday.test.context.junit4.annotation.DefaultLoaderBeanOverridingExplicitConfigClassesInheritedTests;
import cn.taketoday.test.context.junit4.annotation.DefaultLoaderDefaultConfigClassesBaseTests;
import cn.taketoday.test.context.junit4.annotation.DefaultLoaderDefaultConfigClassesInheritedTests;
import cn.taketoday.test.context.junit4.annotation.DefaultLoaderExplicitConfigClassesBaseTests;
import cn.taketoday.test.context.junit4.annotation.DefaultLoaderExplicitConfigClassesInheritedTests;
import cn.taketoday.test.context.junit4.annotation.ExplicitConfigClassesBaseTests;
import cn.taketoday.test.context.junit4.annotation.ExplicitConfigClassesInheritedTests;
import cn.taketoday.test.context.junit4.orm.HibernateSessionFlushingTests;
import cn.taketoday.test.context.junit4.profile.annotation.DefaultProfileAnnotationConfigTests;
import cn.taketoday.test.context.junit4.profile.annotation.DevProfileAnnotationConfigTests;
import cn.taketoday.test.context.junit4.profile.annotation.DevProfileResolverAnnotationConfigTests;
import cn.taketoday.test.context.junit4.profile.xml.DefaultProfileXmlConfigTests;
import cn.taketoday.test.context.junit4.profile.xml.DevProfileResolverXmlConfigTests;
import cn.taketoday.test.context.junit4.profile.xml.DevProfileXmlConfigTests;

/**
 * JUnit test suite for tests involving {@link InfraRunner} and the
 * <em>TestContext Framework</em>; only intended to be run manually as a
 * convenience.
 *
 * <p>This test suite serves a dual purpose of verifying that tests run with
 * {@link InfraRunner} can be used in conjunction with JUnit's
 * {@link Suite} runner.
 *
 * <p>Note that tests included in this suite will be executed at least twice if
 * run from an automated build process, test runner, etc. that is not configured
 * to exclude tests based on a {@code "*TestSuite.class"} pattern match.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(Suite.class)
// Note: the following 'multi-line' layout is for enhanced code readability.
@SuiteClasses({//
        StandardJUnit4FeaturesTests.class,//
        StandardJUnit4FeaturesSpringRunnerTests.class,//
        SpringJUnit47ClassRunnerRuleTests.class,//
        AnnotationConfigJUnit4ClassRunnerAppCtxTests.class,//
        DefaultConfigClassesBaseTests.class,//
        DefaultConfigClassesInheritedTests.class,//
        BeanOverridingDefaultConfigClassesInheritedTests.class,//
        ExplicitConfigClassesBaseTests.class,//
        ExplicitConfigClassesInheritedTests.class,//
        BeanOverridingExplicitConfigClassesInheritedTests.class,//
        DefaultLoaderDefaultConfigClassesBaseTests.class,//
        DefaultLoaderDefaultConfigClassesInheritedTests.class,//
        DefaultLoaderBeanOverridingDefaultConfigClassesInheritedTests.class,//
        DefaultLoaderExplicitConfigClassesBaseTests.class,//
        DefaultLoaderExplicitConfigClassesInheritedTests.class,//
        DefaultLoaderBeanOverridingExplicitConfigClassesInheritedTests.class,//
        DefaultProfileAnnotationConfigTests.class,//
        DevProfileAnnotationConfigTests.class,//
        DevProfileResolverAnnotationConfigTests.class,//
        DefaultProfileXmlConfigTests.class,//
        DevProfileXmlConfigTests.class,//
        DevProfileResolverXmlConfigTests.class,//
        ExpectedExceptionSpringRunnerTests.class,//
        TimedSpringRunnerTests.class,//
        RepeatedSpringRunnerTests.class,//
        EnabledAndIgnoredSpringRunnerTests.class,//
        HardCodedProfileValueSourceSpringRunnerTests.class,//
        JUnit4ClassRunnerAppCtxTests.class,//
        ClassPathResourceJUnit4ClassRunnerAppCtxTests.class,//
        AbsolutePathJUnit4ClassRunnerAppCtxTests.class,//
        RelativePathJUnit4ClassRunnerAppCtxTests.class,//
        MultipleResourcesJUnit4ClassRunnerAppCtxTests.class,//
        InheritedConfigJUnit4ClassRunnerAppCtxTests.class,//
        PropertiesBasedSpringJUnit4ClassRunnerAppCtxTests.class,//
        CustomDefaultContextLoaderClassSpringRunnerTests.class,//
        ParameterizedDependencyInjectionTests.class,//
        ConcreteTransactionalJUnit4ContextTests.class,//
        ClassLevelTransactionalSpringRunnerTests.class,//
        MethodLevelTransactionalSpringRunnerTests.class,//
        DefaultRollbackTrueRollbackAnnotationTransactionalTests.class,//
        DefaultRollbackFalseRollbackAnnotationTransactionalTests.class,//
        RollbackOverrideDefaultRollbackTrueTransactionalTests.class,//
        RollbackOverrideDefaultRollbackFalseTransactionalTests.class,//
        BeforeAndAfterTransactionAnnotationTests.class,//
        TimedTransactionalRunnerTests.class,//
        HibernateSessionFlushingTests.class //
})
public class SpringJUnit4TestSuite {
  /* this test case consists entirely of tests loaded as a suite. */
}
