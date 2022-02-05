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

package cn.taketoday.web.view.script;

import java.nio.charset.Charset;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import cn.taketoday.lang.Nullable;

/**
 * Interface to be implemented by objects that configure and manage a
 * JSR-223 {@link ScriptEngine} for automatic lookup in a web environment.
 * Detected and used by {@link ScriptTemplateView}.
 *
 * @author Sebastien Deleuze
 * @since 4.0
 */
public interface ScriptTemplateConfig {

  /**
   * Return the {@link ScriptEngine} to use by the views.
   */
  @Nullable
  ScriptEngine getEngine();

  /**
   * Return the engine supplier that will be used to instantiate the {@link ScriptEngine}.
   */
  @Nullable
  Supplier<ScriptEngine> getEngineSupplier();

  /**
   * Return the engine name that will be used to instantiate the {@link ScriptEngine}.
   */
  @Nullable
  String getEngineName();

  /**
   * Return whether to use a shared engine for all threads or whether to create
   * thread-local engine instances for each thread.
   */
  @Nullable
  Boolean isSharedEngine();

  /**
   * Return the scripts to be loaded by the script engine (library or user provided).
   */
  @Nullable
  String[] getScripts();

  /**
   * Return the object where the render function belongs (optional).
   */
  @Nullable
  String getRenderObject();

  /**
   * Return the render function name (optional). If not specified, the script templates
   * will be evaluated with {@link ScriptEngine#eval(String, Bindings)}.
   */
  @Nullable
  String getRenderFunction();

  /**
   * Return the content type to use for the response.
   */
  @Nullable
  String getContentType();

  /**
   * Return the charset used to read script and template files.
   */
  @Nullable
  Charset getCharset();

  /**
   * Return the resource loader path(s) via a Framework resource location.
   */
  @Nullable
  String getResourceLoaderPath();

}
