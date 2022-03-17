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

package cn.taketoday.test.context.event;

import cn.taketoday.test.context.event.annotation.AfterTestClass;
import cn.taketoday.test.context.event.annotation.AfterTestExecution;
import cn.taketoday.test.context.event.annotation.AfterTestMethod;
import cn.taketoday.test.context.event.annotation.BeforeTestClass;
import cn.taketoday.test.context.event.annotation.BeforeTestExecution;
import cn.taketoday.test.context.event.annotation.BeforeTestMethod;
import cn.taketoday.test.context.event.annotation.PrepareTestInstance;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.cache.ContextCache;
import cn.taketoday.test.context.support.AbstractTestExecutionListener;

/**
 * {@code TestExecutionListener} that publishes test execution events to the
 * {@link cn.taketoday.context.ApplicationContext ApplicationContext}
 * for the currently executing test.
 *
 * <h3>Supported Events</h3>
 * <ul>
 * <li>{@link BeforeTestClassEvent}</li>
 * <li>{@link PrepareTestInstanceEvent}</li>
 * <li>{@link BeforeTestMethodEvent}</li>
 * <li>{@link BeforeTestExecutionEvent}</li>
 * <li>{@link AfterTestExecutionEvent}</li>
 * <li>{@link AfterTestMethodEvent}</li>
 * <li>{@link AfterTestClassEvent}</li>
 * </ul>
 *
 * <p>These events may be consumed for various reasons, such as resetting <em>mock</em>
 * beans or tracing test execution. One advantage of consuming test events rather
 * than implementing a custom {@link TestExecutionListener
 * TestExecutionListener} is that test events may be consumed by any Spring bean
 * registered in the test {@code ApplicationContext}, and such beans may benefit
 * directly from dependency injection and other features of the {@code ApplicationContext}.
 * In contrast, a {@code TestExecutionListener} is not a bean in the {@code ApplicationContext}.
 *
 * <p>Note that the {@code EventPublishingTestExecutionListener} is registered by
 * default; however, it only publishes events if the {@code ApplicationContext}
 * {@linkplain TestContext#hasApplicationContext() has already been loaded}. This
 * prevents the {@code ApplicationContext} from being loaded unnecessarily or too
 * early. Consequently, a {@code BeforeTestClassEvent} will not be published until
 * after the {@code ApplicationContext} has been loaded by another
 * {@code TestExecutionListener}. For example, with the default set of
 * {@code TestExecutionListeners} registered, a {@code BeforeTestClassEvent} will
 * not be published for the first test class that uses a particular test
 * {@code ApplicationContext}, but a {@code BeforeTestClassEvent} will be published
 * for any subsequent test class in the same test suite that uses the same test
 * {@code ApplicationContext} since the context will already have been loaded
 * when subsequent test classes run (as long as the context has not been removed
 * from the {@link ContextCache ContextCache}
 * via {@link DirtiesContext @DirtiesContext}
 * or the max-size eviction policy). If you wish to ensure that a
 * {@code BeforeTestClassEvent} is published for every test class, you need to
 * register a {@code TestExecutionListener} that loads the {@code ApplicationContext}
 * in the {@link TestExecutionListener#beforeTestClass
 * beforeTestClass} callback, and that {@code TestExecutionListener} must be registered
 * before the {@code EventPublishingTestExecutionListener}. Similarly, if
 * {@code @DirtiesContext} is used to remove the {@code ApplicationContext} from
 * the context cache after the last test method in a given test class, the
 * {@code AfterTestClassEvent} will not be published for that test class.
 *
 * <h3>Exception Handling</h3>
 * <p>By default, if a test event listener throws an exception while consuming
 * a test event, that exception will propagate to the underlying testing framework
 * in use. For example, if the consumption of a {@code BeforeTestMethodEvent}
 * results in an exception, the corresponding test method will fail as a result
 * of the exception. In contrast, if an asynchronous test event listener throws
 * an exception, the exception will not propagate to the underlying testing framework.
 * For further details on asynchronous exception handling, consult the class-level
 * Javadoc for {@link cn.taketoday.context.event.EventListener @EventListener}.
 *
 * <h3>Asynchronous Listeners</h3>
 * <p>If you want a particular test event listener to process events asynchronously,
 * you can use Spring's {@link cn.taketoday.scheduling.annotation.Async @Async}
 * support. For further details, consult the class-level Javadoc for
 * {@link cn.taketoday.context.event.EventListener @EventListener}.
 *
 * @author Sam Brannen
 * @author Frank Scheffler
 * @see BeforeTestClass @BeforeTestClass
 * @see PrepareTestInstance @PrepareTestInstance
 * @see BeforeTestMethod @BeforeTestMethod
 * @see BeforeTestExecution @BeforeTestExecution
 * @see AfterTestExecution @AfterTestExecution
 * @see AfterTestMethod @AfterTestMethod
 * @see AfterTestClass @AfterTestClass
 * @since 4.0
 */
public class EventPublishingTestExecutionListener extends AbstractTestExecutionListener {

  /**
   * Returns {@code 10000}.
   */
  @Override
  public final int getOrder() {
    return 10_000;
  }

  /**
   * Publish a {@link BeforeTestClassEvent} to the {@code ApplicationContext}
   * for the supplied {@link TestContext}.
   */
  @Override
  public void beforeTestClass(TestContext testContext) {
    testContext.publishEvent(BeforeTestClassEvent::new);
  }

  /**
   * Publish a {@link PrepareTestInstanceEvent} to the {@code ApplicationContext}
   * for the supplied {@link TestContext}.
   */
  @Override
  public void prepareTestInstance(TestContext testContext) {
    testContext.publishEvent(PrepareTestInstanceEvent::new);
  }

  /**
   * Publish a {@link BeforeTestMethodEvent} to the {@code ApplicationContext}
   * for the supplied {@link TestContext}.
   */
  @Override
  public void beforeTestMethod(TestContext testContext) {
    testContext.publishEvent(BeforeTestMethodEvent::new);
  }

  /**
   * Publish a {@link BeforeTestExecutionEvent} to the {@code ApplicationContext}
   * for the supplied {@link TestContext}.
   */
  @Override
  public void beforeTestExecution(TestContext testContext) {
    testContext.publishEvent(BeforeTestExecutionEvent::new);
  }

  /**
   * Publish an {@link AfterTestExecutionEvent} to the {@code ApplicationContext}
   * for the supplied {@link TestContext}.
   */
  @Override
  public void afterTestExecution(TestContext testContext) {
    testContext.publishEvent(AfterTestExecutionEvent::new);
  }

  /**
   * Publish an {@link AfterTestMethodEvent} to the {@code ApplicationContext}
   * for the supplied {@link TestContext}.
   */
  @Override
  public void afterTestMethod(TestContext testContext) {
    testContext.publishEvent(AfterTestMethodEvent::new);
  }

  /**
   * Publish an {@link AfterTestClassEvent} to the {@code ApplicationContext}
   * for the supplied {@link TestContext}.
   */
  @Override
  public void afterTestClass(TestContext testContext) {
    testContext.publishEvent(AfterTestClassEvent::new);
  }

}
