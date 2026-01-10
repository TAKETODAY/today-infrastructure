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

package infra.test.context.junit4.profile.resolver;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Configuration;
import infra.test.context.ActiveProfiles;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michail Nikolaev
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration
@ActiveProfiles(resolver = ClassNameActiveProfilesResolver.class)
public class ClassNameActiveProfilesResolverTests {

  @Configuration
  static class Config {

  }

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  public void test() {
    assertThat(Arrays.asList(applicationContext.getEnvironment().getActiveProfiles()).contains(
            getClass().getSimpleName().toLowerCase())).isTrue();
  }

}
