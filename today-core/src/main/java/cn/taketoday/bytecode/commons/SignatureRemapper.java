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

package cn.taketoday.bytecode.commons;

import java.util.ArrayList;

import cn.taketoday.bytecode.signature.SignatureVisitor;

/**
 * A {@link SignatureVisitor} that remaps types with a {@link Remapper}.
 *
 * @author Eugene Kuleshov
 */
public class SignatureRemapper extends SignatureVisitor {

  private final Remapper remapper;
  private final SignatureVisitor signatureVisitor;
  private final ArrayList<String> classNames = new ArrayList<>();

  /**
   * Constructs a new {@link SignatureRemapper}.
   *
   * @param signatureVisitor the signature visitor this remapper must delegate to.
   * @param remapper the remapper to use to remap the types in the visited signature.
   */
  public SignatureRemapper(final SignatureVisitor signatureVisitor, final Remapper remapper) {
    this.signatureVisitor = signatureVisitor;
    this.remapper = remapper;
  }

  @Override
  public void visitClassType(final String name) {
    classNames.add(name);
    signatureVisitor.visitClassType(remapper.mapType(name));
  }

  @Override
  public void visitInnerClassType(final String name) {
    final ArrayList<String> classNames = this.classNames;
    String outerClassName = classNames.remove(classNames.size() - 1);
    String className = outerClassName + '$' + name;
    classNames.add(className);
    String remappedOuter = remapper.mapType(outerClassName) + '$';
    String remappedName = remapper.mapType(className);
    int index = remappedName.startsWith(remappedOuter)
                ? remappedOuter.length()
                : remappedName.lastIndexOf('$') + 1;
    signatureVisitor.visitInnerClassType(remappedName.substring(index));
  }

  @Override
  public void visitFormalTypeParameter(final String name) {
    signatureVisitor.visitFormalTypeParameter(name);
  }

  @Override
  public void visitTypeVariable(final String name) {
    signatureVisitor.visitTypeVariable(name);
  }

  @Override
  public SignatureVisitor visitArrayType() {
    signatureVisitor.visitArrayType();
    return this;
  }

  @Override
  public void visitBaseType(final char descriptor) {
    signatureVisitor.visitBaseType(descriptor);
  }

  @Override
  public SignatureVisitor visitClassBound() {
    signatureVisitor.visitClassBound();
    return this;
  }

  @Override
  public SignatureVisitor visitExceptionType() {
    signatureVisitor.visitExceptionType();
    return this;
  }

  @Override
  public SignatureVisitor visitInterface() {
    signatureVisitor.visitInterface();
    return this;
  }

  @Override
  public SignatureVisitor visitInterfaceBound() {
    signatureVisitor.visitInterfaceBound();
    return this;
  }

  @Override
  public SignatureVisitor visitParameterType() {
    signatureVisitor.visitParameterType();
    return this;
  }

  @Override
  public SignatureVisitor visitReturnType() {
    signatureVisitor.visitReturnType();
    return this;
  }

  @Override
  public SignatureVisitor visitSuperclass() {
    signatureVisitor.visitSuperclass();
    return this;
  }

  @Override
  public void visitTypeArgument() {
    signatureVisitor.visitTypeArgument();
  }

  @Override
  public SignatureVisitor visitTypeArgument(final char wildcard) {
    signatureVisitor.visitTypeArgument(wildcard);
    return this;
  }

  @Override
  public void visitEnd() {
    signatureVisitor.visitEnd();
    classNames.remove(classNames.size() - 1);
  }
}
