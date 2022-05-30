/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-05-15 10:20
 * @since 2.1.6
 */
public class JarEntryResource extends UrlBasedResource implements JarResource {

  private final String name;
  private final File jarFile;

  // @since 4.0
  private JarFile jar;

  public JarEntryResource(URL url) {
    this(url, new File(getJarFilePath(url.getPath())), getJarEntryName(url.getPath()));
  }

  public JarEntryResource(String path) throws IOException {
    this(new URL(getJarUrl(path)), new File(getJarFilePath(path)), getJarEntryName(path));
  }

  public JarEntryResource(URL url, File jarFile, String name) {
    super(url);
    Assert.notNull(name, "name must not be null");
    Assert.notNull(jarFile, "name must not be null");
    this.name = name;
    this.jarFile = jarFile;
  }

  JarEntryResource(URL url, File jarFile, String name, JarFile jar) {
    super(url);
    this.jar = jar;
    this.name = name;
    this.jarFile = jarFile;
  }

  protected static String getJarUrl(String path) {
    if (path.startsWith(ResourceUtils.JAR_ENTRY_URL_PREFIX)) {
      return path;
    }
    String concat = ResourceUtils.JAR_ENTRY_URL_PREFIX.concat(path);
    if (concat.endsWith(ResourceUtils.JAR_URL_SEPARATOR)) {
      return concat;
    }
    return concat.concat(ResourceUtils.JAR_URL_SEPARATOR);
  }

  protected static String getJarFilePath(String path) {

    int indexOf = path.indexOf(ResourceUtils.JAR_SEPARATOR);
    if (path.startsWith("file:")) { // fix #11 jar file not found
      return indexOf == -1 ? path.substring(5) : path.substring(5, indexOf);
    }
    // jar:file:/xxxxxx.jar!/x
    return indexOf == -1 ? path : path.substring(0, indexOf);
  }

  private static String getJarEntryName(String path) {
    int indexOf = path.indexOf(ResourceUtils.JAR_SEPARATOR);
    if (indexOf == -1) {
      return Constant.BLANK;
    }
    if (path.charAt(0) == Constant.PATH_SEPARATOR) {
      return path.substring(1);
    }
    // jar:file:/xxxxxx.jar!/x
    return path.substring(indexOf + 2);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (name.isEmpty()) {
      return new FileInputStream(jarFile);
    }

    JarFile jarFile = getJarFile();
    InputStream inputStream = jarFile.getInputStream(jarFile.getEntry(name));
    return new JarEntryInputStream(inputStream, jarFile);
  }

  @Override
  public JarOutputStream getOutputStream() throws IOException {
    return new JarOutputStream(Files.newOutputStream(getFile().toPath()));
  }

  @Override
  public File getFile() {
    return jarFile;
  }

  @Override
  public boolean exists() {
    if (name.isEmpty()) {
      return jarFile.exists();
    }
    try (JarFile jarFile = getJarFile()) {
      return jarFile.getEntry(name) != null;
    }
    catch (IOException e) {
      return false;
    }
  }

  @Override
  public boolean isDirectory() throws IOException {
    try (JarFile jarFile = getJarFile()) {
      return jarFile.getEntry(name).isDirectory();
    }
  }

  @Override
  public String[] list() throws IOException {
    try (JarFile jarFile = getJarFile()) {

      String name = this.name;
      Set<String> result = new HashSet<>();
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        JarEntry jarEntry = entries.nextElement();
        String entryName = jarEntry.getName();
        if (!entryName.equals(name) && entryName.startsWith(name)) {
          String substring = entryName.substring(name.length());
          int index = substring.indexOf(Constant.PATH_SEPARATOR);

          if (index > -1) { // is dir
            result.add(substring.substring(0, index));
          }
          else {
            result.add(substring);
          }
        }
      }
      if (result.isEmpty()) {
        return Constant.EMPTY_STRING_ARRAY;
      }
      return StringUtils.toStringArray(result);
    }
  }

  @Override
  public JarEntryResource createRelative(String relativePath) throws IOException {
    URL url = new URL(getURL(), relativePath);
    String path = ResourceUtils.getRelativePath(name, relativePath);
    return new JarEntryResource(url, getFile(), path, jar);
  }

  @Override
  public String toString() {
    return "JarEntryResource: ".concat(getURL().toString());
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof JarEntryResource) {
      return Objects.equals(((JarEntryResource) other).name, name)
              && Objects.equals(((JarEntryResource) other).jarFile, jarFile);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), name, jarFile);
  }
//
//  @Override
//  public JarFile getJarFile() throws IOException {
//    if (jar == null) {
//      jar = new JarFile(getFile());
//    }
//    return jar;
//  }

  @Override
  public void close() throws Exception {
    if (jar != null) {
      jar.close();
      jar = null;
    }
  }

  private static class JarEntryInputStream extends FilterInputStream {

    private final JarFile jarFile;

    protected JarEntryInputStream(InputStream in, JarFile jarFile) {
      super(in);
      this.jarFile = jarFile;
    }

    @Override
    public void close() throws IOException {
      in.close();
      jarFile.close();
    }
  }

}
