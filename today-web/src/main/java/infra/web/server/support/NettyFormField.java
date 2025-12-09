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

import java.io.IOException;

import infra.util.ExceptionUtils;
import infra.web.multipart.FormField;
import infra.web.multipart.support.AbstractPart;
import io.netty.handler.codec.http.multipart.Attribute;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/3 22:50
 */
public class NettyFormField extends AbstractPart implements FormField {

  private final Attribute attribute;

  NettyFormField(Attribute attribute) {
    this.attribute = attribute;
  }

  @Override
  public String getValue() {
    try {
      return attribute.getValue();
    }
    catch (IOException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
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
  public boolean isFormField() {
    return true;
  }

  @Override
  public @Nullable String getOriginalFilename() {
    return null;
  }

  @Override
  public String getName() {
    return attribute.getName();
  }

  @Override
  public void cleanup() throws IOException {
    attribute.delete();
  }

}
