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

package cn.taketoday.web.server.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import cn.taketoday.web.multipart.MultipartFile;
import cn.taketoday.web.multipart.support.AbstractMultipartFile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.multipart.FileUpload;

/**
 * Netty MultipartFile
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FileUpload
 * @since 2019-11-14 13:11
 */
final class NettyMultipartFile extends AbstractMultipartFile implements MultipartFile {

  private final FileUpload fileUpload;

  public NettyMultipartFile(FileUpload fileUpload) {
    this.fileUpload = fileUpload;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (fileUpload.isInMemory()) {
      ByteBuf byteBuf = fileUpload.getByteBuf();
      byteBuf.resetReaderIndex();
      return new ByteBufInputStream(byteBuf);
    }

    return new FileInputStream(fileUpload.getFile());
  }

  @Override
  public String getContentType() {
    return fileUpload.getContentType();
  }

  @Override
  public long getSize() {
    return fileUpload.length();
  }

  @Override
  public String getName() {
    return fileUpload.getName();
  }

  @Override
  public String getOriginalFilename() {
    return fileUpload.getFilename();
  }

  @Override
  protected void saveInternal(File dest) throws IOException {
    fileUpload.renameTo(dest);
  }

  @Override
  public long transferTo(OutputStream out) throws IOException {
    if (fileUpload.isInMemory()) {
      ByteBuf byteBuf = fileUpload.getByteBuf();
      int length = byteBuf.readableBytes();
      byteBuf.readBytes(out, length);
      return length;
    }

    try (var in = new FileInputStream(fileUpload.getFile())) {
      return in.transferTo(out);
    }
  }

  @Override
  public long transferTo(FileChannel out, long position, long count) throws IOException {
    if (fileUpload.isInMemory()) {
      // int is ok, you cannot save more than 4GB data in memory
      return fileUpload.getByteBuf().readBytes(out, position, Math.toIntExact(count));
    }

    try (var channel = FileChannel.open(fileUpload.getFile().toPath())) {
      return channel.transferTo(position, count, out);
    }
  }

  @Override
  public void transferTo(Path dest) throws IOException, IllegalStateException {
    try (var channel = FileChannel.open(dest, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
      transferTo(channel, 0, fileUpload.length());
    }
  }

  @Override
  public boolean isEmpty() {
    return getSize() == 0;
  }

  @Override
  protected byte[] doGetBytes() throws IOException {
    return fileUpload.get();
  }

  @Override
  public Object getOriginalResource() {
    return fileUpload;
  }

  @Override
  protected void deleteInternal() {
    fileUpload.delete();
  }

}
