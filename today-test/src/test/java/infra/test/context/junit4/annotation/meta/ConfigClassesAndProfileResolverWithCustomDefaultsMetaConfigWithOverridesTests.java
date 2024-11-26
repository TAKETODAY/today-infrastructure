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
