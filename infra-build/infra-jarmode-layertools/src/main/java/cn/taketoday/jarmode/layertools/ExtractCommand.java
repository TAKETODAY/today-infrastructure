/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.jarmode.layertools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StreamUtils;

/**
 * The {@code 'extract'} tools command.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ExtractCommand extends Command {

  static final Option DESTINATION_OPTION = Option.of("destination", "string", "The destination to extract files to");

  private final Context context;

  private final Layers layers;

  ExtractCommand(Context context) {
    this(context, Layers.get(context));
  }

  ExtractCommand(Context context, Layers layers) {
    super("extract", "Extracts layers from the jar for image creation", Options.of(DESTINATION_OPTION),
            Parameters.of("[<layer>...]"));
    this.context = context;
    this.layers = layers;
  }

  @Override
  protected void run(Map<Option, String> options, List<String> parameters) {
    try {
      File destination = options.containsKey(DESTINATION_OPTION) ? new File(options.get(DESTINATION_OPTION))
                                                                 : this.context.getWorkingDir();
      for (String layer : this.layers) {
        if (parameters.isEmpty() || parameters.contains(layer)) {
          mkDirs(new File(destination, layer));
        }
      }
      try (ZipInputStream zip = new ZipInputStream(new FileInputStream(this.context.getArchiveFile()))) {
        ZipEntry entry = zip.getNextEntry();
        Assert.state(entry != null, "File '" + this.context.getArchiveFile().toString()
                + "' is not compatible with layertools; ensure jar file is valid and launch script is not enabled");
        while (entry != null) {
          if (!entry.isDirectory()) {
            String layer = this.layers.getLayer(entry);
            if (parameters.isEmpty() || parameters.contains(layer)) {
              write(zip, entry, new File(destination, layer));
            }
          }
          entry = zip.getNextEntry();
        }
      }
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void write(ZipInputStream zip, ZipEntry entry, File destination) throws IOException {
    String canonicalOutputPath = destination.getCanonicalPath() + File.separator;
    File file = new File(destination, entry.getName());
    String canonicalEntryPath = file.getCanonicalPath();
    Assert.state(canonicalEntryPath.startsWith(canonicalOutputPath),
            () -> "Entry '" + entry.getName() + "' would be written to '" + canonicalEntryPath
                    + "'. This is outside the output location of '" + canonicalOutputPath
                    + "'. Verify the contents of your archive.");
    mkParentDirs(file);
    try (OutputStream out = new FileOutputStream(file)) {
      StreamUtils.copy(zip, out);
    }
    try {
      Files.getFileAttributeView(file.toPath(), BasicFileAttributeView.class)
              .setTimes(entry.getLastModifiedTime(), entry.getLastAccessTime(), entry.getCreationTime());
    }
    catch (IOException ex) {
      // File system does not support setting time attributes. Continue.
    }
  }

  private void mkParentDirs(File file) throws IOException {
    mkDirs(file.getParentFile());
  }

  private void mkDirs(File file) throws IOException {
    if (!file.exists() && !file.mkdirs()) {
      throw new IOException("Unable to create directory " + file);
    }
  }

}
