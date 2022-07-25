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
package cn.taketoday.bytecode.signature;

/**
 * A visitor to visit a generic signature. The methods of this interface must be called in one of
 * the three following orders (the last one is the only valid order for a {@link SignatureVisitor}
 * that is returned by a method of this interface):
 *
 * <ul>
 *   <li><i>ClassSignature</i> = ( {@code visitFormalTypeParameter} {@code visitClassBound}? {@code
 *       visitInterfaceBound}* )* ({@code visitSuperclass} {@code visitInterface}* )
 *   <li><i>MethodSignature</i> = ( {@code visitFormalTypeParameter} {@code visitClassBound}? {@code
 *       visitInterfaceBound}* )* ({@code visitParameterType}* {@code visitReturnType} {@code
 *       visitExceptionType}* )
 *   <li><i>TypeSignature</i> = {@code visitBaseType} | {@code visitTypeVariable} | {@code
 *       visitArrayType} | ( {@code visitClassType} {@code visitTypeArgument}* ( {@code
 *       visitInnerClassType} {@code visitTypeArgument}* )* {@code visitEnd} ) )
 * </ul>
 *
 * @author Thomas Hallgren
 * @author Eric Bruneton
 */
public abstract class SignatureVisitor {

  /** Wildcard for an "extends" type argument. */
  public static final char EXTENDS = '+';

  /** Wildcard for a "super" type argument. */
  public static final char SUPER = '-';

  /** Wildcard for a normal type argument. */
  public static final char INSTANCEOF = '=';

  /**
   * Constructs a new {@link SignatureVisitor}.
   */
  public SignatureVisitor() { }

  /**
   * Visits a formal type parameter.
   *
   * @param name the name of the formal parameter.
   */
  public void visitFormalTypeParameter(final String name) { }

  /**
   * Visits the class bound of the last visited formal type parameter.
   *
   * @return a non null visitor to visit the signature of the class bound.
   */
  public SignatureVisitor visitClassBound() {
    return this;
  }

  /**
   * Visits an interface bound of the last visited formal type parameter.
   *
   * @return a non null visitor to visit the signature of the interface bound.
   */
  public SignatureVisitor visitInterfaceBound() {
    return this;
  }

  /**
   * Visits the type of the super class.
   *
   * @return a non null visitor to visit the signature of the super class type.
   */
  public SignatureVisitor visitSuperclass() {
    return this;
  }

  /**
   * Visits the type of an interface implemented by the class.
   *
   * @return a non null visitor to visit the signature of the interface type.
   */
  public SignatureVisitor visitInterface() {
    return this;
  }

  /**
   * Visits the type of a method parameter.
   *
   * @return a non null visitor to visit the signature of the parameter type.
   */
  public SignatureVisitor visitParameterType() {
    return this;
  }

  /**
   * Visits the return type of the method.
   *
   * @return a non null visitor to visit the signature of the return type.
   */
  public SignatureVisitor visitReturnType() {
    return this;
  }

  /**
   * Visits the type of a method exception.
   *
   * @return a non null visitor to visit the signature of the exception type.
   */
  public SignatureVisitor visitExceptionType() {
    return this;
  }

  /**
   * Visits a signature corresponding to a primitive type.
   *
   * @param descriptor the descriptor of the primitive type, or 'V' for {@code void} .
   */
  public void visitBaseType(final char descriptor) { }

  /**
   * Visits a signature corresponding to a type variable.
   *
   * @param name the name of the type variable.
   */
  public void visitTypeVariable(final String name) { }

  /**
   * Visits a signature corresponding to an array type.
   *
   * @return a non null visitor to visit the signature of the array element type.
   */
  public SignatureVisitor visitArrayType() {
    return this;
  }

  /**
   * Starts the visit of a signature corresponding to a class or interface type.
   *
   * @param name the internal name of the class or interface.
   */
  public void visitClassType(final String name) { }

  /**
   * Visits an inner class.
   *
   * @param name the local name of the inner class in its enclosing class.
   */
  public void visitInnerClassType(final String name) { }

  /** Visits an unbounded type argument of the last visited class or inner class type. */
  public void visitTypeArgument() { }

  /**
   * Visits a type argument of the last visited class or inner class type.
   *
   * @param wildcard '+', '-' or '='.
   * @return a non null visitor to visit the signature of the type argument.
   */
  public SignatureVisitor visitTypeArgument(final char wildcard) {
    return this;
  }

  /** Ends the visit of a signature corresponding to a class or interface type. */
  public void visitEnd() { }
}
