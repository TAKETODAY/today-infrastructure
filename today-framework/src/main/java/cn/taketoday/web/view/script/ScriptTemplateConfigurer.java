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
 * An implementation of Framework Web's {@link ScriptTemplateConfig} for creating
 * a {@code ScriptEngine} for use in a web application.
 *
 * <pre class="code">
 * // Add the following to an &#64;Configuration class
 * &#64;Bean
 * public ScriptTemplateConfigurer mustacheConfigurer() {
 *    ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
 *    configurer.setEngineName("nashorn");
 *    configurer.setScripts("mustache.js");
 *    configurer.setRenderObject("Mustache");
 *    configurer.setRenderFunction("render");
 *    return configurer;
 * }
 * </pre>
 *
 * <p><b>NOTE:</b> It is possible to use non thread-safe script engines with
 * templating libraries not designed for concurrency, like Handlebars or React running on
 * Nashorn, by setting the {@link #setSharedEngine sharedEngine} property to {@code false}.
 *
 * @author Sebastien Deleuze
 * @see ScriptTemplateView
 * @since 4.0
 */
public class ScriptTemplateConfigurer implements ScriptTemplateConfig {

  @Nullable
  private ScriptEngine engine;

  @Nullable
  private Supplier<ScriptEngine> engineSupplier;

  @Nullable
  private String engineName;

  @Nullable
  private Boolean sharedEngine;

  @Nullable
  private String[] scripts;

  @Nullable
  private String renderObject;

  @Nullable
  private String renderFunction;

  @Nullable
  private String contentType;

  @Nullable
  private Charset charset;

  @Nullable
  private String resourceLoaderPath;

  /**
   * Default constructor.
   */
  public ScriptTemplateConfigurer() { }

  /**
   * Create a new ScriptTemplateConfigurer using the given engine name.
   */
  public ScriptTemplateConfigurer(String engineName) {
    this.engineName = engineName;
  }

  /**
   * Set the {@link ScriptEngine} to use by the view.
   * If {@code renderFunction} is specified, the script engine must implement {@code Invocable}.
   * You must define {@code engine} or {@code engineName}, not both.
   * <p>When the {@code sharedEngine} flag is set to {@code false}, you should not specify
   * the script engine with this setter, but with {@link #setEngineName(String)}
   * or {@link #setEngineSupplier(Supplier)} since it implies multiple lazy
   * instantiations of the script engine.
   *
   * @see #setEngineName(String)
   * @see #setEngineSupplier(Supplier)
   */
  public void setEngine(@Nullable ScriptEngine engine) {
    this.engine = engine;
  }

  @Override
  @Nullable
  public ScriptEngine getEngine() {
    return this.engine;
  }

  /**
   * Set the {@link ScriptEngine} supplier to use by the view, usually used with
   * {@link #setSharedEngine(Boolean)} set to {@code false}.
   * If {@code renderFunction} is specified, the script engine must implement {@code Invocable}.
   * You must either define {@code engineSupplier}, {@code engine} or {@code engineName}.
   *
   * @see #setEngine(ScriptEngine)
   * @see #setEngineName(String)
   */
  public void setEngineSupplier(@Nullable Supplier<ScriptEngine> engineSupplier) {
    this.engineSupplier = engineSupplier;
  }

  @Override
  @Nullable
  public Supplier<ScriptEngine> getEngineSupplier() {
    return this.engineSupplier;
  }

  /**
   * Set the engine name that will be used to instantiate the {@link ScriptEngine}.
   * If {@code renderFunction} is specified, the script engine must implement {@code Invocable}.
   * You must define {@code engine} or {@code engineName}, not both.
   *
   * @see #setEngine(ScriptEngine)
   * @see #setEngineSupplier(Supplier)
   */
  public void setEngineName(@Nullable String engineName) {
    this.engineName = engineName;
  }

  @Override
  @Nullable
  public String getEngineName() {
    return this.engineName;
  }

  /**
   * When set to {@code false}, use thread-local {@link ScriptEngine} instances instead
   * of one single shared instance. This flag should be set to {@code false} for those
   * using non thread-safe script engines with templating libraries not designed for
   * concurrency, like Handlebars or React running on Nashorn for example.
   * <p>When this flag is set to {@code false}, the script engine must be specified using
   * {@link #setEngineName(String)} or {@link #setEngineSupplier(Supplier)}.
   * Using {@link #setEngine(ScriptEngine)} is not possible because multiple instances
   * of the script engine need to be created lazily (one per thread).
   *
   * @see <a href="https://docs.oracle.com/javase/8/docs/api/javax/script/ScriptEngineFactory.html#getParameter-java.lang.String-">THREADING ScriptEngine parameter</a>
   */
  public void setSharedEngine(@Nullable Boolean sharedEngine) {
    this.sharedEngine = sharedEngine;
  }

  @Override
  @Nullable
  public Boolean isSharedEngine() {
    return this.sharedEngine;
  }

  /**
   * Set the scripts to be loaded by the script engine (library or user provided).
   * Since {@code resourceLoaderPath} default value is "classpath:", you can load easily
   * any script available on the classpath.
   * <p>For example, in order to use a JavaScript library available as a WebJars dependency
   * and a custom "render.js" file, you should call
   * {@code configurer.setScripts("/META-INF/resources/webjars/library/version/library.js",
   * "com/myproject/script/render.js");}.
   *
   * @see #setResourceLoaderPath
   * @see <a href="https://www.webjars.org">WebJars</a>
   */
  public void setScripts(@Nullable String... scriptNames) {
    this.scripts = scriptNames;
  }

  @Override
  @Nullable
  public String[] getScripts() {
    return this.scripts;
  }

  /**
   * Set the object where the render function belongs (optional).
   * For example, in order to call {@code Mustache.render()}, {@code renderObject}
   * should be set to {@code "Mustache"} and {@code renderFunction} to {@code "render"}.
   */
  public void setRenderObject(@Nullable String renderObject) {
    this.renderObject = renderObject;
  }

  @Override
  @Nullable
  public String getRenderObject() {
    return this.renderObject;
  }

  /**
   * Set the render function name (optional). If not specified, the script templates
   * will be evaluated with {@link ScriptEngine#eval(String, Bindings)}.
   * <p>This function will be called with the following parameters:
   * <ol>
   * <li>{@code String template}: the template content</li>
   * <li>{@code Map model}: the view model</li>
   * <li>{@code RenderingContext context}: the rendering context (since 5.0)</li>
   * </ol>
   *
   * @see RenderingContext
   */
  public void setRenderFunction(@Nullable String renderFunction) {
    this.renderFunction = renderFunction;
  }

  @Override
  @Nullable
  public String getRenderFunction() {
    return this.renderFunction;
  }

  /**
   * Set the content type to use for the response.
   * ({@code text/html} by default).
   */
  public void setContentType(@Nullable String contentType) {
    this.contentType = contentType;
  }

  /**
   * Return the content type to use for the response.
   */
  @Override
  @Nullable
  public String getContentType() {
    return this.contentType;
  }

  /**
   * Set the charset used to read script and template files.
   * ({@code UTF-8} by default).
   */
  public void setCharset(@Nullable Charset charset) {
    this.charset = charset;
  }

  @Override
  @Nullable
  public Charset getCharset() {
    return this.charset;
  }

  /**
   * Set the resource loader path(s) via a Framework resource location.
   * Accepts multiple locations as a comma-separated list of paths.
   * Standard URLs like "file:" and "classpath:" and pseudo URLs are supported
   * as understood by Framework's {@link cn.taketoday.core.io.ResourceLoader}.
   * Relative paths are allowed when running in an ApplicationContext.
   * <p>Default is "classpath:".
   */
  public void setResourceLoaderPath(@Nullable String resourceLoaderPath) {
    this.resourceLoaderPath = resourceLoaderPath;
  }

  @Override
  @Nullable
  public String getResourceLoaderPath() {
    return this.resourceLoaderPath;
  }

}
