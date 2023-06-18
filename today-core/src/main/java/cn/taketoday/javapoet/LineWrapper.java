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
package cn.taketoday.javapoet;

import java.io.IOException;

import static cn.taketoday.javapoet.Util.checkNotNull;

/**
 * Implements soft line wrapping on an appendable. To use, append characters using {@link #append}
 * or soft-wrapping spaces using {@link #wrappingSpace}.
 */
final class LineWrapper {
  private final RecordingAppendable out;
  private final String indent;
  private final int columnLimit;
  private boolean closed;

  /** Characters written since the last wrapping space that haven't yet been flushed. */
  private final StringBuilder buffer = new StringBuilder();

  /** The number of characters since the most recent newline. Includes both out and the buffer. */
  private int column = 0;

  /**
   * -1 if we have no buffering; otherwise the number of {@code indent}s to write after wrapping.
   */
  private int indentLevel = -1;

  /**
   * Null if we have no buffering; otherwise the type to pass to the next call to {@link #flush}.
   */
  private FlushType nextFlush;

  LineWrapper(Appendable out, String indent, int columnLimit) {
    checkNotNull(out, "out == null");
    this.out = new RecordingAppendable(out);
    this.indent = indent;
    this.columnLimit = columnLimit;
  }

  /** @return the last emitted char or {@link Character#MIN_VALUE} if nothing emitted yet. */
  char lastChar() {
    return out.lastChar;
  }

  /** Emit {@code s}. This may be buffered to permit line wraps to be inserted. */
  void append(String s) throws IOException {
    if (closed)
      throw new IllegalStateException("closed");

    if (nextFlush != null) {
      int nextNewline = s.indexOf('\n');

      // If s doesn't cause the current line to cross the limit, buffer it and return. We'll decide
      // whether or not we have to wrap it later.
      if (nextNewline == -1 && column + s.length() <= columnLimit) {
        buffer.append(s);
        column += s.length();
        return;
      }

      // Wrap if appending s would overflow the current line.
      boolean wrap = nextNewline == -1 || column + nextNewline > columnLimit;
      flush(wrap ? FlushType.WRAP : nextFlush);
    }

    out.append(s);
    int lastNewline = s.lastIndexOf('\n');
    column = lastNewline != -1
             ? s.length() - lastNewline - 1
             : column + s.length();
  }

  /** Emit either a space or a newline character. */
  void wrappingSpace(int indentLevel) throws IOException {
    if (closed)
      throw new IllegalStateException("closed");

    if (this.nextFlush != null)
      flush(nextFlush);
    column++; // Increment the column even though the space is deferred to next call to flush().
    this.nextFlush = FlushType.SPACE;
    this.indentLevel = indentLevel;
  }

  /** Emit a newline character if the line will exceed it's limit, otherwise do nothing. */
  void zeroWidthSpace(int indentLevel) throws IOException {
    if (closed)
      throw new IllegalStateException("closed");

    if (column == 0)
      return;
    if (this.nextFlush != null)
      flush(nextFlush);
    this.nextFlush = FlushType.EMPTY;
    this.indentLevel = indentLevel;
  }

  /** Flush any outstanding text and forbid future writes to this line wrapper. */
  void close() throws IOException {
    if (nextFlush != null)
      flush(nextFlush);
    closed = true;
  }

  /** Write the space followed by any buffered text that follows it. */
  private void flush(FlushType flushType) throws IOException {
    switch (flushType) {
      case WRAP:
        out.append('\n');
        for (int i = 0; i < indentLevel; i++) {
          out.append(indent);
        }
        column = indentLevel * indent.length();
        column += buffer.length();
        break;
      case SPACE:
        out.append(' ');
        break;
      case EMPTY:
        break;
      default:
        throw new IllegalArgumentException("Unknown FlushType: " + flushType);
    }

    out.append(buffer);
    buffer.delete(0, buffer.length());
    indentLevel = -1;
    nextFlush = null;
  }

  private enum FlushType {
    WRAP, SPACE, EMPTY;
  }

  /** A delegating {@link Appendable} that records info about the chars passing through it. */
  static final class RecordingAppendable implements Appendable {
    private final Appendable delegate;

    char lastChar = Character.MIN_VALUE;

    RecordingAppendable(Appendable delegate) {
      this.delegate = delegate;
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
      int length = csq.length();
      if (length != 0) {
        lastChar = csq.charAt(length - 1);
      }
      return delegate.append(csq);
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
      CharSequence sub = csq.subSequence(start, end);
      return append(sub);
    }

    @Override
    public Appendable append(char c) throws IOException {
      lastChar = c;
      return delegate.append(c);
    }
  }
}
