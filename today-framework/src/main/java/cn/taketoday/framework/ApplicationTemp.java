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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.EnumSet;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Provides access to an application specific temporary directory. Generally speaking
 * different Framework applications will get different locations, however, simply
 * restarting an application will give the same location.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:28
 */
public class ApplicationTemp {

  private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

  private static final FileAttribute<?>[] NO_FILE_ATTRIBUTES = {};

  private static final EnumSet<PosixFilePermission> DIRECTORY_PERMISSIONS = EnumSet.of(PosixFilePermission.OWNER_READ,
          PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);

  private final Class<?> sourceClass;

  private volatile Path path;

  /**
   * Create a new {@link ApplicationTemp} instance.
   */
  public ApplicationTemp() {
    this(null);
  }

  /**
   * Create a new {@link ApplicationTemp} instance for the specified source class.
   *
   * @param sourceClass the source class or {@code null}
   */
  public ApplicationTemp(Class<?> sourceClass) {
    this.sourceClass = sourceClass;
  }

  @Override
  public String toString() {
    return getDir().getAbsolutePath();
  }

  /**
   * Return the directory to be used for application specific temp files.
   *
   * @return the application temp directory
   */
  public File getDir() {
    return getPath().toFile();
  }

  /**
   * Return a sub-directory of the application temp.
   *
   * @param subDir the sub-directory name
   * @return a sub-directory
   */
  public File getDir(String subDir) {
    return createDirectory(getPath().resolve(subDir)).toFile();
  }

  private Path getPath() {
    if (this.path == null) {
      synchronized(this) {
        String hash = toHexString(generateHash(this.sourceClass));
        this.path = createDirectory(getTempDirectory().resolve(hash));
      }
    }
    return this.path;
  }

  private Path createDirectory(Path path) {
    try {
      if (!Files.exists(path)) {
        Files.createDirectory(path, getFileAttributes(path.getFileSystem(), DIRECTORY_PERMISSIONS));
      }
      return path;
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to create application temp directory " + path, ex);
    }
  }

  private FileAttribute<?>[] getFileAttributes(FileSystem fileSystem, EnumSet<PosixFilePermission> ownerReadWrite) {
    if (!fileSystem.supportedFileAttributeViews().contains("posix")) {
      return NO_FILE_ATTRIBUTES;
    }
    return new FileAttribute<?>[] { PosixFilePermissions.asFileAttribute(ownerReadWrite) };
  }

  private Path getTempDirectory() {
    String property = System.getProperty("java.io.tmpdir");
    Assert.state(StringUtils.isNotEmpty(property), "No 'java.io.tmpdir' property set");
    Path tempDirectory = Paths.get(property);
    Assert.state(Files.exists(tempDirectory), () -> "Temp directory '" + tempDirectory + "' does not exist");
    Assert.state(Files.isDirectory(tempDirectory),
            () -> "Temp location '" + tempDirectory + "' is not a directory");
    return tempDirectory;
  }

  private byte[] generateHash(Class<?> sourceClass) {
    ApplicationHome home = new ApplicationHome(sourceClass);

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      update(digest, home.getSource());
      update(digest, home.getDir());
      update(digest, System.getProperty("user.dir"));
      update(digest, System.getProperty("java.home"));
      update(digest, System.getProperty("java.class.path"));
      update(digest, System.getProperty("sun.java.command"));
      update(digest, System.getProperty("sun.boot.class.path"));
      return digest.digest();
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  private void update(MessageDigest digest, Object source) {
    if (source != null) {
      digest.update(getUpdateSourceBytes(source));
    }
  }

  private byte[] getUpdateSourceBytes(Object source) {
    if (source instanceof File) {
      return getUpdateSourceBytes(((File) source).getAbsolutePath());
    }
    return source.toString().getBytes();
  }

  private String toHexString(byte[] bytes) {
    char[] hex = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i] & 0xFF;
      hex[i * 2] = HEX_CHARS[b >>> 4];
      hex[i * 2 + 1] = HEX_CHARS[b & 0x0F];
    }
    return new String(hex);
  }

}
