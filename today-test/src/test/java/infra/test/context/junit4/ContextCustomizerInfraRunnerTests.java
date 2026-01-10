/*
 * Copyright 2017 - 2026 the TODAY authors.
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
