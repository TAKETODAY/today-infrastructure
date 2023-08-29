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

package cn.taketoday.context.properties.processor.test;

import java.io.InputStream;

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.metadata.JsonMarshaller;
import cn.taketoday.core.test.tools.Compiled;
import cn.taketoday.core.test.tools.TestCompiler;

/**
 * Read the contents of metadata generated from the {@link TestCompiler}.
 *
 * @author Scott Frederick
 */
public final class CompiledMetadataReader {

  private static final String METADATA_FILE = "META-INF/infra-configuration-metadata.json";

  private CompiledMetadataReader() {
  }

  public static ConfigurationMetadata getMetadata(Compiled compiled) {
    InputStream inputStream = compiled.getClassLoader().getResourceAsStream(METADATA_FILE);
    try {
      if (inputStream != null) {
        return new JsonMarshaller().read(inputStream);
      }
      else {
        return null;
      }
    }
    catch (Exception ex) {
      throw new RuntimeException("Failed to read metadata", ex);
    }
  }

}
