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

package infra.scripting.groovy;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import infra.beans.factory.BeanClassLoaderAware;
import infra.scripting.ScriptCompilationException;
import infra.scripting.ScriptEvaluator;
import infra.scripting.ScriptSource;
import infra.scripting.support.ResourceScriptSource;

/**
 * Groovy-based implementation of Framework's {@link ScriptEvaluator} strategy interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see GroovyShell#evaluate(String, String)
 * @since 4.0
 */
public class GroovyScriptEvaluator implements ScriptEvaluator, BeanClassLoaderAware {

  @Nullable
  private ClassLoader classLoader;

  private CompilerConfiguration compilerConfiguration = new CompilerConfiguration();

  /**
   * Construct a new GroovyScriptEvaluator.
   */
  public GroovyScriptEvaluator() { }

  /**
   * Construct a new GroovyScriptEvaluator.
   *
   * @param classLoader the ClassLoader to use as a parent for the {@link GroovyShell}
   */
  public GroovyScriptEvaluator(@Nullable ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Set a custom compiler configuration for this evaluator.
   *
   * @see #setCompilationCustomizers
   */
  public void setCompilerConfiguration(@Nullable CompilerConfiguration compilerConfiguration) {
    this.compilerConfiguration =
            compilerConfiguration != null ? compilerConfiguration : new CompilerConfiguration();
  }

  /**
   * Return this evaluator's compiler configuration (never {@code null}).
   *
   * @see #setCompilerConfiguration
   */
  public CompilerConfiguration getCompilerConfiguration() {
    return this.compilerConfiguration;
  }

  /**
   * Set one or more customizers to be applied to this evaluator's compiler configuration.
   * <p>Note that this modifies the shared compiler configuration held by this evaluator.
   *
   * @see #setCompilerConfiguration
   */
  public void setCompilationCustomizers(CompilationCustomizer... compilationCustomizers) {
    this.compilerConfiguration.addCompilationCustomizers(compilationCustomizers);
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
    GroovyShell groovyShell = new GroovyShell(
            this.classLoader, new Binding(arguments), this.compilerConfiguration);
    try {
      String filename = script instanceof ResourceScriptSource ?
              ((ResourceScriptSource) script).getResource().getName() : null;
      if (filename != null) {
        return groovyShell.evaluate(script.getScriptAsString(), filename);
      }
      else {
        return groovyShell.evaluate(script.getScriptAsString());
      }
    }
    catch (IOException ex) {
      throw new ScriptCompilationException(script, "Cannot access Groovy script", ex);
    }
    catch (GroovyRuntimeException ex) {
      throw new ScriptCompilationException(script, ex);
    }
  }

}
