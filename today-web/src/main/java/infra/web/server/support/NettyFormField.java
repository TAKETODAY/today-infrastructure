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

package infra.web.server.support;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import infra.http.MediaType;
import infra.util.FileCopyUtils;
import infra.web.multipart.support.AbstractPart;
import io.netty.handler.codec.http.multipart.Attribute;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/3 22:50
 */
public class NettyFormField extends AbstractPart {

  private final Attribute attribute;

  NettyFormField(Attribute attribute) {
    this.attribute = attribute;
  }

  @Override
  public long getContentLength() {
    return attribute.length();
  }

  @Override
  public byte[] getContentAsByteArray() throws IOException {
    return attribute.get();
  }

  @Override
  public String getContentAsString() throws IOException {
    return getContentAsString(StandardCharsets.UTF_8);
  }

  @Override
  public String getContentAsString(@Nullable Charset charset) throws IOException {
    return new String(getContentAsByteArray(), charset == null ? StandardCharsets.UTF_8 : charset);
  }

  @Override
  public boolean isInMemory() {
    return attribute.isInMemory();
  }

  @Override
  public boolean isEmpty() {
    return attribute.length() == 0L;
  }

  @Override
  public boolean isFormField() {
    return true;
  }

  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public @Nullable String getOriginalFilename() {
    return null;
  }

  @Override
  public long transferTo(File dest) throws IOException, IllegalStateException {
    FileCopyUtils.copy(getContentAsByteArray(), dest);
    return getContentLength();
  }

  @Override
  public long transferTo(Path dest) throws IOException, IllegalStateException {
    return transferTo(dest.toFile());
  }

  @Override
  public long transferTo(FileChannel dest, long position) throws IOException {
    return dest.write(ByteBuffer.wrap(getContentAsByteArray()), position);
  }

  @Override
  public long transferTo(FileChannel dest, long position, long count) throws IOException {
    return dest.write(ByteBuffer.wrap(getContentAsByteArray(), 0, Math.toIntExact(count)), position);
  }

  @Override
  public String getName() {
    return attribute.getName();
  }

  @Override
  public @Nullable MediaType getContentType() {
    return null;
  }

  @Override
  public void cleanup() throws IOException {
    attribute.delete();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(getContentAsByteArray());
  }
}
