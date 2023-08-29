/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.annotation.processing.ProcessingEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetadataStore}.
 *
 * @author Andy Wilkinson
 */
class MetadataStoreTests {

  @TempDir
  File tempDir;

  private final ProcessingEnvironment environment = mock(ProcessingEnvironment.class);

  private final MetadataStore metadataStore = new MetadataStore(this.environment);

  @Test
  void additionalMetadataIsLocatedInMavenBuild() throws IOException {
    File app = new File(this.tempDir, "app");
    File classesLocation = new File(app, "target/classes");
    File metaInf = new File(classesLocation, "META-INF");
    metaInf.mkdirs();
    File additionalMetadata = new File(metaInf, "additional-infra-configuration-metadata.json");
    additionalMetadata.createNewFile();
    assertThat(this.metadataStore.locateAdditionalMetadataFile(
            new File(classesLocation, "META-INF/additional-infra-configuration-metadata.json")))
            .isEqualTo(additionalMetadata);
  }

  @Test
  void additionalMetadataIsLocatedInGradle3Build() throws IOException {
    File app = new File(this.tempDir, "app");
    File classesLocation = new File(app, "build/classes/main");
    File resourcesLocation = new File(app, "build/resources/main");
    File metaInf = new File(resourcesLocation, "META-INF");
    metaInf.mkdirs();
    File additionalMetadata = new File(metaInf, "additional-infra-configuration-metadata.json");
    additionalMetadata.createNewFile();
    assertThat(this.metadataStore.locateAdditionalMetadataFile(
            new File(classesLocation, "META-INF/additional-infra-configuration-metadata.json")))
            .isEqualTo(additionalMetadata);
  }

  @Test
  void additionalMetadataIsLocatedInGradle4Build() throws IOException {
    File app = new File(this.tempDir, "app");
    File classesLocation = new File(app, "build/classes/java/main");
    File resourcesLocation = new File(app, "build/resources/main");
    File metaInf = new File(resourcesLocation, "META-INF");
    metaInf.mkdirs();
    File additionalMetadata = new File(metaInf, "additional-infra-configuration-metadata.json");
    additionalMetadata.createNewFile();
    assertThat(this.metadataStore.locateAdditionalMetadataFile(
            new File(classesLocation, "META-INF/additional-infra-configuration-metadata.json")))
            .isEqualTo(additionalMetadata);
  }

  @Test
  void additionalMetadataIsLocatedUsingLocationsOption() throws IOException {
    File app = new File(this.tempDir, "app");
    File location = new File(app, "src/main/resources");
    File metaInf = new File(location, "META-INF");
    metaInf.mkdirs();
    File additionalMetadata = new File(metaInf, "additional-infra-configuration-metadata.json");
    additionalMetadata.createNewFile();
    given(this.environment.getOptions()).willReturn(
            Collections.singletonMap(ConfigurationMetadataAnnotationProcessor.ADDITIONAL_METADATA_LOCATIONS_OPTION,
                    location.getAbsolutePath()));
    assertThat(this.metadataStore.locateAdditionalMetadataFile(new File(app, "foo"))).isEqualTo(additionalMetadata);
  }

}
