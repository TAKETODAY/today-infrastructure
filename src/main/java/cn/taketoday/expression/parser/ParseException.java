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

import cn.taketoday.core.Constant;

/**
 * This exception is thrown when parse errors are encountered. You can
 * explicitly create objects of this exception type by calling the method
 * generateParseException in the generated parser.
 *
 * You can modify this class to customize your error reporting mechanisms so
 * long as you retain the public fields.
 */
@SuppressWarnings("serial")
public class ParseException extends Exception {

  /**
   * This constructor is used by the method "generateParseException" in the
   * generated parser. Calling this constructor generates a new object of this
   * type with the fields "currentToken", "expectedTokenSequences", and
   * "tokenImage" set. The boolean flag "specialConstructor" is also set to true
   * to indicate that this constructor was used to create this object. This
   * constructor calls its super class with the empty string to force the
   * "toString" method of parent class "Throwable" to print the error message in
   * the form: ParseException: <result of getMessage>
   */
  public ParseException(Token currentTokenVal, int[][] expectedTokenSequencesVal, String[] tokenImageVal) {
    super(Constant.BLANK);
    this.specialConstructor = true;
    this.currentToken = currentTokenVal;
    this.tokenImage = tokenImageVal;
    this.expectedTokenSequences = expectedTokenSequencesVal;
  }

  /**
   * The following constructors are for use by you for whatever purpose you can
   * think of. Constructing the exception in this manner makes the exception
   * behave in the normal way - i.e., as documented in the class "Throwable". The
   * fields "errorToken", "expectedTokenSequences", and "tokenImage" do not
   * contain relevant information. The JavaCC generated code does not use these
   * constructors.
   */
  public ParseException() {
    super();
    this.specialConstructor = false;
  }

  public ParseException(String message) {
    super(message);
    this.specialConstructor = false;
  }

  /**
   * This variable determines which constructor was used to create this object and
   * thereby affects the semantics of the "getMessage" method (see below).
   */
  protected boolean specialConstructor;

  /**
   * This is the last token that has been consumed successfully. If this object
   * has been created due to a parse error, the token followng this token will
   * (therefore) be the first error token.
   */
  public Token currentToken;

  /**
   * Each entry in this array is an array of integers. Each array of integers
   * represents a sequence of tokens (by their ordinal values) that is expected at
   * this point of the parse.
   */
  public int[][] expectedTokenSequences;

  /**
   * This is a reference to the "tokenImage" array of the generated parser within
   * which the parse error occurred. This array is defined in the generated
   * ...Constants interface.
   */
  public String[] tokenImage;

  /**
   * This method has the standard behavior when this object has been created using
   * the standard constructors. Otherwise, it uses "currentToken" and
   * "expectedTokenSequences" to generate a parse error message and returns it. If
   * this object has been created due to a parse error, and you do not catch it
   * (it gets thrown from the parser), then this method is called during the
   * printing of the final stack trace, and hence the correct error message gets
   * displayed.
   */
  @Override
  public String getMessage() {
    if (!specialConstructor) {
      return super.getMessage();
    }
//    String expected = Constant.BLANK;
    StringBuilder expected = new StringBuilder();
    int maxSize = 0;
    for (int i = 0; i < expectedTokenSequences.length; i++) {
      if (maxSize < expectedTokenSequences[i].length) {
        maxSize = expectedTokenSequences[i].length;
      }
      for (int j = 0; j < expectedTokenSequences[i].length; j++) {
        expected.append(tokenImage[expectedTokenSequences[i][j]]).append(" ");
      }
      if (expectedTokenSequences[i][expectedTokenSequences[i].length - 1] != 0) {
        expected.append("...");
      }
      expected.append(eol).append("    ");
    }
    StringBuilder ret = new StringBuilder("Encountered \"");

//    String retval = "Encountered \"";
    Token tok = currentToken.next;
    for (int i = 0; i < maxSize; i++) {
      if (i != 0) {
        ret.append(" ");
      }
      if (tok.kind == 0) {
        ret.append(tokenImage[0]);
        break;
      }
      ret.append(add_escapes(tok.image));
      tok = tok.next;
    }
    ret.append("\" at line ").append(currentToken.next.beginLine).append(", column ").append(currentToken.next.beginColumn);

//    retval += "\" at line " + currentToken.next.beginLine + ", column " + currentToken.next.beginColumn;
//    retval += '.' + eol;
    ret.append('.').append(eol);
    if (expectedTokenSequences.length == 1) {
      ret.append("Was expecting:").append(eol).append("    ");
//      retval += "Was expecting:" + eol + "    ";
    }
    else {
      ret.append("Was expecting one of:").append(eol).append("    ");
//      retval += "Was expecting one of:" + eol + "    ";
    }
    ret.append(expected);
//    retval += expected;
    return ret.toString();
  }

  /**
   * The end of line string for this machine.
   */
  protected String eol = System.getProperty("line.separator", "\n");

  /**
   * Used to convert raw characters to their escaped version when these raw
   * version cannot be used as part of an ASCII string literal.
   */
  protected String add_escapes(String str) {
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
            String s = "0000".concat(Integer.toString(ch, 16));
            retval.append("\\u" + s.substring(s.length() - 4, s.length()));
          }
          else {
            retval.append(ch);
          }
          continue;
      }
    }
    return retval.toString();
  }

}
