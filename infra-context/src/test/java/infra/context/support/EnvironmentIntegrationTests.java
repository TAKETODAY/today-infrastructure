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

package infra.context.support;

import org.junit.jupiter.api.Test;

import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests covering the integration of the {@link Environment} into
 * {@link ApplicationContext} hierarchies.
 *
 * @author Chris Beams
 * @see infra.core.env.EnvironmentSystemIntegrationTests
 */
public class EnvironmentIntegrationTests {

  @Test
  public void repro() {
    ConfigurableApplicationContext parent = new GenericApplicationContext();
    parent.refresh();

    AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext();
    child.setParent(parent);
    child.refresh();

    ConfigurableEnvironment env = child.getBean(ConfigurableEnvironment.class);
    assertThat(env).isSameAs(child.getEnvironment());

    child.close();
    parent.close();
  }

}
