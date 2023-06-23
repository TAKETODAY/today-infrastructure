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

package cn.taketoday.aot.nativex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.function.Consumer;

import cn.taketoday.lang.Nullable;

/**
 * A {@link NativeConfigurationWriter} implementation that writes the
 * configuration to disk.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @see <a href="https://www.graalvm.org/22.0/reference-manual/native-image/BuildConfiguration/">Native Image Build Configuration</a>
 * @since 4.0
 */
public class FileNativeConfigurationWriter extends NativeConfigurationWriter {

  private final Path basePath;

  @Nullable
  private final String groupId;

  @Nullable
  private final String artifactId;

  public FileNativeConfigurationWriter(Path basePath) {
    this(basePath, null, null);
  }

  public FileNativeConfigurationWriter(Path basePath, @Nullable String groupId, @Nullable String artifactId) {
    this.basePath = basePath;
    if ((groupId == null && artifactId != null) || (groupId != null && artifactId == null)) {
      throw new IllegalArgumentException("groupId and artifactId must be both null or both non-null");
    }
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  @Override
  protected void writeTo(String fileName, Consumer<BasicJsonWriter> writer) {
    try {
      File file = createIfNecessary(fileName);
      try (FileWriter out = new FileWriter(file)) {
        writer.accept(createJsonWriter(out));
      }
    }
    catch (IOException ex) {
      throw new IllegalStateException("Failed to write native configuration for " + fileName, ex);
    }
  }

  private File createIfNecessary(String filename) throws IOException {
    Path outputDirectory = this.basePath.resolve("META-INF").resolve("native-image");
    if (this.groupId != null && this.artifactId != null) {
      outputDirectory = outputDirectory.resolve(this.groupId).resolve(this.artifactId);
    }
    outputDirectory.toFile().mkdirs();
    File file = outputDirectory.resolve(filename).toFile();
    file.createNewFile();
    return file;
  }

  private BasicJsonWriter createJsonWriter(Writer out) {
    return new BasicJsonWriter(out);
  }

}
