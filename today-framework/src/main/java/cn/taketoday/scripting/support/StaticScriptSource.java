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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scripting.ScriptSource;

/**
 * Static implementation of the
 * {@link ScriptSource} interface,
 * encapsulating a given String that contains the script source text.
 * Supports programmatic updates of the script String.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public class StaticScriptSource implements ScriptSource {

  private String script = "";

  private boolean modified;

  @Nullable
  private String className;

  /**
   * Create a new StaticScriptSource for the given script.
   *
   * @param script the script String
   */
  public StaticScriptSource(String script) {
    setScript(script);
  }

  /**
   * Create a new StaticScriptSource for the given script.
   *
   * @param script the script String
   * @param className the suggested class name for the script
   * (may be {@code null})
   */
  public StaticScriptSource(String script, @Nullable String className) {
    setScript(script);
    this.className = className;
  }

  /**
   * Set a fresh script String, overriding the previous script.
   *
   * @param script the script String
   */
  public synchronized void setScript(String script) {
    Assert.hasText(script, "Script must not be empty");
    this.modified = !script.equals(this.script);
    this.script = script;
  }

  @Override
  public synchronized String getScriptAsString() {
    this.modified = false;
    return this.script;
  }

  @Override
  public synchronized boolean isModified() {
    return this.modified;
  }

  @Override
  @Nullable
  public String suggestedClassName() {
    return this.className;
  }

  @Override
  public String toString() {
    return "static script" + (this.className != null ? " [" + this.className + "]" : "");
  }

}
