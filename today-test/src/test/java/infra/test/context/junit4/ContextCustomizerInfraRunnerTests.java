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

package infra.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.test.context.BootstrapWith;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.support.DefaultTestContextBootstrapper;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 4 based integration test which verifies support of
 * {@link ContextCustomizerFactory} and {@link ContextCustomizer}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@BootstrapWith(ContextCustomizerInfraRunnerTests.CustomTestContextBootstrapper.class)
public class ContextCustomizerInfraRunnerTests {

  @Autowired
  String foo;

  @Test
  public void injectedBean() {
    assertThat(foo).isEqualTo("foo");
  }

  static class CustomTestContextBootstrapper extends DefaultTestContextBootstrapper {

    @Override
    protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
      return singletonList(
              (testClass, configAttributes) ->
                      (context, mergedConfig) -> context.getBeanFactory().registerSingleton("foo", "foo")
      );
    }
  }

}
