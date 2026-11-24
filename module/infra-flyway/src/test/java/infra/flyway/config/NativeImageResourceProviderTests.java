/*
 * Copyright 2012-present the original author or authors.
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

package infra.flyway.config;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.ResourceProvider;
import org.flywaydb.core.api.resource.LoadableResource;
import org.flywaydb.core.internal.scanner.Scanner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import infra.core.NativeDetector;
import infra.test.classpath.ForkedClassPath;
import infra.test.classpath.resources.WithResource;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests for {@link NativeImageResourceProvider}.
 *
 * @author Dongliang Xie
 * @author Stephane Nicoll
 */
class NativeImageResourceProviderTests {

  @Test
  @ForkedClassPath
  @WithResource(name = "db/migration/nested/V2__users.sql", content = "select 1;")
  void nativeImageResourceProviderShouldReadNestedMigrations() throws IOException {
    System.setProperty("org.graalvm.nativeimage.imagecode", "true");
    try {
      Scanner<?> scanner = mock();
      given(scanner.getResources("V", ".sql")).willReturn(Collections.emptyList());
      Location location = Location.fromPath("classpath:", "db/migration");
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      ResourceProvider resourceProvider = new NativeImageResourceProvider(scanner, classLoader, List.of(location),
              StandardCharsets.UTF_8, true);
      Collection<LoadableResource> migrations = resourceProvider.getResources("V", new String[] { ".sql" });
      assertThat(migrations).hasSize(1);
      LoadableResource migration = migrations.iterator().next();
      assertThat(FileCopyUtils.copyToString(migration.read())).isEqualTo("select 1;");
    }
    finally {
      System.clearProperty("org.graalvm.nativeimage.imagecode");
    }
  }

  @Test
  @WithResource(name = "flyway-native/lookup/V1__schema.sql", content = "select 1;")
  void prefersScannerForSingleResourceAndFallsBackToClassLoader() throws IOException {
    Scanner<?> scanner = mock();
    LoadableResource scanned = mock();
    given(scanner.getResource("V2__scanned.sql")).willReturn(scanned);
    var provider = new NativeImageResourceProvider(scanner, Thread.currentThread().getContextClassLoader(),
            List.of(), StandardCharsets.UTF_8, false);

    try (var nativeDetector = mockStatic(NativeDetector.class)) {
      nativeDetector.when(NativeDetector::inNativeImage).thenReturn(true);
      assertThat(provider.getResource("V2__scanned.sql")).isSameAs(scanned);
      LoadableResource fallback = provider.getResource("flyway-native/lookup/V1__schema.sql");
      assertThat(fallback).isNotNull();
      assertThat(FileCopyUtils.copyToString(fallback.read())).isEqualTo("select 1;");
      assertThat(provider.getResource("flyway-native/lookup/missing.sql")).isNull();
    }
  }

  @Test
  @WithResource(name = "flyway-native/list/V1__schema.sql", content = "select 1;")
  @WithResource(name = "flyway-native/list/README.txt", content = "Not a migration")
  void mergesScannedAndClasspathMigrationsWhileIgnoringOptionalLocations() {
    Scanner<?> scanner = mock();
    LoadableResource scanned = mock();
    given(scanned.getFilename()).willReturn("V2__scanned.sql");
    given(scanner.getResources("V", ".sql")).willReturn(List.of(scanned));
    var provider = new NativeImageResourceProvider(scanner, Thread.currentThread().getContextClassLoader(),
            List.of(Location.fromPath("filesystem:", "unused"), Location.fromPath("classpath:", "flyway-native/missing"),
                    Location.fromPath("classpath:", "flyway-native/list")), StandardCharsets.UTF_8, false);

    try (var nativeDetector = mockStatic(NativeDetector.class)) {
      nativeDetector.when(NativeDetector::inNativeImage).thenReturn(true);
      for (int i = 0; i < 2; i++) {
        assertThat(provider.getResources("V", new String[] { ".sql" }))
                .extracting(LoadableResource::getFilename).containsExactlyInAnyOrder("V1__schema.sql", "V2__scanned.sql");
      }
    }
  }

  @Test
  void failsWhenRequiredClasspathLocationIsMissing() {
    var provider = new NativeImageResourceProvider(mock(Scanner.class), Thread.currentThread().getContextClassLoader(),
            List.of(Location.fromPath("classpath:", "flyway-native/required-but-missing")), StandardCharsets.UTF_8, true);

    try (var nativeDetector = mockStatic(NativeDetector.class)) {
      nativeDetector.when(NativeDetector::inNativeImage).thenReturn(true);
      assertThatExceptionOfType(FlywayException.class)
              .isThrownBy(() -> provider.getResources("V", new String[] { ".sql" }))
              .withMessage("Location classpath:flyway-native/required-but-missing doesn't exist");
    }
  }

}
