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

import java.util.Map;

import cn.taketoday.lang.Nullable;

/**
 * Framework's strategy interface for evaluating a script.
 *
 * <p>Aside from language-specific implementations, Framework also ships
 * a version based on the standard {@code javax.script} package (JSR-223):
 * {@link cn.taketoday.scripting.support.StandardScriptEvaluator}.
 *
 * @author Juergen Hoeller
 * @author Costin Leau
 * @since 4.0
 */
public interface ScriptEvaluator {

  /**
   * Evaluate the given script.
   *
   * @param script the ScriptSource for the script to evaluate
   * @return the return value of the script, if any
   * @throws ScriptCompilationException if the evaluator failed to read,
   * compile or evaluate the script
   */
  @Nullable
  Object evaluate(ScriptSource script) throws ScriptCompilationException;

  /**
   * Evaluate the given script with the given arguments.
   *
   * @param script the ScriptSource for the script to evaluate
   * @param arguments the key-value pairs to expose to the script,
   * typically as script variables (may be {@code null} or empty)
   * @return the return value of the script, if any
   * @throws ScriptCompilationException if the evaluator failed to read,
   * compile or evaluate the script
   */
  @Nullable
  Object evaluate(ScriptSource script, @Nullable Map<String, Object> arguments) throws ScriptCompilationException;

}
