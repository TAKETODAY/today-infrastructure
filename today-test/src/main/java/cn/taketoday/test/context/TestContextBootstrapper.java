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

package cn.taketoday.test.context;

import java.util.List;

import cn.taketoday.test.context.support.AbstractTestContextBootstrapper;
import cn.taketoday.test.context.support.DefaultTestContextBootstrapper;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.context.web.WebTestContextBootstrapper;

/**
 * {@code TestContextBootstrapper} defines the SPI for bootstrapping the
 * <em>Spring TestContext Framework</em>.
 *
 * <p>A {@code TestContextBootstrapper} is used by the {@link TestContextManager} to
 * {@linkplain #getTestExecutionListeners get the TestExecutionListeners} for the
 * current test and to {@linkplain #buildTestContext build the TestContext} that
 * it manages.
 *
 * <h3>Configuration</h3>
 *
 * <p>A custom bootstrapping strategy can be configured for a test class (or
 * test class hierarchy) via {@link BootstrapWith @BootstrapWith}, either
 * directly or as a meta-annotation.
 *
 * <p>If a bootstrapper is not explicitly configured via {@code @BootstrapWith},
 * either the {@link DefaultTestContextBootstrapper
 * DefaultTestContextBootstrapper} or the
 * {@link WebTestContextBootstrapper
 * WebTestContextBootstrapper} will be used, depending on the presence of
 * {@link WebAppConfiguration @WebAppConfiguration}.
 *
 * <h3>Implementation Notes</h3>
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * <p><strong>WARNING</strong>: this SPI will likely change in the future in
 * order to accommodate new requirements. Implementers are therefore strongly encouraged
 * <strong>not</strong> to implement this interface directly but rather to <em>extend</em>
 * {@link AbstractTestContextBootstrapper
 * AbstractTestContextBootstrapper} or one of its concrete subclasses instead.
 *
 * @author Sam Brannen
 * @see BootstrapWith
 * @see BootstrapContext
 * @since 4.0
 */
public interface TestContextBootstrapper {

  /**
   * Set the {@link BootstrapContext} to be used by this bootstrapper.
   */
  void setBootstrapContext(BootstrapContext bootstrapContext);

  /**
   * Get the {@link BootstrapContext} associated with this bootstrapper.
   */
  BootstrapContext getBootstrapContext();

  /**
   * Build the {@link TestContext} for the {@link BootstrapContext}
   * associated with this bootstrapper.
   *
   * @return a new {@link TestContext}, never {@code null}
   * @see #buildMergedContextConfiguration()
   * @since 4.0
   */
  TestContext buildTestContext();

  /**
   * Build the {@linkplain MergedContextConfiguration merged context configuration}
   * for the test class in the {@link BootstrapContext} associated with this
   * bootstrapper.
   * <p>Implementations must take the following into account when building the
   * merged configuration:
   * <ul>
   * <li>Context hierarchies declared via {@link ContextHierarchy @ContextHierarchy}
   * and {@link ContextConfiguration @ContextConfiguration}</li>
   * <li>Active bean definition profiles declared via {@link ActiveProfiles @ActiveProfiles}</li>
   * <li>{@linkplain cn.taketoday.context.ApplicationContextInitializer
   * Context initializers} declared via {@link ContextConfiguration#initializers}</li>
   * <li>Test property sources declared via {@link TestPropertySource @TestPropertySource}</li>
   * </ul>
   * <p>Consult the Javadoc for the aforementioned annotations for details on
   * the required semantics.
   * <p>Note that the implementation of {@link #buildTestContext()} should
   * typically delegate to this method when constructing the {@code TestContext}.
   * <p>When determining which {@link ContextLoader} to use for a given test
   * class, the following algorithm should be used:
   * <ol>
   * <li>If a {@code ContextLoader} class has been explicitly declared via
   * {@link ContextConfiguration#loader}, use it.</li>
   * <li>Otherwise, concrete implementations are free to determine which
   * {@code ContextLoader} class to use as a default.</li>
   * </ol>
   *
   * @return the merged context configuration, never {@code null}
   * @see #buildTestContext()
   */
  MergedContextConfiguration buildMergedContextConfiguration();

  /**
   * Get a list of newly instantiated {@link TestExecutionListener TestExecutionListeners}
   * for the test class in the {@link BootstrapContext} associated with this bootstrapper.
   * <p>If {@link TestExecutionListeners @TestExecutionListeners} is not
   * <em>present</em> on the test class in the {@code BootstrapContext},
   * <em>default</em> listeners should be returned. Furthermore, default
   * listeners must be sorted using
   * {@link cn.taketoday.core.annotation.AnnotationAwareOrderComparator
   * AnnotationAwareOrderComparator}.
   * <p>Concrete implementations are free to determine what comprises the
   * set of default listeners. However, by default, the Spring TestContext
   * Framework will use the
   * {@link cn.taketoday.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
   * mechanism to look up all {@code TestExecutionListener} class names
   * configured in all {@code META-INF/today-strategies.properties} files on the classpath.
   * <p>The {@link TestExecutionListeners#inheritListeners() inheritListeners}
   * flag of {@link TestExecutionListeners @TestExecutionListeners} must be
   * taken into consideration. Specifically, if the {@code inheritListeners}
   * flag is set to {@code true}, listeners declared for a given test class must
   * be appended to the end of the list of listeners declared in superclasses.
   *
   * @return a list of {@code TestExecutionListener} instances
   */
  List<TestExecutionListener> getTestExecutionListeners();

}
