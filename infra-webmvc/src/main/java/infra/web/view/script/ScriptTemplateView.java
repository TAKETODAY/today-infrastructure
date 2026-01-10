/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.view.script;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
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

import infra.beans.BeansException;
import infra.beans.factory.BeanFactoryUtils;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.context.ApplicationContext;
import infra.context.ApplicationContextException;
import infra.core.NamedThreadLocal;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.scripting.support.StandardScriptEvalException;
import infra.scripting.support.StandardScriptUtils;
import infra.util.FileCopyUtils;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.RequestContextUtils;
import infra.web.view.AbstractUrlBasedView;
import infra.web.view.ViewRenderingException;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ScriptTemplateConfigurer
 * @see ScriptTemplateViewResolver
 * @since 4.0
 */
public class ScriptTemplateView extends AbstractUrlBasedView {

  /**
   * The default content type for the view.
   */
  public static final String DEFAULT_CONTENT_TYPE = "text/html";

  private static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:";

  private static final NamedThreadLocal<Map<Object, ScriptEngine>> enginesHolder =
          new NamedThreadLocal<>("ScriptTemplateView engines");

  @Nullable
  private ScriptEngine engine;

  @Nullable
  private Supplier<ScriptEngine> engineSupplier;

  @Nullable
  private String engineName;

  @Nullable
  private Boolean sharedEngine;

  private String @Nullable [] scripts;

  @Nullable
  private String renderObject;

  @Nullable
  private String renderFunction;

  @Nullable
  private Charset charset;

  private String @Nullable [] resourceLoaderPaths;

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
      this.charset = viewConfig.getCharset() != null ? viewConfig.getCharset() : Constant.DEFAULT_CHARSET;
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
              "Web application context or the parent root context: ScriptTemplateConfigurer is " +
              "the usual implementation. This bean may have any name.", ex);
    }
  }

  @Override
  public boolean checkResource(Locale locale) {
    String url = getUrl();
    Assert.state(url != null, "'url' not set");
    return getResource(url) != null;
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
      throw new ViewRenderingException("Failed to render script template", new StandardScriptEvalException(ex));
    }
  }

  protected String getTemplate(String path) throws IOException {
    Resource resource = getResource(path);
    if (resource == null) {
      throw new IllegalStateException("Template resource [" + path + "] not found");
    }
    var reader = charset != null
            ? new InputStreamReader(resource.getInputStream(), charset)
            : new InputStreamReader(resource.getInputStream());
    return FileCopyUtils.copyToString(reader);
  }

  /**
   * Key class for the {@code enginesHolder ThreadLocal}.
   * Only used if scripts have been specified; otherwise, the
   * {@code engineName String} will be used as cache key directly.
   */
  record EngineKey(String engineName, String[] scripts) {

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
