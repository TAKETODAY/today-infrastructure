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
package cn.taketoday.bytecode.util;

import java.util.EnumSet;

import cn.taketoday.bytecode.signature.SignatureVisitor;

/**
 * A {@link SignatureVisitor} that checks that its methods are properly used.
 *
 * @author Eric Bruneton
 */
public class CheckSignatureAdapter extends SignatureVisitor {

  /**
   * Type to be used to check class signatures. See {@link #CheckSignatureAdapter(int,
   * SignatureVisitor)}.
   */
  public static final int CLASS_SIGNATURE = 0;

  /**
   * Type to be used to check method signatures. See {@link #CheckSignatureAdapter(int,
   * SignatureVisitor)}.
   */
  public static final int METHOD_SIGNATURE = 1;

  /**
   * Type to be used to check type signatures.See {@link #CheckSignatureAdapter(int,
   * SignatureVisitor)}.
   */
  public static final int TYPE_SIGNATURE = 2;

  /** The valid automaton states for a {@link #visitFormalTypeParameter} method call. */
  private static final EnumSet<State> VISIT_FORMAL_TYPE_PARAMETER_STATES =
          EnumSet.of(State.EMPTY, State.FORMAL, State.BOUND);

  /** The valid automaton states for a {@link #visitClassBound} method call. */
  private static final EnumSet<State> VISIT_CLASS_BOUND_STATES = EnumSet.of(State.FORMAL);

  /** The valid automaton states for a {@link #visitInterfaceBound} method call. */
  private static final EnumSet<State> VISIT_INTERFACE_BOUND_STATES =
          EnumSet.of(State.FORMAL, State.BOUND);

  /** The valid automaton states for a {@link #visitSuperclass} method call. */
  private static final EnumSet<State> VISIT_SUPER_CLASS_STATES =
          EnumSet.of(State.EMPTY, State.FORMAL, State.BOUND);

  /** The valid automaton states for a {@link #visitInterface} method call. */
  private static final EnumSet<State> VISIT_INTERFACE_STATES = EnumSet.of(State.SUPER);

  /** The valid automaton states for a {@link #visitParameterType} method call. */
  private static final EnumSet<State> VISIT_PARAMETER_TYPE_STATES =
          EnumSet.of(State.EMPTY, State.FORMAL, State.BOUND, State.PARAM);

  /** The valid automaton states for a {@link #visitReturnType} method call. */
  private static final EnumSet<State> VISIT_RETURN_TYPE_STATES =
          EnumSet.of(State.EMPTY, State.FORMAL, State.BOUND, State.PARAM);

  /** The valid automaton states for a {@link #visitExceptionType} method call. */
  private static final EnumSet<State> VISIT_EXCEPTION_TYPE_STATES = EnumSet.of(State.RETURN);

  /** The possible states of the automaton used to check the order of method calls. */
  private enum State {
    EMPTY,
    FORMAL,
    BOUND,
    SUPER,
    PARAM,
    RETURN,
    SIMPLE_TYPE,
    CLASS_TYPE,
    END;
  }

  private static final String INVALID = "Invalid ";

  /** The type of the visited signature. */
  private final int type;

  /** The current state of the automaton used to check the order of method calls. */
  private State state;

  /** Whether the visited signature can be 'V'. */
  private boolean canBeVoid;

  /** The visitor to which this adapter must delegate calls. May be {@literal null}. */
  private final SignatureVisitor signatureVisitor;

  /**
   * Constructs a new {@link CheckSignatureAdapter}.
   *
   * @param type the type of signature to be checked. See {@link #CLASS_SIGNATURE}, {@link
   * #METHOD_SIGNATURE} and {@link #TYPE_SIGNATURE}.
   * @param signatureVisitor the visitor to which this adapter must delegate calls. May be {@literal
   * null}.
   */
  public CheckSignatureAdapter(final int type, final SignatureVisitor signatureVisitor) {
    this.type = type;
    this.state = State.EMPTY;
    this.signatureVisitor = signatureVisitor;
  }

  // class and method signatures

  @Override
  public void visitFormalTypeParameter(final String name) {
    if (type == TYPE_SIGNATURE || !VISIT_FORMAL_TYPE_PARAMETER_STATES.contains(state)) {
      throw new IllegalStateException();
    }
    checkIdentifier(name, "formal type parameter");
    state = State.FORMAL;
    if (signatureVisitor != null) {
      signatureVisitor.visitFormalTypeParameter(name);
    }
  }

  @Override
  public SignatureVisitor visitClassBound() {
    if (type == TYPE_SIGNATURE || !VISIT_CLASS_BOUND_STATES.contains(state)) {
      throw new IllegalStateException();
    }
    state = State.BOUND;
    return new CheckSignatureAdapter(
            TYPE_SIGNATURE, signatureVisitor == null ? null : signatureVisitor.visitClassBound());
  }

  @Override
  public SignatureVisitor visitInterfaceBound() {
    if (type == TYPE_SIGNATURE || !VISIT_INTERFACE_BOUND_STATES.contains(state)) {
      throw new IllegalStateException();
    }
    return new CheckSignatureAdapter(
            TYPE_SIGNATURE, signatureVisitor == null ? null : signatureVisitor.visitInterfaceBound());
  }

  // class signatures

  @Override
  public SignatureVisitor visitSuperclass() {
    if (type != CLASS_SIGNATURE || !VISIT_SUPER_CLASS_STATES.contains(state)) {
      throw new IllegalStateException();
    }
    state = State.SUPER;
    return new CheckSignatureAdapter(
            TYPE_SIGNATURE, signatureVisitor == null ? null : signatureVisitor.visitSuperclass());
  }

