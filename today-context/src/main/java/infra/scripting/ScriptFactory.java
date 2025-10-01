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

package infra.scripting;

import org.jspecify.annotations.Nullable;

import java.io.IOException;

import infra.core.io.ResourceLoader;
import infra.scripting.support.ScriptFactoryPostProcessor;

/**
 * Script definition interface, encapsulating the configuration
 * of a specific script as well as a factory method for
 * creating the actual scripted Java {@code Object}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see #getScriptSourceLocator
 * @see #getScriptedObject
 * @since 4.0
 */
public interface ScriptFactory {

  /**
   * Return a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   * <p>Typical supported locators are Framework resource locations
   * (such as "file:C:/myScript.bsh" or "classpath:myPackage/myScript.bsh")
   * and inline scripts ("inline:myScriptText...").
   *
   * @return the script source locator
   * @see ScriptFactoryPostProcessor#convertToScriptSource
   * @see ResourceLoader
   */
  String getScriptSourceLocator();

  /**
   * Return the business interfaces that the script is supposed to implement.
   * <p>Can return {@code null} if the script itself determines
   * its Java interfaces (such as in the case of Groovy).
   *
   * @return the interfaces for the script
   */
  Class<?> @Nullable [] getScriptInterfaces();

  /**
   * Return whether the script requires a config interface to be
   * generated for it. This is typically the case for scripts that
   * do not determine Java signatures themselves, with no appropriate
   * config interface specified in {@code getScriptInterfaces()}.
   *
   * @return whether the script requires a generated config interface
   * @see #getScriptInterfaces()
   */
  boolean requiresConfigInterface();

  /**
   * Factory method for creating the scripted Java object.
   * <p>Implementations are encouraged to cache script metadata such as
   * a generated script class. Note that this method may be invoked
   * concurrently and must be implemented in a thread-safe fashion.
   *
   * @param scriptSource the actual ScriptSource to retrieve
   * the script source text from (never {@code null})
   * @param actualInterfaces the actual interfaces to expose,
   * including script interfaces as well as a generated config interface
   * (if applicable; may be {@code null})
   * @return the scripted Java object
   * @throws IOException if script retrieval failed
   * @throws ScriptCompilationException if script compilation failed
   */
  @Nullable
  Object getScriptedObject(ScriptSource scriptSource, Class<?> @Nullable ... actualInterfaces)
          throws IOException, ScriptCompilationException;

  /**
   * Determine the type of the scripted Java object.
   * <p>Implementations are encouraged to cache script metadata such as
   * a generated script class. Note that this method may be invoked
   * concurrently and must be implemented in a thread-safe fashion.
   *
   * @param scriptSource the actual ScriptSource to retrieve
   * the script source text from (never {@code null})
   * @return the type of the scripted Java object, or {@code null}
   * if none could be determined
   * @throws IOException if script retrieval failed
   * @throws ScriptCompilationException if script compilation failed
   */
  @Nullable
  Class<?> getScriptedObjectType(ScriptSource scriptSource)
          throws IOException, ScriptCompilationException;

  /**
   * Determine whether a refresh is required (e.g. through
   * ScriptSource's {@code isModified()} method).
   *
   * @param scriptSource the actual ScriptSource to retrieve
   * the script source text from (never {@code null})
   * @return whether a fresh {@link #getScriptedObject} call is required
   * @see ScriptSource#isModified()
   */
  boolean requiresScriptedObjectRefresh(ScriptSource scriptSource);

}
