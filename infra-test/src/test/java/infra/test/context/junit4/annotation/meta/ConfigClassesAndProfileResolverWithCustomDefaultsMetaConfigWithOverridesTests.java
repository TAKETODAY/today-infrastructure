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

package infra.test.context.junit4.annotation.meta;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Profile;
import infra.test.context.ActiveProfilesResolver;
import infra.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-annotation attribute override support, overriding
 * default attribute values defined in {@link ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig(classes = LocalDevConfig.class, resolver = DevResolver.class)
public class ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfigWithOverridesTests {

  @Autowired
  private String foo;

  @Test
  public void foo() {
    assertThat(foo).isEqualTo("Local Dev Foo");
  }
}

@Configuration
@Profile("dev")
class LocalDevConfig {

  @Bean
  public String foo() {
    return "Local Dev Foo";
  }
}

class DevResolver implements ActiveProfilesResolver {

  @Override
  public String[] resolve(Class<?> testClass) {
    // Checking that the "test class" name ends with "*Tests" ensures that an actual
    // test class is passed to this method as opposed to a "*Config" class which would
    // imply that we likely have been passed the 'declaringClass' instead of the
    // 'rootDeclaringClass'.
    return testClass.getName().endsWith("Tests") ? new String[] { "dev" } : new String[] {};
  }
}
