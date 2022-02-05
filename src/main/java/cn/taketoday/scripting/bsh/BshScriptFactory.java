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

import bsh.EvalError;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scripting.ScriptCompilationException;
import cn.taketoday.scripting.ScriptFactory;
import cn.taketoday.scripting.ScriptSource;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * {@link ScriptFactory} implementation
 * for a BeanShell script.
 *
 * <p>Typically used in combination with a
 * {@link cn.taketoday.scripting.support.ScriptFactoryPostProcessor};
 * see the latter's javadoc for a configuration example.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see BshScriptUtils
 * @see cn.taketoday.scripting.support.ScriptFactoryPostProcessor
 * @since 4.0
 */
public class BshScriptFactory implements ScriptFactory, BeanClassLoaderAware {

  private final String scriptSourceLocator;

  @Nullable
  private final Class<?>[] scriptInterfaces;

  @Nullable
  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  @Nullable
  private Class<?> scriptClass;

  private final Object scriptClassMonitor = new Object();

  private boolean wasModifiedForTypeCheck = false;

  /**
   * Create a new BshScriptFactory for the given script source.
   * <p>With this {@code BshScriptFactory} variant, the script needs to
   * declare a full class or return an actual instance of the scripted object.
   *
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   */
  public BshScriptFactory(String scriptSourceLocator) {
    Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
    this.scriptSourceLocator = scriptSourceLocator;
    this.scriptInterfaces = null;
  }

  /**
   * Create a new BshScriptFactory for the given script source.
   * <p>The script may either be a simple script that needs a corresponding proxy
   * generated (implementing the specified interfaces), or declare a full class
   * or return an actual instance of the scripted object (in which case the
   * specified interfaces, if any, need to be implemented by that class/instance).
   *
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   * @param scriptInterfaces the Java interfaces that the scripted object
   * is supposed to implement (may be {@code null})
   */
  public BshScriptFactory(String scriptSourceLocator, @Nullable Class<?>... scriptInterfaces) {
    Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
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

  /**
   * BeanShell scripts do require a config interface.
   */
  @Override
  public boolean requiresConfigInterface() {
    return true;
  }

  /**
   * Load and parse the BeanShell script via {@link BshScriptUtils}.
   *
   * @see BshScriptUtils#createBshObject(String, Class[], ClassLoader)
   */
  @Override
  @Nullable
  public Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
          throws IOException, ScriptCompilationException {

    Class<?> clazz;

    try {
      synchronized(this.scriptClassMonitor) {
        boolean requiresScriptEvaluation = (this.wasModifiedForTypeCheck && this.scriptClass == null);
        this.wasModifiedForTypeCheck = false;

        if (scriptSource.isModified() || requiresScriptEvaluation) {
          // New script content: Let's check whether it evaluates to a Class.
          Object result = BshScriptUtils.evaluateBshScript(
                  scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
          if (result instanceof Class) {
            // A Class: We'll cache the Class here and create an instance
            // outside of the synchronized block.
            this.scriptClass = (Class<?>) result;
          }
          else {
            // Not a Class: OK, we'll simply create BeanShell objects
            // through evaluating the script for every call later on.
            // For this first-time check, let's simply return the
            // already evaluated object.
            return result;
          }
        }
        clazz = this.scriptClass;
      }
    }
    catch (EvalError ex) {
      this.scriptClass = null;
      throw new ScriptCompilationException(scriptSource, ex);
    }

    if (clazz != null) {
      // A Class: We need to create an instance for every call.
      try {
        return ReflectionUtils.accessibleConstructor(clazz).newInstance();
      }
      catch (Throwable ex) {
        throw new ScriptCompilationException(
                scriptSource, "Could not instantiate script class: " + clazz.getName(), ex);
      }
    }
    else {
      // Not a Class: We need to evaluate the script for every call.
      try {
        return BshScriptUtils.createBshObject(
                scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
      }
      catch (EvalError ex) {
        throw new ScriptCompilationException(scriptSource, ex);
      }
    }
  }

  @Override
  @Nullable
  public Class<?> getScriptedObjectType(ScriptSource scriptSource)
          throws IOException, ScriptCompilationException {

    synchronized(this.scriptClassMonitor) {
      try {
        if (scriptSource.isModified()) {
          // New script content: Let's check whether it evaluates to a Class.
          this.wasModifiedForTypeCheck = true;
          this.scriptClass = BshScriptUtils.determineBshObjectType(
                  scriptSource.getScriptAsString(), this.beanClassLoader);
        }
        return this.scriptClass;
      }
      catch (EvalError ex) {
        this.scriptClass = null;
        throw new ScriptCompilationException(scriptSource, ex);
      }
    }
  }

  @Override
  public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
    synchronized(this.scriptClassMonitor) {
      return (scriptSource.isModified() || this.wasModifiedForTypeCheck);
    }
  }

  @Override
  public String toString() {
    return "BshScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
  }

}
