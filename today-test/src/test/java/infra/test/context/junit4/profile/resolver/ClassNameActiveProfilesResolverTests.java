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