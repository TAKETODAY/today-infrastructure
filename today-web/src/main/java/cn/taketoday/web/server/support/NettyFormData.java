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

import java.io.IOException;

import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.multipart.support.AbstractMultipart;
import io.netty.handler.codec.http.multipart.Attribute;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/3 22:50
 */
public class NettyFormData extends AbstractMultipart {

  private final Attribute attribute;

  NettyFormData(Attribute attribute) {
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
  public byte[] getBytes() throws IOException {
    return attribute.get();
  }

  @Override
  public boolean isFormField() {
    return true;
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
