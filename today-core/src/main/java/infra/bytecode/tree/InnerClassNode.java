/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package infra.bytecode.tree;

import org.jspecify.annotations.Nullable;

import infra.bytecode.ClassVisitor;
import infra.bytecode.Type;

/**
 * A node that represents an inner class.
 *
 * @author Eric Bruneton
 */
public class InnerClassNode {

  /** The internal name of an inner class (see {@link Type#getInternalName()}). */
  public String name;

  /**
   * The internal name of the class to which the inner class belongs (see {@link
   * Type#getInternalName()}). May be {@literal null}.
   */
  @Nullable
  public String outerName;

  /**
   * The (simple) name of the inner class inside its enclosing class. May be {@literal null} for
   * anonymous inner classes.
   */
  @Nullable
  public String innerName;

  /** The access flags of the inner class as originally declared in the enclosing class. */
  public int access;

  /**
   * Constructs a new {@link InnerClassNode}.
   *
   * @param name the internal name of an inner class (see {@link
   * Type#getInternalName()}).
   * @param outerName the internal name of the class to which the inner class belongs (see {@link
   * Type#getInternalName()}). May be {@literal null}.
   * @param innerName the (simple) name of the inner class inside its enclosing class. May be
   * {@literal null} for anonymous inner classes.
   * @param access the access flags of the inner class as originally declared in the enclosing
   * class.
   */
  public InnerClassNode(final String name, @Nullable final String outerName, @Nullable final String innerName, final int access) {
    this.name = name;
    this.outerName = outerName;
    this.innerName = innerName;
    this.access = access;
  }

  /**
   * Makes the given class visitor visit this inner class.
   *
   * @param classVisitor a class visitor.
   */
  public void accept(final ClassVisitor classVisitor) {
    classVisitor.visitInnerClass(name, outerName, innerName, access);
  }
}
