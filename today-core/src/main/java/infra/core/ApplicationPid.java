/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.core;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import infra.lang.Assert;
import infra.util.ObjectUtils;

/**
 * An application process ID.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 11:15
 */
public class ApplicationPid {

  private static final PosixFilePermission[] WRITE_PERMISSIONS = {
          PosixFilePermission.OWNER_WRITE,
          PosixFilePermission.GROUP_WRITE,
          PosixFilePermission.OTHERS_WRITE
  };

  @Nullable
  private final Long pid;

  public ApplicationPid() {
    this.pid = currentProcessPid();
  }

  protected ApplicationPid(@Nullable Long pid) {
    this.pid = pid;
  }

  @Nullable
  private Long currentProcessPid() {
    try {
      return ProcessHandle.current().pid();
    }
    catch (Throwable ex) {
      return null;
    }
  }

  /**
   * Return if the application PID is available.
   *
   * @return {@code true} if the PID is available
   * @since 5.0
   */
  public boolean isAvailable() {
    return this.pid != null;
  }

  /**
   * Return the application PID as a {@link Long}.
   *
   * @return the application PID or {@code null}
   */
  @Nullable
  public Long toLong() {
    return this.pid;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ApplicationPid other) {
      return ObjectUtils.nullSafeEquals(this.pid, other.pid);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.pid);
  }

  @Override
  public String toString() {
    return (this.pid != null) ? String.valueOf(this.pid) : "???";
  }

  /**
   * Write the PID to the specified file.
   *
   * @param file the PID file
   * @throws IllegalStateException if no PID is available.
   * @throws IOException if the file cannot be written
   */
  public void write(File file) throws IOException {
    Assert.state(this.pid != null, "No PID available");
    createParentDirectory(file);
    if (file.exists()) {
      assertCanOverwrite(file);
    }
    try (FileWriter writer = new FileWriter(file)) {
      writer.append(String.valueOf(this.pid));
    }
  }

  private void createParentDirectory(File file) {
    File parent = file.getParentFile();
    if (parent != null) {
      parent.mkdirs();
    }
  }

  private void assertCanOverwrite(File file) throws IOException {
    if (!file.canWrite() || !canWritePosixFile(file)) {
      throw new FileNotFoundException(file + " (permission denied)");
    }
  }

  private boolean canWritePosixFile(File file) throws IOException {
    try {
      Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file.toPath());
      for (PosixFilePermission permission : WRITE_PERMISSIONS) {
        if (permissions.contains(permission)) {
          return true;
        }
      }
      return false;
    }
    catch (UnsupportedOperationException ex) {
      // Assume that we can
      return true;
    }
  }

}
