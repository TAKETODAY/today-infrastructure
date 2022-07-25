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

package org.aopalliance.aop;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Superclass for all AOP infrastructure exceptions. Unchecked, as such
 * exceptions are fatal and end user code shouldn't be forced to catch them.
 *
 * @author Rod Johnson
 * @author Bob Lee
 */
public class AspectException extends RuntimeException {

  private static final long serialVersionUID = 371663334385751868L;

  private String message;

  private String stackTrace;

  private Throwable t;

  /**
   * Constructor for AspectException.
   *
   * @param s
   */
  public AspectException(String s) {
    super(s);
    this.message = s;
    this.stackTrace = s;
  }

  /**
   * Constructor for AspectException.
   *
   * @param s
   * @param t
   */
  public AspectException(String s, Throwable t) {
    super(s + "; nested exception is " + t.getMessage());
    this.t = t;
    StringWriter out = new StringWriter();
    t.printStackTrace(new PrintWriter(out));
    this.stackTrace = out.toString();
  }

  /**
   * Return the root cause of this exception. May be null
   *
   * @return Throwable
   */
  public Throwable getCause() {
    return t;
  }

  public String toString() {
    return this.getMessage();
  }

  public String getMessage() {
    return this.message;
  }

  public void printStackTrace() {
    System.err.print(this.stackTrace);
  }

  public void printStackTrace(PrintStream out) {
    printStackTrace(new PrintWriter(out));
  }

  public void printStackTrace(PrintWriter out) {
    out.print(this.stackTrace);
  }

}
