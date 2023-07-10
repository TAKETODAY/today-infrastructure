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

package cn.taketoday.app.loader;

import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

import cn.taketoday.app.loader.archive.Archive;
import cn.taketoday.util.FileCopyUtils;

/**
 * Base class for testing {@link ExecutableArchiveLauncher} implementations.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 */
public abstract class AbstractExecutableArchiveLauncherTests {

  @TempDir
  File tempDir;

  protected File createJarArchive(String name, String entryPrefix) throws IOException {
    return createJarArchive(name, entryPrefix, false, Collections.emptyList());
  }

  @SuppressWarnings("resource")
  protected File createJarArchive(String name, String entryPrefix, boolean indexed, List<String> extraLibs)
          throws IOException {
    return createJarArchive(name, null, entryPrefix, indexed, extraLibs);
  }

  @SuppressWarnings("resource")
  protected File createJarArchive(String name, Manifest manifest, String entryPrefix, boolean indexed,
          List<String> extraLibs) throws IOException {
    File archive = new File(this.tempDir, name);
    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(archive));
    if (manifest != null) {
      jarOutputStream.putNextEntry(new JarEntry("META-INF/"));
      jarOutputStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
      manifest.write(jarOutputStream);
      jarOutputStream.closeEntry();
    }
    jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/"));
    jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/classes/"));
    jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/lib/"));
    if (indexed) {
      jarOutputStream.putNextEntry(new JarEntry(entryPrefix + "/classpath.idx"));
      Writer writer = new OutputStreamWriter(jarOutputStream, StandardCharsets.UTF_8);
      writer.write("- \"" + entryPrefix + "/lib/foo.jar\"\n");
      writer.write("- \"" + entryPrefix + "/lib/bar.jar\"\n");
      writer.write("- \"" + entryPrefix + "/lib/baz.jar\"\n");
      writer.flush();
      jarOutputStream.closeEntry();
    }
    addNestedJars(entryPrefix, "/lib/foo.jar", jarOutputStream);
    addNestedJars(entryPrefix, "/lib/bar.jar", jarOutputStream);
    addNestedJars(entryPrefix, "/lib/baz.jar", jarOutputStream);
    for (String lib : extraLibs) {
      addNestedJars(entryPrefix, "/lib/" + lib, jarOutputStream);
    }
    jarOutputStream.close();
    return archive;
  }

  private void addNestedJars(String entryPrefix, String lib, JarOutputStream jarOutputStream) throws IOException {
    JarEntry libFoo = new JarEntry(entryPrefix + lib);
    libFoo.setMethod(ZipEntry.STORED);
    ByteArrayOutputStream fooJarStream = new ByteArrayOutputStream();
    new JarOutputStream(fooJarStream).close();
    libFoo.setSize(fooJarStream.size());
    CRC32 crc32 = new CRC32();
    crc32.update(fooJarStream.toByteArray());
    libFoo.setCrc(crc32.getValue());
    jarOutputStream.putNextEntry(libFoo);
    jarOutputStream.write(fooJarStream.toByteArray());
  }

  protected File explode(File archive) throws IOException {
    File exploded = new File(this.tempDir, "exploded");
    exploded.mkdirs();
    JarFile jarFile = new JarFile(archive);
    Enumeration<JarEntry> entries = jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      File entryFile = new File(exploded, entry.getName());
      if (entry.isDirectory()) {
        entryFile.mkdirs();
      }
      else {
        FileCopyUtils.copy(jarFile.getInputStream(entry), new FileOutputStream(entryFile));
      }
    }
    jarFile.close();
    return exploded;
  }

  protected Set<URL> getUrls(List<Archive> archives) throws MalformedURLException {
    Set<URL> urls = new LinkedHashSet<>(archives.size());
    for (Archive archive : archives) {
      urls.add(archive.getUrl());
    }
    return urls;
  }

  protected final URL toUrl(File file) {
    try {
      return file.toURI().toURL();
    }
    catch (MalformedURLException ex) {
      throw new IllegalStateException(ex);
    }
  }

}
