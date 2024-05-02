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

package cn.taketoday.app.loader.testsupport;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * Support class to create or get test jars.
 *
 * @author Phillip Webb
 */
public abstract class TestJar {

  public static final int MULTI_JAR_VERSION = Runtime.version().feature();

  private static final int BASE_VERSION = 8;

  public static void create(File file) throws Exception {
    create(file, false);
  }

  public static void create(File file, boolean unpackNested) throws Exception {
    create(file, unpackNested, false);
  }

  public static void create(File file, boolean unpackNested, boolean addSignatureFile) throws Exception {
    FileOutputStream fileOutputStream = new FileOutputStream(file);
    try (JarOutputStream jarOutputStream = new JarOutputStream(fileOutputStream)) {
      jarOutputStream.setComment("outer");
      writeManifest(jarOutputStream, "j1");
      if (addSignatureFile) {
        writeEntry(jarOutputStream, "META-INF/some.DSA", 0);
      }
      writeEntry(jarOutputStream, "1.dat", 1);
      writeEntry(jarOutputStream, "2.dat", 2);
      writeDirEntry(jarOutputStream, "d/");
      writeEntry(jarOutputStream, "d/9.dat", 9);
      writeDirEntry(jarOutputStream, "special/");
      writeEntry(jarOutputStream, "special/\u00EB.dat", '\u00EB');
      writeNestedEntry("nested.jar", unpackNested, jarOutputStream);
      writeNestedEntry("another-nested.jar", unpackNested, jarOutputStream);
      writeNestedEntry("space nested.jar", unpackNested, jarOutputStream);
      writeNestedMultiReleaseEntry("multi-release.jar", unpackNested, jarOutputStream);
    }
  }

  public static List<String> expectedEntries() {
    return List.of("META-INF/", "META-INF/MANIFEST.MF", "1.dat", "2.dat", "d/", "d/9.dat", "special/",
            "special/\u00EB.dat", "nested.jar", "another-nested.jar", "space nested.jar", "multi-release.jar");
  }

  private static void writeNestedEntry(String name, boolean unpackNested, JarOutputStream jarOutputStream)
          throws Exception {
    writeNestedEntry(name, unpackNested, jarOutputStream, false);
  }

  private static void writeNestedMultiReleaseEntry(String name, boolean unpackNested, JarOutputStream jarOutputStream)
          throws Exception {
    writeNestedEntry(name, unpackNested, jarOutputStream, true);
  }

  private static void writeNestedEntry(String name, boolean unpackNested, JarOutputStream jarOutputStream,
          boolean multiRelease) throws Exception {
    JarEntry nestedEntry = new JarEntry(name);
    byte[] nestedJarData = getNestedJarData(multiRelease);
    nestedEntry.setSize(nestedJarData.length);
    nestedEntry.setCompressedSize(nestedJarData.length);
    if (unpackNested) {
      nestedEntry.setComment("UNPACK:0000000000000000000000000000000000000000");
    }
    CRC32 crc32 = new CRC32();
    crc32.update(nestedJarData);
    nestedEntry.setCrc(crc32.getValue());
    nestedEntry.setMethod(ZipEntry.STORED);
    jarOutputStream.putNextEntry(nestedEntry);
    jarOutputStream.write(nestedJarData);
    jarOutputStream.closeEntry();
  }

  private static byte[] getNestedJarData(boolean multiRelease) throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    JarOutputStream jarOutputStream = new JarOutputStream(byteArrayOutputStream);
    jarOutputStream.setComment("nested");
    writeManifest(jarOutputStream, "j2", multiRelease);
    if (multiRelease) {
      writeEntry(jarOutputStream, "multi-release.dat", BASE_VERSION);
      writeEntry(jarOutputStream, String.format("META-INF/versions/%d/multi-release.dat", MULTI_JAR_VERSION),
              MULTI_JAR_VERSION);
    }
    else {
      writeEntry(jarOutputStream, "3.dat", 3);
      writeEntry(jarOutputStream, "4.dat", 4);
      writeEntry(jarOutputStream, "\u00E4.dat", '\u00E4');
    }
    jarOutputStream.close();
    return byteArrayOutputStream.toByteArray();
  }

  private static void writeManifest(JarOutputStream jarOutputStream, String name) throws Exception {
    writeManifest(jarOutputStream, name, false);
  }

  private static void writeManifest(JarOutputStream jarOutputStream, String name, boolean multiRelease)
          throws Exception {
    writeDirEntry(jarOutputStream, "META-INF/");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().putValue("Built-By", name);
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (multiRelease) {
      manifest.getMainAttributes().putValue("Multi-Release", Boolean.toString(true));
    }
    jarOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
    manifest.write(jarOutputStream);
    jarOutputStream.closeEntry();
  }

  private static void writeDirEntry(JarOutputStream jarOutputStream, String name) throws IOException {
    jarOutputStream.putNextEntry(new JarEntry(name));
    jarOutputStream.closeEntry();
  }

  private static void writeEntry(JarOutputStream jarOutputStream, String name, int data) throws IOException {
    jarOutputStream.putNextEntry(new JarEntry(name));
    jarOutputStream.write(new byte[] { (byte) data });
    jarOutputStream.closeEntry();
  }

  public static File getSigned() {
    String[] entries = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
    for (String entry : entries) {
      if (entry.contains("bcprov")) {
        return new File(entry);
      }
    }
    return null;
  }

}
