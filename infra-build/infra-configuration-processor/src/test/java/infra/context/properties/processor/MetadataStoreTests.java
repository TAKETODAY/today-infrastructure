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

package infra.context.properties.processor;

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

  private final MetadataStore metadataStore = new MetadataStore(this.environment, mock(TypeUtils.class));

  @Test
  void additionalMetadataIsLocatedInMavenBuild() throws IOException {
    File app = new File(this.tempDir, "app");
    File classesLocation = new File(app, "target/classes");
    File metaInf = new File(classesLocation, "META-INF");
    metaInf.mkdirs();
    File additionalMetadata = new File(metaInf, "additional-infra-configuration-metadata.json");
    additionalMetadata.createNewFile();
    assertThat(this.metadataStore.locateAdditionalMetadataFile(
            new File(classesLocation, "META-INF/additional-infra-configuration-metadata.json"),
            "META-INF/additional-infra-configuration-metadata.json"))
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
            new File(classesLocation, "META-INF/additional-infra-configuration-metadata.json"),
            "META-INF/additional-infra-configuration-metadata.json"))
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
            new File(classesLocation, "META-INF/additional-infra-configuration-metadata.json"),
            "META-INF/additional-infra-configuration-metadata.json"))
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
    assertThat(this.metadataStore.locateAdditionalMetadataFile(new File(app, "foo"),
            "META-INF/additional-infra-configuration-metadata.json"))
            .isEqualTo(additionalMetadata);
  }

}
