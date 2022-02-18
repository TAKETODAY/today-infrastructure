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
package cn.taketoday.scripting.support;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scripting.ScriptCompilationException;
import cn.taketoday.scripting.ScriptFactory;
import cn.taketoday.scripting.ScriptSource;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link cn.taketoday.scripting.ScriptFactory} implementation based
 * on the JSR-223 script engine abstraction (as included in Java 6+).
 * Supports JavaScript, Groovy, JRuby, and other JSR-223 compliant engines.
 *
 * <p>Typically used in combination with a
 * {@link cn.taketoday.scripting.support.ScriptFactoryPostProcessor};
 * see the latter's javadoc for a configuration example.
 *
 * @author Juergen Hoeller
 * @see ScriptFactoryPostProcessor
 * @since 4.0
 */
public class StandardScriptFactory implements ScriptFactory, BeanClassLoaderAware {

  @Nullable
  private final String scriptEngineName;

  private final String scriptSourceLocator;

  @Nullable
  private final Class<?>[] scriptInterfaces;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private volatile ScriptEngine scriptEngine;

  /**
   * Create a new StandardScriptFactory for the given script source.
   *
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   */
  public StandardScriptFactory(String scriptSourceLocator) {
    this(null, scriptSourceLocator, (Class<?>[]) null);
  }

  /**
   * Create a new StandardScriptFactory for the given script source.
   *
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   * @param scriptInterfaces the Java interfaces that the scripted object
   * is supposed to implement
   */
  public StandardScriptFactory(String scriptSourceLocator, Class<?>... scriptInterfaces) {
    this(null, scriptSourceLocator, scriptInterfaces);
  }

  /**
   * Create a new StandardScriptFactory for the given script source.
   *
   * @param scriptEngineName the name of the JSR-223 ScriptEngine to use
   * (explicitly given instead of inferred from the script source)
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   */
  public StandardScriptFactory(String scriptEngineName, String scriptSourceLocator) {
    this(scriptEngineName, scriptSourceLocator, (Class<?>[]) null);
  }

  /**
   * Create a new StandardScriptFactory for the given script source.
   *
   * @param scriptEngineName the name of the JSR-223 ScriptEngine to use
   * (explicitly given instead of inferred from the script source)
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   * @param scriptInterfaces the Java interfaces that the scripted object
   * is supposed to implement
   */
  public StandardScriptFactory(
          @Nullable String scriptEngineName, String scriptSourceLocator, @Nullable Class<?>... scriptInterfaces) {
    Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
    this.scriptEngineName = scriptEngineName;
    this.scriptSourceLocator = scriptSourceLocator;
    this.scriptInterfaces = scriptInterfaces;
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public String getScriptSourceLocator() {
    return this.scriptSourceLocator;
  }

  @Override
  @Nullable
  public Class<?>[] getScriptInterfaces() {
    return this.scriptInterfaces;
  }

  @Override
  public boolean requiresConfigInterface() {
    return false;
  }

  /**
   * Load and parse the script via JSR-223's ScriptEngine.
   */
  @Override
  @Nullable
  public Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
          throws IOException, ScriptCompilationException {

    Object script = evaluateScript(scriptSource);

    if (ObjectUtils.isNotEmpty(actualInterfaces)) {
      boolean adaptationRequired = false;
      for (Class<?> requestedIfc : actualInterfaces) {
        if (script instanceof Class ? !requestedIfc.isAssignableFrom((Class<?>) script) :
            !requestedIfc.isInstance(script)) {
          adaptationRequired = true;
          break;
        }
      }
      if (adaptationRequired) {
        script = adaptToInterfaces(script, scriptSource, actualInterfaces);
      }
    }

    if (script instanceof Class<?> scriptClass) {
      try {
        return ReflectionUtils.accessibleConstructor(scriptClass).newInstance();
      }
      catch (ConstructorNotFoundException ex) {
        throw new ScriptCompilationException(
                "No default constructor on script class: " + scriptClass.getName(), ex);
      }
      catch (InstantiationException ex) {
        throw new ScriptCompilationException(
                scriptSource, "Unable to instantiate script class: " + scriptClass.getName(), ex);
      }
      catch (IllegalAccessException ex) {
        throw new ScriptCompilationException(
                scriptSource, "Could not access script constructor: " + scriptClass.getName(), ex);
      }
      catch (InvocationTargetException ex) {
        throw new ScriptCompilationException(
                "Failed to invoke script constructor: " + scriptClass.getName(), ex.getTargetException());
      }
    }

    return script;
  }

  protected Object evaluateScript(ScriptSource scriptSource) {
    try {
      ScriptEngine scriptEngine = this.scriptEngine;
      if (scriptEngine == null) {
        scriptEngine = retrieveScriptEngine(scriptSource);
        if (scriptEngine == null) {
          throw new IllegalStateException("Could not determine script engine for " + scriptSource);
        }
        this.scriptEngine = scriptEngine;
      }
      return scriptEngine.eval(scriptSource.getScriptAsString());
    }
    catch (Exception ex) {
      throw new ScriptCompilationException(scriptSource, ex);
    }
  }

  @Nullable
  protected ScriptEngine retrieveScriptEngine(ScriptSource scriptSource) {
    ScriptEngineManager scriptEngineManager = new ScriptEngineManager(this.beanClassLoader);

    if (this.scriptEngineName != null) {
      return StandardScriptUtils.retrieveEngineByName(scriptEngineManager, this.scriptEngineName);
    }

    if (scriptSource instanceof ResourceScriptSource) {
      String filename = ((ResourceScriptSource) scriptSource).getResource().getName();
      if (filename != null) {
        String extension = StringUtils.getFilenameExtension(filename);
        if (extension != null) {
          return scriptEngineManager.getEngineByExtension(extension);
        }
      }
    }

    return null;
  }

  @Nullable
  protected Object adaptToInterfaces(
          @Nullable Object script, ScriptSource scriptSource, Class<?>... actualInterfaces) {

    Class<?> adaptedIfc;
    if (actualInterfaces.length == 1) {
      adaptedIfc = actualInterfaces[0];
    }
    else {
      adaptedIfc = ClassUtils.createCompositeInterface(actualInterfaces, this.beanClassLoader);
    }

    if (adaptedIfc != null) {
      ScriptEngine scriptEngine = this.scriptEngine;
      if (!(scriptEngine instanceof Invocable invocable)) {
        throw new ScriptCompilationException(scriptSource,
                "ScriptEngine must implement Invocable in order to adapt it to an interface: " + scriptEngine);
      }
      if (script != null) {
        script = invocable.getInterface(script, adaptedIfc);
      }
      if (script == null) {
        script = invocable.getInterface(adaptedIfc);
        if (script == null) {
          throw new ScriptCompilationException(scriptSource,
                  "Could not adapt script to interface [" + adaptedIfc.getName() + "]");
        }
      }
    }

    return script;
  }

  @Override
  @Nullable
  public Class<?> getScriptedObjectType(ScriptSource scriptSource)
          throws IOException, ScriptCompilationException {

    return null;
  }

  @Override
  public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
    return scriptSource.isModified();
  }

  @Override
  public String toString() {
    return "StandardScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
  }

}
