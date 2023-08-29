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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.metadata.InvalidConfigurationMetadataException;
import cn.taketoday.context.properties.processor.metadata.JsonMarshaller;

/**
 * A {@code MetadataStore} is responsible for the storage of metadata on the filesystem.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MetadataStore {

  static final String METADATA_PATH = "META-INF/infra-configuration-metadata.json";

  private static final String ADDITIONAL_METADATA_PATH = "META-INF/additional-infra-configuration-metadata.json";

  private static final String RESOURCES_DIRECTORY = "resources";

  private static final String CLASSES_DIRECTORY = "classes";

  private final ProcessingEnvironment environment;

  public MetadataStore(ProcessingEnvironment environment) {
    this.environment = environment;
  }

  public ConfigurationMetadata readMetadata() {
    try {
      return readMetadata(getMetadataResource().openInputStream());
    }
    catch (IOException ex) {
      return null;
    }
  }

  public void writeMetadata(ConfigurationMetadata metadata) throws IOException {
    if (!metadata.getItems().isEmpty()) {
      try (OutputStream outputStream = createMetadataResource().openOutputStream()) {
        new JsonMarshaller().write(metadata, outputStream);
      }
    }
  }

  public ConfigurationMetadata readAdditionalMetadata() throws IOException {
    return readMetadata(getAdditionalMetadataStream());
  }

  private ConfigurationMetadata readMetadata(InputStream in) {
    try (in) {
      return new JsonMarshaller().read(in);
    }
    catch (IOException ex) {
      return null;
    }
    catch (Exception ex) {
      throw new InvalidConfigurationMetadataException(
              "Invalid additional meta-data in '" + METADATA_PATH + "': " + ex.getMessage(),
              Diagnostic.Kind.ERROR);
    }
  }

  private FileObject getMetadataResource() throws IOException {
    return this.environment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
  }

  private FileObject createMetadataResource() throws IOException {
    return this.environment.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", METADATA_PATH);
  }

  private InputStream getAdditionalMetadataStream() throws IOException {
    // Most build systems will have copied the file to the class output location
    FileObject fileObject = this.environment.getFiler()
            .getResource(StandardLocation.CLASS_OUTPUT, "", ADDITIONAL_METADATA_PATH);
    InputStream inputStream = getMetadataStream(fileObject);
    if (inputStream != null) {
      return inputStream;
    }
    try {
      File file = locateAdditionalMetadataFile(new File(fileObject.toUri()));
      return (file.exists() ? new FileInputStream(file) : fileObject.toUri().toURL().openStream());
    }
    catch (Exception ex) {
      throw new FileNotFoundException();
    }
  }

  private InputStream getMetadataStream(FileObject fileObject) {
    try {
      return fileObject.openInputStream();
    }
    catch (IOException ex) {
      return null;
    }
  }

  File locateAdditionalMetadataFile(File standardLocation) throws IOException {
    if (standardLocation.exists()) {
      return standardLocation;
    }
    String locations = this.environment.getOptions()
            .get(ConfigurationMetadataAnnotationProcessor.ADDITIONAL_METADATA_LOCATIONS_OPTION);
    if (locations != null) {
      for (String location : locations.split(",")) {
        File candidate = new File(location, ADDITIONAL_METADATA_PATH);
        if (candidate.isFile()) {
          return candidate;
        }
      }
    }
    return new File(locateGradleResourcesDirectory(standardLocation), ADDITIONAL_METADATA_PATH);
  }

  private File locateGradleResourcesDirectory(File standardAdditionalMetadataLocation) throws FileNotFoundException {
    String path = standardAdditionalMetadataLocation.getPath();
    int index = path.lastIndexOf(CLASSES_DIRECTORY);
    if (index < 0) {
      throw new FileNotFoundException();
    }
    String buildDirectoryPath = path.substring(0, index);
    File classOutputLocation = standardAdditionalMetadataLocation.getParentFile().getParentFile();
    return new File(buildDirectoryPath, RESOURCES_DIRECTORY + '/' + classOutputLocation.getName());
  }

}
