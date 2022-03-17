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
package cn.taketoday.core.bytecode.signature;

import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;

/**
 * A parser for signature literals, as defined in the Java Virtual Machine Specification (JVMS), to
 * visit them with a SignatureVisitor.
 *
 * @author Thomas Hallgren
 * @author Eric Bruneton
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9.1">JVMS
 * 4.7.9.1</a>
 */
public class SignatureReader {

  /** The JVMS signature to be read. */
  private final String signatureValue;

  /**
   * Constructs a {@link SignatureReader} for the given signature.
   *
   * @param signature A <i>JavaTypeSignature</i>, <i>ClassSignature</i> or <i>MethodSignature</i>.
   */
  public SignatureReader(final String signature) {
    this.signatureValue = signature;
  }

  /**
   * Makes the given visitor visit the signature of this {@link SignatureReader}. This signature is
   * the one specified in the constructor (see {@link #SignatureReader}). This method is intended to
   * be called on a {@link SignatureReader} that was created using a <i>ClassSignature</i> (such as
   * the <code>signature</code> parameter of the {@link ClassVisitor#visit}
   * method) or a <i>MethodSignature</i> (such as the <code>signature</code> parameter of the {@link
   * ClassVisitor#visitMethod} method).
   *
   * @param signatureVisitor the visitor that must visit this signature.
   */
  public void accept(final SignatureVisitor signatureVisitor) {
    String signature = this.signatureValue;
    int length = signature.length();
    int offset; // Current offset in the parsed signature (parsed from left to right).
    char currentChar; // The signature character at 'offset', or just before.

    // If the signature starts with '<', it starts with TypeParameters, i.e. a formal type parameter
    // identifier, followed by one or more pair ':',ReferenceTypeSignature (for its class bound and
    // interface bounds).
    if (signature.charAt(0) == '<') {
      // Invariant: offset points to the second character of a formal type parameter name at the
      // beginning of each iteration of the loop below.
      offset = 2;
      do {
        // The formal type parameter name is everything between offset - 1 and the first ':'.
        int classBoundStartOffset = signature.indexOf(':', offset);
        signatureVisitor.visitFormalTypeParameter(
                signature.substring(offset - 1, classBoundStartOffset));

        // If the character after the ':' class bound marker is not the start of a
        // ReferenceTypeSignature, it means the class bound is empty (which is a valid case).
        offset = classBoundStartOffset + 1;
        currentChar = signature.charAt(offset);
        if (currentChar == 'L' || currentChar == '[' || currentChar == 'T') {
          offset = parseType(signature, offset, signatureVisitor.visitClassBound());
        }

        // While the character after the class bound or after the last parsed interface bound
        // is ':', we need to parse another interface bound.
        while ((currentChar = signature.charAt(offset++)) == ':') {
          offset = parseType(signature, offset, signatureVisitor.visitInterfaceBound());
        }

        // At this point a TypeParameter has been fully parsed, and we need to parse the next one
        // (note that currentChar is now the first character of the next TypeParameter, and that
        // offset points to the second character), unless the character just after this
        // TypeParameter signals the end of the TypeParameters.
      }
      while (currentChar != '>');
    }
    else {
      offset = 0;
    }

    // If the (optional) TypeParameters is followed by '(' this means we are parsing a
    // MethodSignature, which has JavaTypeSignature type inside parentheses, followed by a Result
    // type and optional ThrowsSignature types.
    if (signature.charAt(offset) == '(') {
      offset++;
      while (signature.charAt(offset) != ')') {
        offset = parseType(signature, offset, signatureVisitor.visitParameterType());
      }
      // Use offset + 1 to skip ')'.
      offset = parseType(signature, offset + 1, signatureVisitor.visitReturnType());
      while (offset < length) {
        // Use offset + 1 to skip the first character of a ThrowsSignature, i.e. '^'.
        offset = parseType(signature, offset + 1, signatureVisitor.visitExceptionType());
      }
    }
    else {
      // Otherwise we are parsing a ClassSignature (by hypothesis on the method input), which has
      // one or more ClassTypeSignature for the super class and the implemented interfaces.
      offset = parseType(signature, offset, signatureVisitor.visitSuperclass());
      while (offset < length) {
        offset = parseType(signature, offset, signatureVisitor.visitInterface());
      }
    }
  }

