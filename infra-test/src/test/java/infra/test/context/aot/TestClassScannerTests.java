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
