/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aot.generate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.InputStreamSource;
import cn.taketoday.util.function.ThrowingConsumer;

/**
 * Adapter class to convert a {@link ThrowingConsumer} of {@link Appendable} to
 * an {@link InputStreamSource}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class AppendableConsumerInputStreamSource implements InputStreamSource {

  private final ThrowingConsumer<Appendable> content;

  AppendableConsumerInputStreamSource(ThrowingConsumer<Appendable> content) {
    this.content = content;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(toString().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    this.content.accept(buffer);
    return buffer.toString();
  }

}
