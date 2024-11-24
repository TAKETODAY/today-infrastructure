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

package infra.test.context.aot;

import org.junit.jupiter.api.Test;

import infra.test.context.aot.samples.basic.BasicInfraJupiterImportedConfigTests;
import infra.test.context.aot.samples.basic.BasicInfraJupiterSharedConfigTests;
import infra.test.context.aot.samples.basic.BasicInfraJupiterTests;
import infra.test.context.aot.samples.basic.BasicInfraVintageTests;
import infra.test.context.aot.samples.basic.DisabledInAotProcessingTests;
import infra.test.context.aot.samples.basic.DisabledInAotRuntimeClassLevelTests;
import infra.test.context.aot.samples.basic.DisabledInAotRuntimeMethodLevelTests;

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
    assertThat(scan("infra.test.context.aot.samples.basic"))
            .containsExactlyInAnyOrder(
                    BasicInfraJupiterImportedConfigTests.class,
                    BasicInfraJupiterSharedConfigTests.class,
                    BasicInfraJupiterTests.class,
                    BasicInfraJupiterTests.NestedTests.class,
                    BasicInfraVintageTests.class,
                    DisabledInAotProcessingTests.class,
                    DisabledInAotRuntimeClassLevelTests.class,
                    DisabledInAotRuntimeMethodLevelTests.class
            );
  }

  @Test
  void scanTestSuitesForJupiter() {
    assertThat(scan("infra.test.context.aot.samples.suites.jupiter"))
            .containsExactlyInAnyOrder(BasicInfraJupiterImportedConfigTests.class,
                    BasicInfraJupiterSharedConfigTests.class, BasicInfraJupiterTests.class,
                    BasicInfraJupiterTests.NestedTests.class,
                    DisabledInAotProcessingTests.class,
                    DisabledInAotRuntimeClassLevelTests.class,
                    DisabledInAotRuntimeMethodLevelTests.class
            );
  }

  @Test
  void scanTestSuitesForVintage() {
    assertThat(scan("infra.test.context.aot.samples.suites.vintage"))
            .containsExactly(BasicInfraVintageTests.class);
  }

  @Test
  void scanTestSuitesForAllTestEngines() {
    assertThat(scan("infra.test.context.aot.samples.suites.all"))
            .containsExactlyInAnyOrder(
                    BasicInfraJupiterImportedConfigTests.class,
                    BasicInfraJupiterSharedConfigTests.class,
                    BasicInfraJupiterTests.class,
                    BasicInfraJupiterTests.NestedTests.class,
                    BasicInfraVintageTests.class,
                    DisabledInAotProcessingTests.class,
                    DisabledInAotRuntimeClassLevelTests.class,
                    DisabledInAotRuntimeMethodLevelTests.class
            );
  }

  @Test
  void scanTestSuitesWithNestedSuites() {
    assertThat(scan("infra.test.context.aot.samples.suites.nested"))
            .containsExactlyInAnyOrder(
                    BasicInfraJupiterImportedConfigTests.class,
                    BasicInfraJupiterSharedConfigTests.class,
                    BasicInfraJupiterTests.class,
                    BasicInfraJupiterTests.NestedTests.class,
                    BasicInfraVintageTests.class,
                    DisabledInAotProcessingTests.class,
                    DisabledInAotRuntimeClassLevelTests.class,
                    DisabledInAotRuntimeMethodLevelTests.class
            );
  }

  @Test
  void scanEntireTestModule() {
    assertThat(scan()).hasSizeGreaterThan(400);
  }

}
