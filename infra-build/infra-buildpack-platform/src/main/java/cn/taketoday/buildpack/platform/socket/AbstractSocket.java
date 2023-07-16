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
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Abstract base class for custom socket implementation.
 *
 * @author Phillip Webb
 */
class AbstractSocket extends Socket {

  @Override
  public void connect(SocketAddress endpoint) throws IOException {
  }

  @Override
  public void connect(SocketAddress endpoint, int timeout) throws IOException {
  }

  @Override
  public boolean isConnected() {
    return true;
  }

  @Override
  public boolean isBound() {
    return true;
  }

  @Override
  public void shutdownInput() throws IOException {
    throw new UnsupportedSocketOperationException();
  }

  @Override
  public void shutdownOutput() throws IOException {
    throw new UnsupportedSocketOperationException();
  }

  @Override
  public InetAddress getInetAddress() {
    return null;
  }

  @Override
  public InetAddress getLocalAddress() {
    return null;
  }

  @Override
  public SocketAddress getLocalSocketAddress() {
    return null;
  }

  @Override
  public SocketAddress getRemoteSocketAddress() {
    return null;
  }

  private static class UnsupportedSocketOperationException extends UnsupportedOperationException {

    UnsupportedSocketOperationException() {
      super("Unsupported socket operation");
    }

  }

}
