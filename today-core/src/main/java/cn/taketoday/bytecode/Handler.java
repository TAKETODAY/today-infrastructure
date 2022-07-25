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
package cn.taketoday.bytecode;

/**
 * Information about an exception handler. Corresponds to an element of the exception_table array of
 * a Code attribute, as defined in the Java Virtual Machine Specification (JVMS). Handler instances
 * can be chained together, with their {@link #nextHandler} field, to describe a full JVMS
 * exception_table array.
 *
 * @author Eric Bruneton
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.3">JVMS
 * 4.7.3</a>
 */
final class Handler {

  /**
   * The start_pc field of this JVMS exception_table entry. Corresponds to the beginning of the
   * exception handler's scope (inclusive).
   */
  public final Label startPc;

  /**
   * The end_pc field of this JVMS exception_table entry. Corresponds to the end of the exception
   * handler's scope (exclusive).
   */
  public final Label endPc;

  /**
   * The handler_pc field of this JVMS exception_table entry. Corresponding to the beginning of the
   * exception handler's code.
   */
  public final Label handlerPc;

  /**
   * The catch_type field of this JVMS exception_table entry. This is the constant pool index of the
   * internal name of the type of exceptions handled by this handler, or 0 to catch any exceptions.
   */
  public final int catchType;

  /**
   * The internal name of the type of exceptions handled by this handler, or {@literal null} to
   * catch any exceptions.
   */
  public final String catchTypeDescriptor;

  /** The next exception handler. */
  public Handler nextHandler;

  /**
   * Constructs a new Handler.
   *
   * @param startPc the start_pc field of this JVMS exception_table entry.
   * @param endPc the end_pc field of this JVMS exception_table entry.
   * @param handlerPc the handler_pc field of this JVMS exception_table entry.
   * @param catchType The catch_type field of this JVMS exception_table entry.
   * @param catchTypeDescriptor The internal name of the type of exceptions handled by this handler,
   * or {@literal null} to catch any exceptions.
   */
  Handler(final Label startPc,
          final Label endPc,
          final Label handlerPc,
          final int catchType,
          final String catchTypeDescriptor) {
    this.startPc = startPc;
    this.endPc = endPc;
    this.handlerPc = handlerPc;
    this.catchType = catchType;
    this.catchTypeDescriptor = catchTypeDescriptor;
  }

  /**
   * Constructs a new Handler from the given one, with a different scope.
   *
   * @param handler an existing Handler.
   * @param startPc the start_pc field of this JVMS exception_table entry.
   * @param endPc the end_pc field of this JVMS exception_table entry.
   */
  Handler(final Handler handler, final Label startPc, final Label endPc) {
    this(startPc, endPc, handler.handlerPc, handler.catchType, handler.catchTypeDescriptor);
    this.nextHandler = handler.nextHandler;
  }

  /**
   * Removes the range between start and end from the Handler list that begins with the given
   * element.
   *
   * @param firstHandler the beginning of a Handler list. May be {@literal null}.
   * @param start the start of the range to be removed.
   * @param end the end of the range to be removed. Maybe {@literal null}.
   * @return the exception handler list with the start-end range removed.
   */
  static Handler removeRange(final Handler firstHandler, final Label start, final Label end) {
    if (firstHandler == null) {
      return null;
    }
    else {
      firstHandler.nextHandler = removeRange(firstHandler.nextHandler, start, end);
    }
    int handlerStart = firstHandler.startPc.bytecodeOffset;
    int handlerEnd = firstHandler.endPc.bytecodeOffset;
    int rangeStart = start.bytecodeOffset;
    int rangeEnd = end == null ? Integer.MAX_VALUE : end.bytecodeOffset;
    // Return early if [handlerStart,handlerEnd[ and [rangeStart,rangeEnd[ don't intersect.
    if (rangeStart >= handlerEnd || rangeEnd <= handlerStart) {
      return firstHandler;
    }
    if (rangeStart <= handlerStart) {
      if (rangeEnd >= handlerEnd) {
        // If [handlerStart,handlerEnd[ is included in [rangeStart,rangeEnd[, remove firstHandler.
        return firstHandler.nextHandler;
      }
      else {
        // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ = [rangeEnd,handlerEnd[
        return new Handler(firstHandler, end, firstHandler.endPc);
      }
    }
    else if (rangeEnd >= handlerEnd) {
      // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ = [handlerStart,rangeStart[
      return new Handler(firstHandler, firstHandler.startPc, start);
    }
    else {
      // [handlerStart,handlerEnd[ - [rangeStart,rangeEnd[ =
      //     [handlerStart,rangeStart[ + [rangeEnd,handerEnd[
      firstHandler.nextHandler = new Handler(firstHandler, end, firstHandler.endPc);
      return new Handler(firstHandler, firstHandler.startPc, start);
    }
  }

  /**
   * Returns the number of elements of the Handler list that begins with the given element.
   *
   * @param firstHandler the beginning of a Handler list. May be {@literal null}.
   * @return the number of elements of the Handler list that begins with 'handler'.
   */
  static int getExceptionTableLength(final Handler firstHandler) {
    int length = 0;
    Handler handler = firstHandler;
    while (handler != null) {
      length++;
      handler = handler.nextHandler;
    }
    return length;
  }

  /**
   * Returns the size in bytes of the JVMS exception_table corresponding to the Handler list that
   * begins with the given element. <i>This includes the exception_table_length field.</i>
   *
   * @param firstHandler the beginning of a Handler list. May be {@literal null}.
   * @return the size in bytes of the exception_table_length and exception_table structures.
   */
  static int getExceptionTableSize(final Handler firstHandler) {
    return 2 + 8 * getExceptionTableLength(firstHandler);
  }

  /**
   * Puts the JVMS exception_table corresponding to the Handler list that begins with the given
   * element. <i>This includes the exception_table_length field.</i>
   *
   * @param firstHandler the beginning of a Handler list. May be {@literal null}.
   * @param output where the exception_table_length and exception_table structures must be put.
   */
  static void putExceptionTable(final Handler firstHandler, final ByteVector output) {
    output.putShort(getExceptionTableLength(firstHandler));
    Handler handler = firstHandler;
    while (handler != null) {
      output.putShort(handler.startPc.bytecodeOffset)
              .putShort(handler.endPc.bytecodeOffset)
              .putShort(handler.handlerPc.bytecodeOffset)
              .putShort(handler.catchType);
      handler = handler.nextHandler;
    }
  }
}
