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

package cn.taketoday.buildpack.platform.socket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;

import cn.taketoday.lang.Assert;

/**
 * {@link DomainSocket} implementation for BSD based platforms.
 *
 * @author Phillip Webb
 */
class BsdDomainSocket extends DomainSocket {

  private static final int MAX_PATH_LENGTH = 104;

  static {
    Native.register(Platform.C_LIBRARY_NAME);
  }

  BsdDomainSocket(String path) throws IOException {
    super(path);
  }

  @Override
  protected void connect(String path, int handle) {
    SockaddrUn address = new SockaddrUn(AF_LOCAL, path.getBytes(StandardCharsets.UTF_8));
    connect(handle, address, address.size());
  }

  private native int connect(int fd, SockaddrUn address, int addressLen) throws LastErrorException;

  /**
   * Native {@code sockaddr_un} structure as defined in {@code sys/un.h}.
   */
  public static class SockaddrUn extends Structure implements Structure.ByReference {

    public byte sunLen;

    public byte sunFamily;

    public byte[] sunPath = new byte[MAX_PATH_LENGTH];

    private SockaddrUn(byte sunFamily, byte[] path) {
      Assert.isTrue(path.length < MAX_PATH_LENGTH, () -> "Path cannot exceed " + MAX_PATH_LENGTH + " bytes");
      System.arraycopy(path, 0, this.sunPath, 0, path.length);
      this.sunPath[path.length] = 0;
      this.sunLen = (byte) (fieldOffset("sunPath") + path.length);
      this.sunFamily = sunFamily;
      allocateMemory();
    }

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(new String[] { "sunLen", "sunFamily", "sunPath" });
    }

  }

}
