/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.logging;

import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A simple log message type for use with Commons Logging, allowing
 * for convenient lazy resolution of a given {@link Supplier} instance
 * (typically bound to a Java 8 lambda expression) or a printf-style
 * format string ({@link MessageFormatter#format}) in its {@link #toString()}.
 *
 * @author Juergen Hoeller
 * @see #from(Supplier)
 * @see #format(String, Object)
 * @see #format(String, Object...)
 * @see Logger#error(Object)
 * @see Logger#warn(Object)
 * @see Logger#info(Object)
 * @see Logger#debug(Object)
 * @see Logger#trace(Object)
 * @since 4.0
 */
public abstract class LogMessage implements CharSequence {

  @Nullable
  private String result;

  @Override
  public int length() {
    return toString().length();
  }

  @Override
  public char charAt(int index) {
    return toString().charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return toString().subSequence(start, end);
  }

  /**
   * This will be called by the logging provider, potentially once
   * per log target (therefore locally caching the result here).
   */
  @Override
  public String toString() {
    if (this.result == null) {
      this.result = buildString();
    }
    return this.result;
  }

  abstract String buildString();

  /**
   * Build a lazily resolving message from the given supplier.
   *
   * @param supplier the supplier (typically bound to a Java 8 lambda expression)
   * @see #toString()
   */
  public static LogMessage from(Supplier<? extends CharSequence> supplier) {
    return new SupplierMessage(supplier);
  }

  /**
   * Build a lazily formatted message from the given format string and argument.
   *
   * @param format the format string (following {@link MessageFormatter#format(String, Object[])} rules)
   * @param arg1 the argument
   * @see MessageFormatter#format(String, Object...)
   */
  public static LogMessage format(String format, Object arg1) {
    return new FormatMessage1(format, arg1);
  }

  /**
   * Build a lazily formatted message from the given format string and arguments.
   *
   * @param format the format string (following {@link MessageFormatter#format(String, Object[])} rules)
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @see MessageFormatter#format(String, Object[])
   */
  public static LogMessage format(String format, Object arg1, Object arg2) {
    return new FormatMessage2(format, arg1, arg2);
  }

  /**
   * Build a lazily formatted message from the given format string and arguments.
   *
   * @param format the format string (following {@link MessageFormatter#format(String, Object[])} rules)
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @param arg3 the third argument
   * @see MessageFormatter#format(String, Object...)
   */
  public static LogMessage format(String format, Object arg1, Object arg2, Object arg3) {
    return new FormatMessage3(format, arg1, arg2, arg3);
  }

  /**
   * Build a lazily formatted message from the given format string and arguments.
   *
   * @param format the format string (following {@link MessageFormatter#format(String, Object[])} rules)
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @param arg3 the third argument
   * @param arg4 the fourth argument
   * @see MessageFormatter#format(String, Object...)
   */
  public static LogMessage format(String format, Object arg1, Object arg2, Object arg3, Object arg4) {
    return new FormatMessage4(format, arg1, arg2, arg3, arg4);
  }

  /**
   * Build a lazily formatted message from the given format string and varargs.
   *
   * @param format the format string (following {@link MessageFormatter#format(String, Object[])} rules)
   * @param args the varargs array (costly, prefer individual arguments)
   * @see MessageFormatter#format(String, Object...)
   */
  public static LogMessage format(String format, Object... args) {
    return new FormatMessageX(format, args);
  }

  private static final class SupplierMessage extends LogMessage {

    private final Supplier<? extends CharSequence> supplier;

    SupplierMessage(Supplier<? extends CharSequence> supplier) {
      Assert.notNull(supplier, "Supplier is required");
      this.supplier = supplier;
    }

    @Override
    String buildString() {
      return this.supplier.get().toString();
    }
  }

  private static abstract class FormatMessage extends LogMessage {

    protected final String format;

    FormatMessage(String format) {
      Assert.notNull(format, "Format is required");
      this.format = format;
    }
  }

  private static final class FormatMessage1 extends FormatMessage {

    private final Object arg1;

    FormatMessage1(String format, Object arg1) {
      super(format);
      this.arg1 = arg1;
    }

    @Override
    protected String buildString() {
      return MessageFormatter.format(this.format, this.arg1);
    }
  }

  private static final class FormatMessage2 extends FormatMessage {

    private final Object arg1;
    private final Object arg2;

    FormatMessage2(String format, Object arg1, Object arg2) {
      super(format);
      this.arg1 = arg1;
      this.arg2 = arg2;
    }

    @Override
    String buildString() {
      return MessageFormatter.format(this.format, new Object[] { this.arg1, this.arg2 });
    }
  }

  private static final class FormatMessage3 extends FormatMessage {

    private final Object arg1;
    private final Object arg2;
    private final Object arg3;

    FormatMessage3(String format, Object arg1, Object arg2, Object arg3) {
      super(format);
      this.arg1 = arg1;
      this.arg2 = arg2;
      this.arg3 = arg3;
    }

    @Override
    String buildString() {
      return MessageFormatter.format(this.format, new Object[] { this.arg1, this.arg2, this.arg3 });
    }
  }

  private static final class FormatMessage4 extends FormatMessage {

    private final Object arg1;
    private final Object arg2;
    private final Object arg3;
    private final Object arg4;

    FormatMessage4(String format, Object arg1, Object arg2, Object arg3, Object arg4) {
      super(format);
      this.arg1 = arg1;
      this.arg2 = arg2;
      this.arg3 = arg3;
      this.arg4 = arg4;
    }

    @Override
    String buildString() {
      return MessageFormatter.format(this.format, new Object[] { this.arg1, this.arg2, this.arg3, this.arg4 });
    }
  }

  private static final class FormatMessageX extends FormatMessage {

    private final Object[] args;

    FormatMessageX(String format, Object[] args) {
      super(format);
      this.args = args;
    }

    @Override
    String buildString() {
      return MessageFormatter.format(this.format, this.args);
    }
  }

}
