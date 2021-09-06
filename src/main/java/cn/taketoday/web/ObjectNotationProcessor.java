/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import cn.taketoday.util.TypeDescriptor;

/**
 * @author TODAY 2021/5/17 13:07
 * @since 3.0.1
 */
public abstract class ObjectNotationProcessor {

  /** for write string */
  private Charset charset = StandardCharsets.UTF_8;

  /**
   * Write message to Writer
   *
   * @param output
   *         output writer
   * @param message
   *         The message to write
   *
   * @throws IOException
   *         If any input output exception occurred
   */
  public abstract void write(Writer output, Object message) throws IOException;

  /**
   * Write message to output-stream
   *
   * @param output
   *         output-stream
   * @param message
   *         The message to write
   *
   * @throws IOException
   *         If any input output exception occurred
   */
  public void write(OutputStream output, Object message) throws IOException {
    final OutputStreamWriter writer = new OutputStreamWriter(output, charset);
    write(writer, message);
    output.flush();
  }

  /**
   * read a object from given string message
   *
   * @param message
   *         string message
   * @param descriptor
   *         type descriptor
   *
   * @throws IOException
   *         if underlying input contains invalid content
   */
  public abstract Object read(String message, TypeDescriptor descriptor) throws IOException;

  /**
   * read an object from given InputStream
   *
   * @param source
   *         source message stream
   * @param descriptor
   *         type descriptor
   *
   * @throws IOException
   *         If a low-level I/O problem (missing input, network error) occurs,
   *         a {@link IOException} will be thrown.
   *         If a parsing problem occurs (invalid JSON)
   */
  public abstract Object read(InputStream source, TypeDescriptor descriptor) throws IOException;

  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  public Charset getCharset() {
    return charset;
  }
}
