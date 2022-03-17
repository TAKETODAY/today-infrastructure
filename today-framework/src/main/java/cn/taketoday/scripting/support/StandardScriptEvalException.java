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
package cn.taketoday.scripting.support;

import javax.script.ScriptException;

/**
 * Exception decorating a {@link ScriptException} coming out of
 * JSR-223 script evaluation, i.e. a {@link javax.script.ScriptEngine#eval}
 * call or {@link javax.script.Invocable#invokeMethod} /
 * {@link javax.script.Invocable#invokeFunction} call.
 *
 * <p>This exception does not print the Java stacktrace, since the JSR-223
 * {@link ScriptException} results in a rather convoluted text output.
 * From that perspective, this exception is primarily a decorator for a
 * {@link ScriptException} root cause passed into an outer exception.
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.0
 */
@SuppressWarnings("serial")
public class StandardScriptEvalException extends RuntimeException {

  private final ScriptException scriptException;

  /**
   * Construct a new script eval exception with the specified original exception.
   */
  public StandardScriptEvalException(ScriptException ex) {
    super(ex.getMessage());
    this.scriptException = ex;
  }

  public final ScriptException getScriptException() {
    return this.scriptException;
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

}
