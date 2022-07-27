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

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scripting.support.StandardScriptEvalException;
import cn.taketoday.scripting.support.StandardScriptUtils;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.view.AbstractUrlBasedView;
import jakarta.servlet.ServletException;

/**
 * An {@link AbstractUrlBasedView} subclass designed to run any template library
 * based on a JSR-223 script engine.
 *
 * <p>If not set, each property is auto-detected by looking up a single
 * {@link ScriptTemplateConfig} bean in the web application context and using
 * it to obtain the configured properties.
 *
 * <p>The Nashorn JavaScript engine requires Java 8+ and may require setting the
 * {@code sharedEngine} property to {@code false} in order to run properly. See
 * {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} for more details.
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @see ScriptTemplateConfigurer
 * @see ScriptTemplateViewResolver
 * @since 4.0
 */
public class ScriptTemplateView extends AbstractUrlBasedView {

  /**
   * The default content type for the view.
   */
  public static final String DEFAULT_CONTENT_TYPE = "text/html";

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:";

  private static final ThreadLocal<Map<Object, ScriptEngine>> enginesHolder =
          new NamedThreadLocal<>("ScriptTemplateView engines");

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
  private Charset charset;

  @Nullable
  private String[] resourceLoaderPaths;

  @Nullable
  private volatile ScriptEngineManager scriptEngineManager;

  /**
   * Constructor for use as a bean.
   *
   * @see #setUrl
   */
  public ScriptTemplateView() {
    setContentType(null);
  }

  /**
   * Create a new ScriptTemplateView with the given URL.
   */
  public ScriptTemplateView(String url) {
    super(url);
    setContentType(null);
  }

  /**
   * See {@link ScriptTemplateConfigurer#setEngine(ScriptEngine)} documentation.
   */
  public void setEngine(ScriptEngine engine) {
    this.engine = engine;
  }

  /**
   * See {@link ScriptTemplateConfigurer#setEngineSupplier(Supplier)} documentation.
   */
  public void setEngineSupplier(Supplier<ScriptEngine> engineSupplier) {
    this.engineSupplier = engineSupplier;
  }

  /**
   * See {@link ScriptTemplateConfigurer#setEngineName(String)} documentation.
   */
  public void setEngineName(String engineName) {
    this.engineName = engineName;
  }

  /**
   * See {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} documentation.
   */
  public void setSharedEngine(Boolean sharedEngine) {
    this.sharedEngine = sharedEngine;
  }

  /**
   * See {@link ScriptTemplateConfigurer#setScripts(String...)} documentation.
   */
  public void setScripts(String... scripts) {
    this.scripts = scripts;
  }

  /**
   * See {@link ScriptTemplateConfigurer#setRenderObject(String)} documentation.
   */
  public void setRenderObject(String renderObject) {
    this.renderObject = renderObject;
  }

  /**
   * See {@link ScriptTemplateConfigurer#setRenderFunction(String)} documentation.
   */
  public void setRenderFunction(String functionName) {
    this.renderFunction = functionName;
  }

  /**
   * See {@link ScriptTemplateConfigurer#setCharset(Charset)} documentation.
   */
  public void setCharset(Charset charset) {
    this.charset = charset;
  }

  /**
   * See {@link ScriptTemplateConfigurer#setResourceLoaderPath(String)} documentation.
   */
  public void setResourceLoaderPath(String resourceLoaderPath) {
    String[] paths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
    this.resourceLoaderPaths = new String[paths.length + 1];
    this.resourceLoaderPaths[0] = "";
    for (int i = 0; i < paths.length; i++) {
      String path = paths[i];
      if (!path.endsWith("/") && !path.endsWith(":")) {
        path = path + "/";
      }
      this.resourceLoaderPaths[i + 1] = path;
    }
  }

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext(context);

