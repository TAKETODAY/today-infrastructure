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

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestContextManager;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.TestExecutionListeners;
import cn.taketoday.test.context.event.ApplicationEventsTestExecutionListener;
import cn.taketoday.test.context.event.EventPublishingTestExecutionListener;
import cn.taketoday.test.context.junit4.rules.ApplicationClassRule;
import cn.taketoday.test.context.junit4.rules.ApplicationMethodRule;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import cn.taketoday.test.context.support.DirtiesContextTestExecutionListener;
import cn.taketoday.test.context.testng.AbstractTestNGContextTests;
import cn.taketoday.test.context.web.ServletTestExecutionListener;

/**
 * Abstract base test class which integrates the <em>Spring TestContext
 * Framework</em> with explicit {@link ApplicationContext} testing support
 * in a <strong>JUnit 4</strong> environment.
 *
 * <p>Concrete subclasses should typically declare a class-level
 * {@link ContextConfiguration @ContextConfiguration} annotation to
 * configure the {@linkplain ApplicationContext application context} {@linkplain
 * ContextConfiguration#locations() resource locations} or {@linkplain
 * ContextConfiguration#classes() component classes}. <em>If your test does not
 * need to load an application context, you may choose to omit the
 * {@link ContextConfiguration @ContextConfiguration} declaration and to configure
 * the appropriate {@link TestExecutionListener
 * TestExecutionListeners} manually.</em>
 *
 * <p>The following {@link TestExecutionListener
 * TestExecutionListeners} are configured by default:
 *
 * <ul>
 * <li>{@link ServletTestExecutionListener}
 * <li>{@link DirtiesContextBeforeModesTestExecutionListener}
 * <li>{@link ApplicationEventsTestExecutionListener}
 * <li>{@link DependencyInjectionTestExecutionListener}
 * <li>{@link DirtiesContextTestExecutionListener}
 * <li>{@link EventPublishingTestExecutionListener}
 * </ul>
 *
 * <p>This class serves only as a convenience for extension.
 * <ul>
 * <li>If you do not wish for your test classes to be tied to a Spring-specific
 * class hierarchy, you may configure your own custom test classes by using
 * {@link InfraRunner}, {@link ContextConfiguration @ContextConfiguration},
 * {@link TestExecutionListeners @TestExecutionListeners}, etc.</li>
 * <li>If you wish to extend this class and use a runner other than the
 * {@link InfraRunner}, you can use
 * {@link ApplicationClassRule ApplicationClassRule} and
 * {@link ApplicationMethodRule ApplicationMethodRule}
 * and specify your runner of choice via {@link RunWith @RunWith(...)}.</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @see ContextConfiguration
 * @see TestContext
 * @see TestContextManager
 * @see TestExecutionListeners
 * @see ServletTestExecutionListener
 * @see DirtiesContextBeforeModesTestExecutionListener
 * @see ApplicationEventsTestExecutionListener
 * @see DependencyInjectionTestExecutionListener
 * @see DirtiesContextTestExecutionListener
 * @see EventPublishingTestExecutionListener
 * @see AbstractTransactionalJUnit4ContextTests
 * @see AbstractTestNGContextTests
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@TestExecutionListeners({ ServletTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
        ApplicationEventsTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class, EventPublishingTestExecutionListener.class })
public abstract class AbstractJUnit4ContextTests implements ApplicationContextAware {

  /**
   * Logger available to subclasses.
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * The {@link ApplicationContext} that was injected into this test instance
   * via {@link #setApplicationContext(ApplicationContext)}.
   */
  @Nullable
  protected ApplicationContext applicationContext;

  /**
   * Set the {@link ApplicationContext} to be used by this test instance,
   * provided via {@link ApplicationContextAware} semantics.
   *
   * @param applicationContext the ApplicationContext that this test runs in
   */
  @Override
  public final void setApplicationContext(final ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

}
