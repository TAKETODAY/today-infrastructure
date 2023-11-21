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

package cn.taketoday.buildpack.platform.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collection;

import cn.taketoday.lang.Assert;

/**
 * Utilities for dealing with file permissions and attributes.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class FilePermissions {

  private FilePermissions() {
  }

  /**
   * Return the integer representation of the file permissions for a path, where the
   * integer value conforms to the
   * <a href="https://en.wikipedia.org/wiki/Umask">umask</a> octal notation.
   *
   * @param path the file path
   * @return the integer representation
   * @throws IOException if path permissions cannot be read
   */
  public static int umaskForPath(Path path) throws IOException {
    Assert.notNull(path, "Path is required");
    PosixFileAttributeView attributeView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
    Assert.state(attributeView != null, "Unsupported file type for retrieving Posix attributes");
    return posixPermissionsToUmask(attributeView.readAttributes().permissions());
  }

  /**
   * Return the integer representation of a set of Posix file permissions, where the
   * integer value conforms to the
   * <a href="https://en.wikipedia.org/wiki/Umask">umask</a> octal notation.
   *
   * @param permissions the set of {@code PosixFilePermission}s
   * @return the integer representation
   */
  public static int posixPermissionsToUmask(Collection<PosixFilePermission> permissions) {
    Assert.notNull(permissions, "Permissions is required");
    int owner = permissionToUmask(permissions, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_READ);
    int group = permissionToUmask(permissions, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_READ);
    int other = permissionToUmask(permissions, PosixFilePermission.OTHERS_EXECUTE, PosixFilePermission.OTHERS_WRITE,
            PosixFilePermission.OTHERS_READ);
    return Integer.parseInt("" + owner + group + other, 8);
  }

  private static int permissionToUmask(Collection<PosixFilePermission> permissions, PosixFilePermission execute,
          PosixFilePermission write, PosixFilePermission read) {
    int value = 0;
    if (permissions.contains(execute)) {
      value += 1;
    }
    if (permissions.contains(write)) {
      value += 2;
    }
    if (permissions.contains(read)) {
      value += 4;
    }
    return value;
  }

}
