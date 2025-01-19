/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.scripting.support;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

/**
 * Common operations for dealing with a JSR-223 {@link ScriptEngine}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public abstract class StandardScriptUtils {

  /**
   * Retrieve a {@link ScriptEngine} from the given {@link ScriptEngineManager}
   * by name, delegating to {@link ScriptEngineManager#getEngineByName} but
   * throwing a descriptive exception if not found or if initialization failed.
   *
   * @param scriptEngineManager the ScriptEngineManager to use
   * @param engineName the name of the engine
   * @return a corresponding ScriptEngine (never {@code null})
   * @throws IllegalArgumentException if no matching engine has been found
   * @throws IllegalStateException if the desired engine failed to initialize
   */
  public static ScriptEngine retrieveEngineByName(ScriptEngineManager scriptEngineManager, String engineName) {
    ScriptEngine engine = scriptEngineManager.getEngineByName(engineName);
    if (engine == null) {
      LinkedHashSet<String> engineNames = new LinkedHashSet<>();
      for (ScriptEngineFactory engineFactory : scriptEngineManager.getEngineFactories()) {
        List<String> factoryNames = engineFactory.getNames();
        if (factoryNames.contains(engineName)) {
          // Special case: getEngineByName returned null but engine is present...
          // Let's assume it failed to initialize (which ScriptEngineManager silently swallows).
          // If it happens to initialize fine now, alright, but we really expect an exception.
          try {
            engine = engineFactory.getScriptEngine();
            engine.setBindings(scriptEngineManager.getBindings(), ScriptContext.GLOBAL_SCOPE);
          }
          catch (Throwable ex) {
            throw new IllegalStateException(
                    "Script engine with name '%s' failed to initialize".formatted(engineName), ex);
          }
        }
        engineNames.addAll(factoryNames);
      }
      throw new IllegalArgumentException("Script engine with name '%s' not found; registered engine names: %s"
              .formatted(engineName, engineNames));
    }
    return engine;
  }

  static Bindings getBindings(Map<String, Object> bindings) {
    return (bindings instanceof Bindings ? (Bindings) bindings : new SimpleBindings(bindings));
  }

}
