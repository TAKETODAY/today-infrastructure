/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.web.resource;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.FileBasedResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.FileCopyUtils;

/**
 * @author Andy Wilkinson
 */
class GzipSupport implements AfterEachCallback, ParameterResolver {

  private static final Namespace namespace = Namespace.create(GzipSupport.class);

  @Override
  public void afterEach(ExtensionContext context) throws Exception {
    GzippedFiles gzippedFiles = getStore(context).remove(GzippedFiles.class, GzippedFiles.class);
    if (gzippedFiles != null) {
      for (File gzippedFile : gzippedFiles.created) {
        gzippedFile.delete();
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(GzippedFiles.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return getStore(extensionContext).getOrComputeIfAbsent(GzippedFiles.class);
  }

  private Store getStore(ExtensionContext extensionContext) {
    return extensionContext.getStore(namespace);
  }

  static class GzippedFiles {

    private final Set<File> created = new HashSet<>();

    void create(String filePath) {
      try {
        Resource location = new ClassPathResource("test/", EncodedResourceResolverTests.class);
        Resource resource = new FileBasedResource(location.createRelative(filePath).getFile());

        Path gzFilePath = Paths.get(resource.getFile().getAbsolutePath() + ".gz");
        Files.deleteIfExists(gzFilePath);

        File gzFile = Files.createFile(gzFilePath).toFile();
        GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(gzFile));
        FileCopyUtils.copy(resource.getInputStream(), out);
        created.add(gzFile);
      }
      catch (IOException ex) {
        throw new RuntimeException(ex);
      }
    }

  }

}
