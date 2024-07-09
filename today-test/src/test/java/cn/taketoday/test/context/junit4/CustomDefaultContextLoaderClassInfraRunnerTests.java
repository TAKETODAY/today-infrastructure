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

package cn.taketoday.test.context.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.test.context.BootstrapWith;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextLoader;
import cn.taketoday.test.context.support.DefaultTestContextBootstrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which verify that a subclass of {@link DefaultTestContextBootstrapper}
 * can specify a custom <em>default ContextLoader class</em> that overrides the standard
 * default class name.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@BootstrapWith(CustomDefaultContextLoaderClassInfraRunnerTests.PropertiesBasedTestContextBootstrapper.class)
@ContextConfiguration("PropertiesBasedInfraJUnit4ClassRunnerAppCtxTests-context.properties")
public class CustomDefaultContextLoaderClassInfraRunnerTests {

  @Autowired
  private Pet cat;

  @Autowired
  private String testString;

  @Test
  public void verifyAnnotationAutowiredFields() {
    assertThat(this.cat).as("The cat field should have been autowired.").isNotNull();
    assertThat(this.cat.getName()).isEqualTo("Garfield");

    assertThat(this.testString).as("The testString field should have been autowired.").isNotNull();
    assertThat(this.testString).isEqualTo("Test String");
  }

  public static class PropertiesBasedTestContextBootstrapper extends DefaultTestContextBootstrapper {

    @Override
    @SuppressWarnings("deprecation")
    protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
      return cn.taketoday.test.context.support.GenericPropertiesContextLoader.class;
    }
  }

}