  @Override
  public SignatureVisitor visitInterface() {
    if (type != CLASS_SIGNATURE || !VISIT_INTERFACE_STATES.contains(state)) {
      throw new IllegalStateException();
    }
    return new CheckSignatureAdapter(
            TYPE_SIGNATURE, signatureVisitor == null ? null : signatureVisitor.visitInterface());
  }

  // method signatures

  @Override
  public SignatureVisitor visitParameterType() {
    if (type != METHOD_SIGNATURE || !VISIT_PARAMETER_TYPE_STATES.contains(state)) {
      throw new IllegalStateException();
    }
    state = State.PARAM;
    return new CheckSignatureAdapter(
            TYPE_SIGNATURE, signatureVisitor == null ? null : signatureVisitor.visitParameterType());
  }

  @Override
  public SignatureVisitor visitReturnType() {
    if (type != METHOD_SIGNATURE || !VISIT_RETURN_TYPE_STATES.contains(state)) {
      throw new IllegalStateException();
    }
    state = State.RETURN;
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(
                    TYPE_SIGNATURE, signatureVisitor == null ? null : signatureVisitor.visitReturnType());
    checkSignatureAdapter.canBeVoid = true;
    return checkSignatureAdapter;
  }

  @Override
  public SignatureVisitor visitExceptionType() {
    if (type != METHOD_SIGNATURE || !VISIT_EXCEPTION_TYPE_STATES.contains(state)) {
      throw new IllegalStateException();
    }
    return new CheckSignatureAdapter(
            TYPE_SIGNATURE, signatureVisitor == null ? null : signatureVisitor.visitExceptionType());
  }

  // type signatures

  @Override
  public void visitBaseType(final char descriptor) {
    if (type != TYPE_SIGNATURE || state != State.EMPTY) {
      throw new IllegalStateException();
    }
    if (descriptor == 'V') {
      if (!canBeVoid) {
        throw new IllegalArgumentException("Base type descriptor can't be V");
      }
    }
    else {
      if ("ZCBSIFJD".indexOf(descriptor) == -1) {
        throw new IllegalArgumentException("Base type descriptor must be one of ZCBSIFJD");
      }
    }
    state = State.SIMPLE_TYPE;
    if (signatureVisitor != null) {
      signatureVisitor.visitBaseType(descriptor);
    }
  }

  @Override
  public void visitTypeVariable(final String name) {
    if (type != TYPE_SIGNATURE || state != State.EMPTY) {
      throw new IllegalStateException();
    }
    checkIdentifier(name, "type variable");
    state = State.SIMPLE_TYPE;
    if (signatureVisitor != null) {
      signatureVisitor.visitTypeVariable(name);
    }
  }

  @Override
  public SignatureVisitor visitArrayType() {
    if (type != TYPE_SIGNATURE || state != State.EMPTY) {
      throw new IllegalStateException();
    }
    state = State.SIMPLE_TYPE;
    return new CheckSignatureAdapter(
            TYPE_SIGNATURE, signatureVisitor == null ? null : signatureVisitor.visitArrayType());
  }

  @Override
  public void visitClassType(final String name) {
    if (type != TYPE_SIGNATURE || state != State.EMPTY) {
      throw new IllegalStateException();
    }
    checkClassName(name, "class name");
    state = State.CLASS_TYPE;
    if (signatureVisitor != null) {
      signatureVisitor.visitClassType(name);
    }
  }

  @Override
  public void visitInnerClassType(final String name) {
    if (state != State.CLASS_TYPE) {
      throw new IllegalStateException();
    }
    checkIdentifier(name, "inner class name");
    if (signatureVisitor != null) {
      signatureVisitor.visitInnerClassType(name);
    }
  }

  @Override
  public void visitTypeArgument() {
    if (state != State.CLASS_TYPE) {
      throw new IllegalStateException();
    }
    if (signatureVisitor != null) {
      signatureVisitor.visitTypeArgument();
    }
  }

  @Override
  public SignatureVisitor visitTypeArgument(final char wildcard) {
    if (state != State.CLASS_TYPE) {
      throw new IllegalStateException();
    }
    if ("+-=".indexOf(wildcard) == -1) {
      throw new IllegalArgumentException("Wildcard must be one of +-=");
    }
    return new CheckSignatureAdapter(
            TYPE_SIGNATURE,
            signatureVisitor == null ? null : signatureVisitor.visitTypeArgument(wildcard));
  }

  @Override
  public void visitEnd() {
    if (state != State.CLASS_TYPE) {
      throw new IllegalStateException();
    }
    state = State.END;
    if (signatureVisitor != null) {
      signatureVisitor.visitEnd();
    }
  }

  private void checkClassName(final String name, final String message) {
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException(INVALID + message + " (must not be null or empty)");
    }
    for (int i = 0; i < name.length(); ++i) {
      if (".;[<>:".indexOf(name.charAt(i)) != -1) {
        throw new IllegalArgumentException(
                INVALID + message + " (must not contain . ; [ < > or :): " + name);
      }
    }
  }

  private void checkIdentifier(final String name, final String message) {
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException(INVALID + message + " (must not be null or empty)");
    }
    for (int i = 0; i < name.length(); ++i) {
      if (".;[/<>:".indexOf(name.charAt(i)) != -1) {
        throw new IllegalArgumentException(
                INVALID + message + " (must not contain . ; [ / < > or :): " + name);
      }
    }
  }
}
