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

package cn.taketoday.framework.test.system;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import cn.taketoday.framework.ansi.AnsiOutput;
import cn.taketoday.framework.ansi.AnsiOutput.Enabled;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;

/**
 * Provides support for capturing {@link System#out System.out} and {@link System#err
 * System.err}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @see OutputCaptureExtension
 * @see OutputCaptureRule
 */
class OutputCapture implements CapturedOutput {

  private final Deque<SystemCapture> systemCaptures = new ArrayDeque<>();

  private AnsiOutputState ansiOutputState;

  /**
   * Push a new system capture session onto the stack.
   */
  final void push() {
    if (this.systemCaptures.isEmpty()) {
      this.ansiOutputState = AnsiOutputState.saveAndDisable();
    }
    this.systemCaptures.addLast(new SystemCapture());
  }

  /**
   * Pop the last system capture session from the stack.
   */
  final void pop() {
    this.systemCaptures.removeLast().release();
    if (this.systemCaptures.isEmpty() && this.ansiOutputState != null) {
      this.ansiOutputState.restore();
      this.ansiOutputState = null;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof CapturedOutput || obj instanceof CharSequence) {
      return getAll().equals(obj.toString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public String toString() {
    return getAll();
  }

  /**
   * Return all content (both {@link System#out System.out} and {@link System#err
   * System.err}) in the order that it was captured.
   *
   * @return all captured output
   */
  @Override
  public String getAll() {
    return get((type) -> true);
  }

  /**
   * Return {@link System#out System.out} content in the order that it was captured.
   *
   * @return {@link System#out System.out} captured output
   */
  @Override
  public String getOut() {
    return get(Type.OUT::equals);
  }

  /**
   * Return {@link System#err System.err} content in the order that it was captured.
   *
   * @return {@link System#err System.err} captured output
   */
  @Override
  public String getErr() {
    return get(Type.ERR::equals);
  }

  /**
   * Resets the current capture session, clearing its captured output.
   */
  void reset() {
    this.systemCaptures.peek().reset();
  }

  private String get(Predicate<Type> filter) {
    Assert.state(!this.systemCaptures.isEmpty(),
            "No system captures found. Please check your output capture registration.");
    StringBuilder builder = new StringBuilder();
    for (SystemCapture systemCapture : this.systemCaptures) {
      systemCapture.append(builder, filter);
    }
    return builder.toString();
  }

  /**
   * A capture session that captures {@link System#out System.out} and {@link System#out
   * System.err}.
   */
  private static class SystemCapture {

    private final Object monitor = new Object();

    private final PrintStreamCapture out;

    private final PrintStreamCapture err;

    private final List<CapturedString> capturedStrings = new ArrayList<>();

    SystemCapture() {
      this.out = new PrintStreamCapture(System.out, this::captureOut);
      this.err = new PrintStreamCapture(System.err, this::captureErr);
      System.setOut(this.out);
      System.setErr(this.err);
    }

    void release() {
      System.setOut(this.out.getParent());
      System.setErr(this.err.getParent());
    }

    private void captureOut(String string) {
      synchronized(this.monitor) {
        this.capturedStrings.add(new CapturedString(Type.OUT, string));
      }
    }

    private void captureErr(String string) {
      synchronized(this.monitor) {
        this.capturedStrings.add(new CapturedString(Type.ERR, string));
      }
    }

    void append(StringBuilder builder, Predicate<Type> filter) {
      synchronized(this.monitor) {
        for (CapturedString stringCapture : this.capturedStrings) {
          if (filter.test(stringCapture.getType())) {
            builder.append(stringCapture);
          }
        }
      }
    }

    void reset() {
      synchronized(this.monitor) {
        this.capturedStrings.clear();
      }
    }

  }

  /**
   * A {@link PrintStream} implementation that captures written strings.
   */
  private static class PrintStreamCapture extends PrintStream {

    private final PrintStream parent;

    PrintStreamCapture(PrintStream parent, Consumer<String> copy) {
      super(new OutputStreamCapture(getSystemStream(parent), copy));
      this.parent = parent;
    }

    PrintStream getParent() {
      return this.parent;
    }

    private static PrintStream getSystemStream(PrintStream printStream) {
      while (printStream instanceof PrintStreamCapture) {
        printStream = ((PrintStreamCapture) printStream).getParent();
      }
      return printStream;
    }

  }

  /**
   * An {@link OutputStream} implementation that captures written strings.
   */
  private static class OutputStreamCapture extends OutputStream {

    private final PrintStream systemStream;

    private final Consumer<String> copy;

    OutputStreamCapture(PrintStream systemStream, Consumer<String> copy) {
      this.systemStream = systemStream;
      this.copy = copy;
    }

    @Override
    public void write(int b) throws IOException {
      write(new byte[] { (byte) (b & 0xFF) });
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      this.copy.accept(new String(b, off, len));
      this.systemStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      this.systemStream.flush();
    }

  }

  /**
   * A captured string that forms part of the full output.
   */
  private static class CapturedString {

    private final Type type;

    private final String string;

    CapturedString(Type type, String string) {
      this.type = type;
      this.string = string;
    }

    Type getType() {
      return this.type;
    }

    @Override
    public String toString() {
      return this.string;
    }

  }

  /**
   * Types of content that can be captured.
   */
  private enum Type {

    OUT, ERR

  }

  /**
   * Save, disable and restore AnsiOutput without it needing to be on the classpath.
   */
  private static class AnsiOutputState {

    private final Enabled saved;

    AnsiOutputState() {
      this.saved = AnsiOutput.getEnabled();
      AnsiOutput.setEnabled(Enabled.NEVER);
    }

    void restore() {
      AnsiOutput.setEnabled(this.saved);
    }

    static AnsiOutputState saveAndDisable() {
      if (!ClassUtils.isPresent("cn.taketoday.framework.ansi.AnsiOutput",
              OutputCapture.class.getClassLoader())) {
        return null;
      }
      return new AnsiOutputState();
    }

  }

}
