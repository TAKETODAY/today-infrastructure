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

package cn.taketoday.web.testfixture.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import cn.taketoday.lang.Nullable;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.jsp.JspWriter;

/**
 * Mock implementation of the {@link JspWriter} class.
 * Only necessary for testing applications when testing custom JSP tags.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class MockJspWriter extends JspWriter {

  private final HttpServletResponse response;

  @Nullable
  private PrintWriter targetWriter;

  /**
   * Create a MockJspWriter for the given response,
   * using the response's default Writer.
   *
   * @param response the servlet response to wrap
   */
  public MockJspWriter(HttpServletResponse response) {
    this(response, null);
  }

  /**
   * Create a MockJspWriter for the given plain Writer.
   *
   * @param targetWriter the target Writer to wrap
   */
  public MockJspWriter(Writer targetWriter) {
    this(null, targetWriter);
  }

  /**
   * Create a MockJspWriter for the given response.
   *
   * @param response the servlet response to wrap
   * @param targetWriter the target Writer to wrap
   */
  public MockJspWriter(@Nullable HttpServletResponse response, @Nullable Writer targetWriter) {
    super(DEFAULT_BUFFER, true);
    this.response = (response != null ? response : new MockHttpServletResponse());
    if (targetWriter instanceof PrintWriter) {
      this.targetWriter = (PrintWriter) targetWriter;
    }
    else if (targetWriter != null) {
      this.targetWriter = new PrintWriter(targetWriter);
    }
  }

  /**
   * Lazily initialize the target Writer.
   */
  protected PrintWriter getTargetWriter() throws IOException {
    if (this.targetWriter == null) {
      this.targetWriter = this.response.getWriter();
    }
    return this.targetWriter;
  }

  @Override
  public void clear() throws IOException {
    if (this.response.isCommitted()) {
      throw new IOException("Response already committed");
    }
    this.response.resetBuffer();
  }

  @Override
  public void clearBuffer() throws IOException {
  }

  @Override
  public void flush() throws IOException {
    this.response.flushBuffer();
  }

  @Override
  public void close() throws IOException {
    flush();
  }

  @Override
  public int getRemaining() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void newLine() throws IOException {
    getTargetWriter().println();
  }

  @Override
  public void write(char[] value, int offset, int length) throws IOException {
    getTargetWriter().write(value, offset, length);
  }

  @Override
  public void print(boolean value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void print(char value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void print(char[] value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void print(double value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void print(float value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void print(int value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void print(long value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void print(Object value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void print(String value) throws IOException {
    getTargetWriter().print(value);
  }

  @Override
  public void println() throws IOException {
    getTargetWriter().println();
  }

  @Override
  public void println(boolean value) throws IOException {
    getTargetWriter().println(value);
  }

  @Override
  public void println(char value) throws IOException {
    getTargetWriter().println(value);
  }

  @Override
  public void println(char[] value) throws IOException {
    getTargetWriter().println(value);
  }

  @Override
  public void println(double value) throws IOException {
    getTargetWriter().println(value);
  }

  @Override
  public void println(float value) throws IOException {
    getTargetWriter().println(value);
  }

  @Override
  public void println(int value) throws IOException {
    getTargetWriter().println(value);
  }

  @Override
  public void println(long value) throws IOException {
    getTargetWriter().println(value);
  }

  @Override
  public void println(Object value) throws IOException {
    getTargetWriter().println(value);
  }

  @Override
  public void println(String value) throws IOException {
    getTargetWriter().println(value);
  }

}
