/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.context.junit4.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.testfixture.beans.Pet;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.junit4.JUnit4ClassRunner;
import cn.taketoday.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for configuration classes in
 * the TestContext Framework.
 *
 * <p>Configuration will be loaded from {@link DefaultConfigClassesInheritedTests.ContextConfiguration}
 * and {@link DefaultConfigClassesBaseTests.ContextConfiguration}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = DefaultConfigClassesInheritedTests.ContextConfiguration.class)
public class ExplicitConfigClassesInheritedTests extends ExplicitConfigClassesBaseTests {

  @Autowired
  private Pet pet;

  @Test
  public void verifyPetSetFromExtendedContextConfig() {
    assertThat(this.pet).as("The pet should have been autowired.").isNotNull();
    assertThat(this.pet.getName()).isEqualTo("Fido");
  }

}
