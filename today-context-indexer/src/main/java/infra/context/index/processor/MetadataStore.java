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

package infra.context.index.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Store {@link CandidateComponentsMetadata} on the filesystem.
 *
 * @author Stephane Nicoll
 * @since 4.0
 */
class MetadataStore {

  static final String METADATA_PATH = "META-INF/today.components";
  private final ProcessingEnvironment environment;

  public MetadataStore(ProcessingEnvironment environment) {
    this.environment = environment;
  }

  public CandidateComponentsMetadata readMetadata() {
    try {
      return readMetadata(getMetadataResource().openInputStream());
    }
    catch (IOException ex) {
      // Failed to read metadata -> ignore.
      return null;
    }
  }

  public void writeMetadata(CandidateComponentsMetadata metadata) throws IOException {
    if (!metadata.getItems().isEmpty()) {
      try (OutputStream outputStream = createMetadataResource().openOutputStream()) {
        PropertiesMarshaller.write(metadata, outputStream);
      }
    }
  }

  private CandidateComponentsMetadata readMetadata(InputStream in) throws IOException {
    try (in) {
      return PropertiesMarshaller.read(in);
    }
  }

  private FileObject getMetadataResource() throws IOException {
    return this.environment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
  }

  private FileObject createMetadataResource() throws IOException {
    return this.environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
  }

}
