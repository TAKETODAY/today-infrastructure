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

import org.junit.jupiter.api.Test;

import cn.taketoday.buildpack.platform.socket.FileDescriptor.Handle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FileDescriptor}.
 *
 * @author Phillip Webb
 */
class FileDescriptorTests {

  private final int sourceHandle = 123;

  private int closedHandle = 0;

  @Test
  void acquireReturnsHandle() throws Exception {
    FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
    try (Handle handle = descriptor.acquire()) {
      assertThat(handle.intValue()).isEqualTo(this.sourceHandle);
      assertThat(handle.isClosed()).isFalse();
    }
  }

  @Test
  void acquireWhenClosedReturnsClosedHandle() throws Exception {
    FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
    descriptor.close();
    try (Handle handle = descriptor.acquire()) {
      assertThat(handle.intValue()).isEqualTo(-1);
      assertThat(handle.isClosed()).isTrue();
    }
  }

  @Test
  void acquireWhenPendingCloseReturnsClosedHandle() throws Exception {
    FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
    try (Handle handle1 = descriptor.acquire()) {
      descriptor.close();
      try (Handle handle2 = descriptor.acquire()) {
        assertThat(handle2.intValue()).isEqualTo(-1);
        assertThat(handle2.isClosed()).isTrue();
      }
    }
  }

  @Test
  void finalizeTriggersClose() {
    FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
    descriptor.close();
    assertThat(this.closedHandle).isEqualTo(this.sourceHandle);
  }

  @Test
  void closeWhenHandleAcquiredClosesOnRelease() throws Exception {
    FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
    try (Handle handle = descriptor.acquire()) {
      descriptor.close();
      assertThat(this.closedHandle).isZero();
    }
    assertThat(this.closedHandle).isEqualTo(this.sourceHandle);
  }

  @Test
  void closeWhenHandleNotAcquiredClosesImmediately() {
    FileDescriptor descriptor = new FileDescriptor(this.sourceHandle, this::close);
    descriptor.close();
    assertThat(this.closedHandle).isEqualTo(this.sourceHandle);
  }

  private void close(int handle) {
    this.closedHandle = handle;
  }

}
