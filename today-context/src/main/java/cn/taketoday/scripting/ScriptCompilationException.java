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
package cn.taketoday.scripting;

import cn.taketoday.core.NestedRuntimeException;
import cn.taketoday.lang.Nullable;

/**
 * Exception to be thrown on script compilation failure.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ScriptCompilationException extends NestedRuntimeException {

  @Nullable
  private final ScriptSource scriptSource;

  /**
   * Constructor for ScriptCompilationException.
   *
   * @param msg the detail message
   */
  public ScriptCompilationException(String msg) {
    super(msg);
    this.scriptSource = null;
  }

  /**
   * Constructor for ScriptCompilationException.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using an underlying script compiler API)
   */
  public ScriptCompilationException(String msg, Throwable cause) {
    super(msg, cause);
    this.scriptSource = null;
  }

  /**
   * Constructor for ScriptCompilationException.
   *
   * @param scriptSource the source for the offending script
   * @param msg the detail message
   * @since 4.0
   */
  public ScriptCompilationException(ScriptSource scriptSource, String msg) {
    super("Could not compile " + scriptSource + ": " + msg);
    this.scriptSource = scriptSource;
  }

  /**
   * Constructor for ScriptCompilationException.
   *
   * @param scriptSource the source for the offending script
   * @param cause the root cause (usually from using an underlying script compiler API)
   */
  public ScriptCompilationException(ScriptSource scriptSource, Throwable cause) {
    super("Could not compile " + scriptSource, cause);
    this.scriptSource = scriptSource;
  }

  /**
   * Constructor for ScriptCompilationException.
   *
   * @param scriptSource the source for the offending script
   * @param msg the detail message
   * @param cause the root cause (usually from using an underlying script compiler API)
   */
  public ScriptCompilationException(ScriptSource scriptSource, String msg, Throwable cause) {
    super("Could not compile " + scriptSource + ": " + msg, cause);
    this.scriptSource = scriptSource;
  }

  /**
   * Return the source for the offending script.
   *
   * @return the source, or {@code null} if not available
   */
  @Nullable
  public ScriptSource getScriptSource() {
    return this.scriptSource;
  }

}
