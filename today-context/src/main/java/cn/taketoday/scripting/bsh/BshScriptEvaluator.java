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
package cn.taketoday.scripting.bsh;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import bsh.EvalError;
import bsh.Interpreter;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scripting.ScriptCompilationException;
import cn.taketoday.scripting.ScriptEvaluator;
import cn.taketoday.scripting.ScriptSource;

/**
 * BeanShell-based implementation of Framework's {@link ScriptEvaluator} strategy interface.
 *
 * @author Juergen Hoeller
 * @see Interpreter#eval(String)
 * @since 4.0
 */
public class BshScriptEvaluator implements ScriptEvaluator, BeanClassLoaderAware {

  @Nullable
  private ClassLoader classLoader;

  /**
   * Construct a new BshScriptEvaluator.
   */
  public BshScriptEvaluator() {
  }

  /**
   * Construct a new BshScriptEvaluator.
   *
   * @param classLoader the ClassLoader to use for the {@link Interpreter}
   */
  public BshScriptEvaluator(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  @Nullable
  public Object evaluate(ScriptSource script) {
    return evaluate(script, null);
  }

  @Override
  @Nullable
  public Object evaluate(ScriptSource script, @Nullable Map<String, Object> arguments) {
    try {
      Interpreter interpreter = new Interpreter();
      interpreter.setClassLoader(this.classLoader);
      if (arguments != null) {
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
          interpreter.set(entry.getKey(), entry.getValue());
        }
      }
      return interpreter.eval(new StringReader(script.getScriptAsString()));
    }
    catch (IOException ex) {
      throw new ScriptCompilationException(script, "Cannot access BeanShell script", ex);
    }
    catch (EvalError ex) {
      throw new ScriptCompilationException(script, ex);
    }
  }

}