    ScriptTemplateConfig viewConfig = autodetectViewConfig();
    if (this.engine == null && viewConfig.getEngine() != null) {
      this.engine = viewConfig.getEngine();
    }
    if (this.engineSupplier == null && viewConfig.getEngineSupplier() != null) {
      this.engineSupplier = viewConfig.getEngineSupplier();
    }
    if (this.engineName == null && viewConfig.getEngineName() != null) {
      this.engineName = viewConfig.getEngineName();
    }
    if (this.scripts == null && viewConfig.getScripts() != null) {
      this.scripts = viewConfig.getScripts();
    }
    if (this.renderObject == null && viewConfig.getRenderObject() != null) {
      this.renderObject = viewConfig.getRenderObject();
    }
    if (this.renderFunction == null && viewConfig.getRenderFunction() != null) {
      this.renderFunction = viewConfig.getRenderFunction();
    }
    if (this.getContentType() == null) {
      setContentType(viewConfig.getContentType() != null ? viewConfig.getContentType() : DEFAULT_CONTENT_TYPE);
    }
    if (this.charset == null) {
      this.charset = viewConfig.getCharset() != null ? viewConfig.getCharset() : DEFAULT_CHARSET;
    }
    if (this.resourceLoaderPaths == null) {
      String resourceLoaderPath = viewConfig.getResourceLoaderPath();
      setResourceLoaderPath(resourceLoaderPath != null ? resourceLoaderPath : DEFAULT_RESOURCE_LOADER_PATH);
    }
    if (this.sharedEngine == null && viewConfig.isSharedEngine() != null) {
      this.sharedEngine = viewConfig.isSharedEngine();
    }

    int engineCount = 0;
    if (this.engine != null) {
      engineCount++;
    }
    if (this.engineSupplier != null) {
      engineCount++;
    }
    if (this.engineName != null) {
      engineCount++;
    }

    Assert.isTrue(engineCount == 1,
            "You should define either 'engine', 'engineSupplier' or 'engineName'.");
    if (Boolean.FALSE.equals(this.sharedEngine)) {
      Assert.isTrue(this.engine == null,
              "When 'sharedEngine' is set to false, you should specify the " +
                      "script engine using 'engineName' or 'engineSupplier' , not 'engine'.");
    }
    else if (this.engine != null) {
      loadScripts(this.engine);
    }
    else if (this.engineName != null) {
      setEngine(createEngineFromName(this.engineName));
    }
    else {
      setEngine(createEngineFromSupplier());
    }

