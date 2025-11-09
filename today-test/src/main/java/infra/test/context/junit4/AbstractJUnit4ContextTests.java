/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.context.junit4;

import org.jspecify.annotations.Nullable;
import org.junit.runner.RunWith;

import infra.context.ApplicationContext;
import infra.context.ApplicationContextAware;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestContext;
import infra.test.context.TestContextManager;
import infra.test.context.TestExecutionListeners;
import infra.test.context.event.ApplicationEventsTestExecutionListener;
import infra.test.context.event.EventPublishingTestExecutionListener;
import infra.test.context.junit4.rules.InfraClassRule;
import infra.test.context.junit4.rules.InfraMethodRule;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import infra.test.context.support.DirtiesContextTestExecutionListener;
import infra.test.context.web.MockTestExecutionListener;

/**
 * Abstract base test class which integrates the <em>Infra TestContext
 * Framework</em> with explicit {@link ApplicationContext} testing support
 * in a <strong>JUnit 4</strong> environment.
 *
 * <p>Concrete subclasses should typically declare a class-level
 * {@link ContextConfiguration @ContextConfiguration} annotation to
 * configure the {@linkplain ApplicationContext application context} {@linkplain
 * ContextConfiguration#locations() resource locations} or {@linkplain
 * ContextConfiguration#classes() component classes}.
 *
 * <p>This class serves only as a convenience for extension.
 * <ul>
 * <li>If you do not wish for your test classes to be tied to a Infra-specific
 * class hierarchy, you may configure your own custom test classes by using
 * {@link InfraRunner}, {@link ContextConfiguration @ContextConfiguration},
 * {@link TestExecutionListeners @TestExecutionListeners}, etc.</li>
 * <li>If you wish to extend this class and use a runner other than the
 * {@link InfraRunner}, you can use
 * {@link InfraClassRule ApplicationClassRule} and
 * {@link InfraMethodRule ApplicationMethodRule}
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
 * @see MockTestExecutionListener
 * @see DirtiesContextBeforeModesTestExecutionListener
 * @see ApplicationEventsTestExecutionListener
 * @see DependencyInjectionTestExecutionListener
 * @see DirtiesContextTestExecutionListener
 * @see EventPublishingTestExecutionListener
 * @see AbstractTransactionalJUnit4ContextTests
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@TestExecutionListeners({ MockTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
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
