/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package cn.taketoday.expression.parser;

@SuppressWarnings("serial")
public class TokenMgrError extends Error {
  /*
   * Ordinals for various reasons why an Error of this type can be thrown.
   */

  /**
   * Lexical error occured.
   */
  static final int LEXICAL_ERROR = 0;

  /**
   * An attempt wass made to create a second instance of a static token manager.
   */
  static final int STATIC_LEXER_ERROR = 1;

  /**
   * Tried to change to an invalid lexical state.
   */
  static final int INVALID_LEXICAL_STATE = 2;

  /**
   * Detected (and bailed out of) an infinite loop in the token manager.
   */
  static final int LOOP_DETECTED = 3;

  /**
   * Indicates the reason why the exception is thrown. It will have one of the
   * above 4 values.
   */
  int errorCode;

  /**
   * Replaces unprintable characters by their espaced (or unicode escaped)
   * equivalents in the given string
   */
  protected static String addEscapes(String str) {
    StringBuilder retval = new StringBuilder();
    char ch;
    final int length = str.length();
    for (int i = 0; i < length; i++) {
      switch (str.charAt(i)) {
        case 0:
          continue;
        case '\b':
          retval.append("\\b");
          continue;
        case '\t':
          retval.append("\\t");
          continue;
        case '\n':
          retval.append("\\n");
          continue;
        case '\f':
          retval.append("\\f");
          continue;
        case '\r':
          retval.append("\\r");
          continue;
        case '\"':
          retval.append("\\\"");
          continue;
        case '\'':
          retval.append("\\\'");
          continue;
        case '\\':
          retval.append("\\\\");
          continue;
        default:
          if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
            String s = "0000" + Integer.toString(ch, 16);
            retval.append("\\u").append(s.substring(s.length() - 4));
          }
          else {
            retval.append(ch);
          }
      }
    }
    return retval.toString();
  }

  /**
   * Returns a detailed message for the Error when it is thrown by the token
   * manager to indicate a lexical error. Parameters : EOFSeen : indicates if EOF
   * caused the lexicl error curLexState : lexical state in which this error
   * occured errorLine : line number when the error occured errorColumn : column
   * number when the error occured errorAfter : prefix that was seen before this
   * error occured curchar : the offending character Note: You can customize the
   * lexical error message by modifying this method.
   */
  protected static String LexicalError(boolean EOFSeen,
                                       int lexState,
                                       int errorLine,
                                       int errorColumn,
                                       String errorAfter,
                                       char curChar) {
    return ("Lexical error at line " + errorLine + ", " +
            "column " + errorColumn + ".  Encountered: " + (EOFSeen
                                                            ? "<EOF> "
                                                            : ("\"" + addEscapes(String.valueOf(curChar)) + "\"")
                                                                    + " (" + (int) curChar + "), ") + "after : \""
            + addEscapes(errorAfter) + "\"");
  }

  /**
   * You can also modify the body of this method to customize your error messages.
   * For example, cases like LOOP_DETECTED and INVALID_LEXICAL_STATE are not of
   * end-users concern, so you can return something like :
   *
   * "Internal Error : Please file a bug report .... "
   *
   * from this method for such cases in the release version of your parser.
   */
  public String getMessage() {
    return super.getMessage();
  }

  /*
   * Constructors of various flavors follow.
   */

  public TokenMgrError() { }

  public TokenMgrError(String message, int reason) {
    super(message);
    errorCode = reason;
  }

  public TokenMgrError(boolean EOFSeen, int lexState, int errorLine, int errorColumn, String errorAfter, char curChar, int reason) {
    this(LexicalError(EOFSeen, lexState, errorLine, errorColumn, errorAfter, curChar), reason);
  }
}