  /**
   * Makes the given visitor visit the signature of this {@link SignatureReader}. This signature is
   * the one specified in the constructor (see {@link #SignatureReader}). This method is intended to
   * be called on a {@link SignatureReader} that was created using a <i>JavaTypeSignature</i>, such
   * as the <code>signature</code> parameter of the {@link
   * ClassVisitor#visitField} or {@link
   * MethodVisitor#visitLocalVariable} methods.
   *
   * @param signatureVisitor the visitor that must visit this signature.
   */
  public void acceptType(final SignatureVisitor signatureVisitor) {
    parseType(signatureValue, 0, signatureVisitor);
  }

  /**
   * Parses a JavaTypeSignature and makes the given visitor visit it.
   *
   * @param signature a string containing the signature that must be parsed.
   * @param startOffset index of the first character of the signature to parsed.
   * @param signatureVisitor the visitor that must visit this signature.
   * @return the index of the first character after the parsed signature.
   */
  private static int parseType(
          final String signature, final int startOffset, final SignatureVisitor signatureVisitor) {
    int offset = startOffset; // Current offset in the parsed signature.
    char currentChar = signature.charAt(offset++); // The signature character at 'offset'.

    // Switch based on the first character of the JavaTypeSignature, which indicates its kind.
    switch (currentChar) {
      case 'Z':
      case 'C':
      case 'B':
      case 'S':
      case 'I':
      case 'F':
      case 'J':
      case 'D':
      case 'V':
        // Case of a BaseType or a VoidDescriptor.
        signatureVisitor.visitBaseType(currentChar);
        return offset;

      case '[':
        // Case of an ArrayTypeSignature, a '[' followed by a JavaTypeSignature.
        return parseType(signature, offset, signatureVisitor.visitArrayType());

      case 'T':
        // Case of TypeVariableSignature, an identifier between 'T' and ';'.
        int endOffset = signature.indexOf(';', offset);
        signatureVisitor.visitTypeVariable(signature.substring(offset, endOffset));
        return endOffset + 1;

      case 'L':
        // Case of a ClassTypeSignature, which ends with ';'.
        // These signatures have a main class type followed by zero or more inner class types
        // (separated by '.'). Each can have type arguments, inside '<' and '>'.
        int start = offset; // The start offset of the currently parsed main or inner class name.
        boolean visited = false; // Whether the currently parsed class name has been visited.
        boolean inner = false; // Whether we are currently parsing an inner class type.
        // Parses the signature, one character at a time.
        while (true) {
          currentChar = signature.charAt(offset++);
          if (currentChar == '.' || currentChar == ';') {
            // If a '.' or ';' is encountered, this means we have fully parsed the main class name
            // or an inner class name. This name may already have been visited it is was followed by
            // type arguments between '<' and '>'. If not, we need to visit it here.
            if (!visited) {
              String name = signature.substring(start, offset - 1);
              if (inner) {
                signatureVisitor.visitInnerClassType(name);
              }
              else {
                signatureVisitor.visitClassType(name);
              }
            }
            // If we reached the end of the ClassTypeSignature return, otherwise start the parsing
            // of a new class name, which is necessarily an inner class name.
            if (currentChar == ';') {
              signatureVisitor.visitEnd();
              break;
            }
            start = offset;
            visited = false;
            inner = true;
          }
          else if (currentChar == '<') {
            // If a '<' is encountered, this means we have fully parsed the main class name or an
            // inner class name, and that we now need to parse TypeArguments. First, we need to
            // visit the parsed class name.
            String name = signature.substring(start, offset - 1);
            if (inner) {
              signatureVisitor.visitInnerClassType(name);
            }
            else {
              signatureVisitor.visitClassType(name);
            }
            visited = true;
            // Now, parse the TypeArgument(s), one at a time.
            while ((currentChar = signature.charAt(offset)) != '>') {
              switch (currentChar) {
                case '*' -> {
                  // Unbounded TypeArgument.
                  ++offset;
                  signatureVisitor.visitTypeArgument();
                }
                case '+', '-' ->
                        // Extends or Super TypeArgument. Use offset + 1 to skip the '+' or '-'.
                        offset = parseType(signature, offset + 1, signatureVisitor.visitTypeArgument(currentChar));
                default ->
                        // Instanceof TypeArgument. The '=' is implicit.
                        offset = parseType(signature, offset, signatureVisitor.visitTypeArgument('='));
              }
            }
          }
        }
        return offset;

      default:
        throw new IllegalArgumentException();
    }
  }
}