    if (renderFunction != null && engine != null) {
      Assert.isInstanceOf(Invocable.class, engine,
              "ScriptEngine must implement Invocable when 'renderFunction' is specified");
    }
  }

  protected ScriptEngine getEngine() {
    if (Boolean.FALSE.equals(sharedEngine)) {
      Map<Object, ScriptEngine> engines = enginesHolder.get();
      if (engines == null) {
        engines = new HashMap<>(4);
        enginesHolder.set(engines);
      }

      String engineName = this.engineName;
      String name = engineName != null ? engineName : "";
      Object engineKey = ObjectUtils.isNotEmpty(scripts) ? new EngineKey(name, scripts) : name;
      ScriptEngine engine = engines.get(engineKey);
      if (engine == null) {
        if (engineName != null) {
          engine = createEngineFromName(engineName);
        }
        else {
          engine = createEngineFromSupplier();
        }
        engines.put(engineKey, engine);
      }
      return engine;
    }
    else {
      // Simply return the configured ScriptEngine...
      Assert.state(engine != null, "No shared engine available");
      return engine;
    }
  }

  protected ScriptEngine createEngineFromName(String engineName) {
    ScriptEngineManager scriptEngineManager = this.scriptEngineManager;
    if (scriptEngineManager == null) {
      scriptEngineManager = new ScriptEngineManager(obtainApplicationContext().getClassLoader());
      this.scriptEngineManager = scriptEngineManager;
    }

    ScriptEngine engine = StandardScriptUtils.retrieveEngineByName(scriptEngineManager, engineName);
    loadScripts(engine);
    return engine;
  }

  private ScriptEngine createEngineFromSupplier() {
    Assert.state(engineSupplier != null, "No engine supplier available");
    ScriptEngine engine = engineSupplier.get();
    if (renderFunction != null) {
      Assert.isInstanceOf(Invocable.class, engine,
              "ScriptEngine must implement Invocable when 'renderFunction' is specified");
    }
    loadScripts(engine);
    return engine;
  }

  protected void loadScripts(ScriptEngine engine) {
    if (ObjectUtils.isNotEmpty(scripts)) {
      for (String script : scripts) {
        Resource resource = getResource(script);
        if (resource == null) {
          throw new IllegalStateException("Script resource [" + script + "] not found");
        }
        try {
          engine.eval(new InputStreamReader(resource.getInputStream()));
        }
        catch (Throwable ex) {
          throw new IllegalStateException("Failed to evaluate script [" + script + "]", ex);
        }
      }
    }
  }

  @Nullable
  protected Resource getResource(String location) {
    if (resourceLoaderPaths != null) {
      ApplicationContext context = obtainApplicationContext();
      for (String path : resourceLoaderPaths) {
        Resource resource = context.getResource(path + location);
        if (resource.exists()) {
          return resource;
        }
      }
    }
    return null;
  }

  protected ScriptTemplateConfig autodetectViewConfig() throws BeansException {
    try {
      return BeanFactoryUtils.beanOfTypeIncludingAncestors(
              obtainApplicationContext(), ScriptTemplateConfig.class, true, false);
    }
    catch (NoSuchBeanDefinitionException ex) {
      throw new ApplicationContextException("Expected a single ScriptTemplateConfig bean in the current " +
              "Servlet web application context or the parent root context: ScriptTemplateConfigurer is " +
              "the usual implementation. This bean may have any name.", ex);
    }
  }

  @Override
  public boolean checkResource(Locale locale) {
    String url = getUrl();
    Assert.state(url != null, "'url' not set");
    return (getResource(url) != null);
  }

  @Override
  protected void prepareResponse(RequestContext context) {
    super.prepareResponse(context);
    setResponseContentType(context);
  }

  @Override
  protected void renderMergedOutputModel(
          Map<String, Object> model, RequestContext request) throws Exception {

    try {
      ScriptEngine engine = getEngine();
      String url = getUrl();
      Assert.state(url != null, "'url' not set");
      String template = getTemplate(url);

      Function<String, String> templateLoader = path -> {
        try {
          return getTemplate(path);
        }
        catch (IOException ex) {
          throw new IllegalStateException(ex);
        }
      };

      Locale locale = RequestContextUtils.getLocale(request);
      RenderingContext context = new RenderingContext(obtainApplicationContext(), locale, templateLoader, url);

      Object html;
      if (this.renderFunction == null) {
        SimpleBindings bindings = new SimpleBindings();
        bindings.putAll(model);
        model.put("renderingContext", context);
        html = engine.eval(template, bindings);
      }
      else if (this.renderObject != null) {
        Object thiz = engine.eval(this.renderObject);
        html = ((Invocable) engine).invokeMethod(thiz, this.renderFunction, template, model, context);
      }
      else {
        html = ((Invocable) engine).invokeFunction(this.renderFunction, template, model, context);
      }

      request.getWriter().write(String.valueOf(html));
    }
    catch (ScriptException ex) {
      throw new ServletException("Failed to render script template", new StandardScriptEvalException(ex));
    }
  }

  protected String getTemplate(String path) throws IOException {
    Resource resource = getResource(path);
    if (resource == null) {
      throw new IllegalStateException("Template resource [" + path + "] not found");
    }
    InputStreamReader reader = this.charset != null
                               ? new InputStreamReader(resource.getInputStream(), this.charset)
                               : new InputStreamReader(resource.getInputStream());
    return FileCopyUtils.copyToString(reader);
  }

  /**
   * Key class for the {@code enginesHolder ThreadLocal}.
   * Only used if scripts have been specified; otherwise, the
   * {@code engineName String} will be used as cache key directly.
   */
  private record EngineKey(String engineName, String[] scripts) {

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof EngineKey otherKey)) {
        return false;
      }
      return (this.engineName.equals(otherKey.engineName) && Arrays.equals(this.scripts, otherKey.scripts));
    }

    @Override
    public int hashCode() {
      return this.engineName.hashCode() * 29 + Arrays.hashCode(this.scripts);
    }
  }

}
