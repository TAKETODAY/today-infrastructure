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

import java.util.Properties;

import infra.beans.factory.annotation.Autowired;
import infra.beans.testfixture.beans.Pet;
import infra.context.ApplicationContext;
import infra.test.context.ContextConfiguration;
import infra.test.context.support.GenericPropertiesContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>
 * JUnit 4 based test class, which verifies the expected functionality of
 * {@link InfraRunner} in conjunction with support for application contexts
 * loaded from Java {@link Properties} files. Specifically, the
 * {@link ContextConfiguration#loader() loader} attribute of {@code ContextConfiguration}
 * and the
 * {@link GenericPropertiesContextLoader#getResourceSuffix()
 * resourceSuffix} property of {@code GenericPropertiesContextLoader} are tested.
 * </p>
 * <p>
 * Since no {@link ContextConfiguration#locations() locations} are explicitly defined, the
 * {@code resourceSuffix} is set to &quot;-context.properties&quot;, and since default
 * resource locations will be detected by default, this test class's dependencies will be
 * injected via {@link Autowired annotation-based autowiring} from beans defined in the
 * {@link ApplicationContext} loaded from the default classpath resource: &quot;
 * {@code /infra/test/junit4/PropertiesBasedInfraJUnit4ClassRunnerAppCtxTests-context.properties}
 * &quot;.
 * </p>
 *
 * @author Sam Brannen
 * @see GenericPropertiesContextLoader
 * @see JUnit4ClassRunnerAppCtxTests
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@ContextConfiguration(loader = GenericPropertiesContextLoader.class)
public class PropertiesBasedInfraJUnit4ClassRunnerAppCtxTests {

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

}
