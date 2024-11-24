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
import infra.beans.testfixture.beans.Employee;
import infra.beans.testfixture.beans.Pet;
import infra.test.context.junit4.JUnit4ClassRunner;
import infra.test.context.junit4.annotation.PojoAndStringConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for meta-annotation attribute override support, overriding
 * default attribute values defined in {@link ConfigClassesAndProfilesWithCustomDefaultsMetaConfig}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ConfigClassesAndProfilesWithCustomDefaultsMetaConfig(classes = { PojoAndStringConfig.class,
        ConfigClassesAndProfilesWithCustomDefaultsMetaConfig.ProductionConfig.class }, profiles = "prod")
public class ConfigClassesAndProfilesWithCustomDefaultsMetaConfigWithOverridesTests {

  @Autowired
  private String foo;

  @Autowired
  private Pet pet;

  @Autowired
  protected Employee employee;

  @Test
  public void verifyEmployee() {
    assertThat(this.employee).as("The employee should have been autowired.").isNotNull();
    assertThat(this.employee.getName()).isEqualTo("John Smith");
  }

  @Test
  public void verifyPet() {
    assertThat(this.pet).as("The pet should have been autowired.").isNotNull();
    assertThat(this.pet.getName()).isEqualTo("Fido");
  }

  @Test
  public void verifyFoo() {
    assertThat(this.foo).isEqualTo("Production Foo");
  }
}
