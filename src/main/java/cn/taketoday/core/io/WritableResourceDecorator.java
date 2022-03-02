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

package cn.taketoday.core.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.WritableByteChannel;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/2 10:26
 */
public class WritableResourceDecorator extends ResourceDecorator implements WritableResource {

  protected WritableResourceDecorator() { }

  public WritableResourceDecorator(Resource delegate) {
    super(delegate);
  }

  // WritableResource

  @Override
  public OutputStream getOutputStream() throws IOException {
    return writableResource().getOutputStream();
  }

  @Override
  public Writer getWriter() throws IOException {
    return writableResource().getWriter();
  }

  @Override
  public WritableByteChannel writableChannel() throws IOException {
    return writableResource().writableChannel();
  }

  @Override
  public boolean isWritable() {
    return writableResource().isWritable();
  }

  /**
   * @throws UnsupportedOperationException not a WritableResource
   */
  protected WritableResource writableResource() {
    Resource delegate = getDelegate();
    if (delegate instanceof WritableResource writableResource) {
      return writableResource;
    }
    throw new UnsupportedOperationException("Writable operation is not supported");
  }

}
