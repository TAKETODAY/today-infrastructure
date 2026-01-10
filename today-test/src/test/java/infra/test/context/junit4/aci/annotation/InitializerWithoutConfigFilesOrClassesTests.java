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

package infra.test.context.junit4.aci.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.support.GenericApplicationContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies support for {@link ApplicationContextInitializer
 * ApplicationContextInitializers} in the TestContext framework when the test class
 * declares neither XML configuration files nor annotated configuration classes.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(initializers = InitializerWithoutConfigFilesOrClassesTests.EntireAppInitializer.class)
public class InitializerWithoutConfigFilesOrClassesTests {

  @Autowired
  private String foo;

  @Test
  public void foo() {
    assertThat(foo).isEqualTo("foo");
  }

  static class EntireAppInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      if (applicationContext instanceof GenericApplicationContext context) {
        new AnnotatedBeanDefinitionReader(context).register(GlobalConfig.class);
      }
    }
  }

}
