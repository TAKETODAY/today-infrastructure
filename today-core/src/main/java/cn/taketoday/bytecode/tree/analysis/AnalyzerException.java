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
package cn.taketoday.bytecode.tree.analysis;

import java.io.Serial;

import cn.taketoday.bytecode.tree.AbstractInsnNode;

/**
 * An exception thrown if a problem occurs during the analysis of a method.
 *
 * @author Bing Ran
 * @author Eric Bruneton
 */
public class AnalyzerException extends Exception {

  @Serial
  private static final long serialVersionUID = 3154190448018943333L;

  /* The bytecode instruction where the analysis failed. */
  public final transient AbstractInsnNode node;

  /**
   * Constructs a new {@link AnalyzerException}.
   *
   * @param insn the bytecode instruction where the analysis failed.
   * @param message the reason why the analysis failed.
   */
  public AnalyzerException(final AbstractInsnNode insn, final String message) {
    super(message);
    this.node = insn;
  }

  /**
   * Constructs a new {@link AnalyzerException}.
   *
   * @param insn the bytecode instruction where the analysis failed.
   * @param message the reason why the analysis failed.
   * @param cause the cause of the failure.
   */
  public AnalyzerException(
          final AbstractInsnNode insn, final String message, final Throwable cause) {
    super(message, cause);
    this.node = insn;
  }

  /**
   * Constructs a new {@link AnalyzerException}.
   *
   * @param insn the bytecode instruction where the analysis failed.
   * @param message the reason why the analysis failed.
   * @param expected an expected value.
   * @param actual the actual value, different from the expected one.
   */
  public AnalyzerException(
          final AbstractInsnNode insn, final String message, final Object expected, final Value actual) {
    super((message == null ? "Expected " : message + ": expected ") + expected + ", but found " + actual);
    this.node = insn;
  }
}
