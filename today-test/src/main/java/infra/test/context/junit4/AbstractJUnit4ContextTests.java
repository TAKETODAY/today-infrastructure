/*
 * Copyright 2002-present the original author or authors.
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
