/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.context.aot;

import org.junit.jupiter.api.Test;

import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterImportedConfigTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterSharedConfigTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraJupiterTests;
import cn.taketoday.test.context.aot.samples.basic.BasicInfraVintageTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestClassScanner}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
class TestClassScannerTests extends AbstractAotTests {

  @Test
  void scanBasicTestClasses() {
    assertThat(scan("cn.taketoday.test.context.aot.samples.basic"))
            .containsExactlyInAnyOrder(
                    BasicInfraJupiterImportedConfigTests.class,
                    BasicInfraJupiterSharedConfigTests.class,
                    BasicInfraJupiterTests.class,
                    BasicInfraJupiterTests.NestedTests.class,
                    BasicInfraVintageTests.class
            );
  }

  @Test
  void scanTestSuitesForJupiter() {
    assertThat(scan("cn.taketoday.test.context.aot.samples.suites.jupiter"))
            .containsExactlyInAnyOrder(BasicInfraJupiterImportedConfigTests.class,
                    BasicInfraJupiterSharedConfigTests.class, BasicInfraJupiterTests.class,
                    BasicInfraJupiterTests.NestedTests.class);
  }

  @Test
  void scanTestSuitesForVintage() {
    assertThat(scan("cn.taketoday.test.context.aot.samples.suites.vintage"))
            .containsExactly(BasicInfraVintageTests.class);
  }

  @Test
  void scanTestSuitesForAllTestEngines() {
    assertThat(scan("cn.taketoday.test.context.aot.samples.suites.all"))
            .containsExactlyInAnyOrder(
                    BasicInfraJupiterImportedConfigTests.class,
                    BasicInfraJupiterSharedConfigTests.class,
                    BasicInfraJupiterTests.class,
                    BasicInfraJupiterTests.NestedTests.class,
                    BasicInfraVintageTests.class
            );
  }

  @Test
  void scanTestSuitesWithNestedSuites() {
    assertThat(scan("cn.taketoday.test.context.aot.samples.suites.nested"))
            .containsExactlyInAnyOrder(
                    BasicInfraJupiterImportedConfigTests.class,
                    BasicInfraJupiterSharedConfigTests.class,
                    BasicInfraJupiterTests.class,
                    BasicInfraJupiterTests.NestedTests.class,
                    BasicInfraVintageTests.class
            );
  }

  @Test
  void scanEntireSpringTestModule() {
    assertThat(scan()).hasSizeGreaterThan(400);
  }

}
