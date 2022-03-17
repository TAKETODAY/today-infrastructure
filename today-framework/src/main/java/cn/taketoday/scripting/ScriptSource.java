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

import java.io.IOException;

import cn.taketoday.lang.Nullable;

/**
 * Interface that defines the source of a script.
 * Tracks whether the underlying script has been modified.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface ScriptSource {

  /**
   * Retrieve the current script source text as String.
   *
   * @return the script text
   * @throws IOException if script retrieval failed
   */
  String getScriptAsString() throws IOException;

  /**
   * Indicate whether the underlying script data has been modified since
   * the last time {@link #getScriptAsString()} was called.
   * Returns {@code true} if the script has not been read yet.
   *
   * @return whether the script data has been modified
   */
  boolean isModified();

  /**
   * Determine a class name for the underlying script.
   *
   * @return the suggested class name, or {@code null} if none available
   */
  @Nullable
  String suggestedClassName();

}
