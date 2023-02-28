/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 11:15
 */
public class ApplicationPid {

  private static final PosixFilePermission[] WRITE_PERMISSIONS = {
          PosixFilePermission.OWNER_WRITE,
          PosixFilePermission.GROUP_WRITE,
          PosixFilePermission.OTHERS_WRITE
  };

  private final String pid;

  public ApplicationPid() {
    this.pid = getPid();
  }

  protected ApplicationPid(String pid) {
    this.pid = pid;
  }

  private String getPid() {
    try {
      String jvmName = ManagementFactory.getRuntimeMXBean().getName();
      return jvmName.split("@")[0];
    }
    catch (Throwable ex) {
      return null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof ApplicationPid) {
      return ObjectUtils.nullSafeEquals(this.pid, ((ApplicationPid) obj).pid);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.pid);
  }

  @Override
  public String toString() {
    return (this.pid != null) ? this.pid : "???";
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
      writer.append(this.pid);
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
