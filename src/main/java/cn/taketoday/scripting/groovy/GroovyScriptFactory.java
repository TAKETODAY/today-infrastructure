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
package cn.taketoday.scripting.groovy;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.scripting.ScriptCompilationException;
import cn.taketoday.scripting.ScriptFactory;
import cn.taketoday.scripting.ScriptSource;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.Script;

/**
 * {@link cn.taketoday.scripting.ScriptFactory} implementation
 * for a Groovy script.
 *
 * <p>Typically used in combination with a
 * {@link cn.taketoday.scripting.support.ScriptFactoryPostProcessor};
 * see the latter's javadoc for a configuration example.
 *
 * <p>supports Groovy 1.8 and higher.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rod Johnson
 * @see groovy.lang.GroovyClassLoader
 * @see cn.taketoday.scripting.support.ScriptFactoryPostProcessor
 * @since 4.0
 */
public class GroovyScriptFactory implements ScriptFactory, BeanFactoryAware, BeanClassLoaderAware {

  private final String scriptSourceLocator;

  @Nullable
  private GroovyObjectCustomizer groovyObjectCustomizer;

  @Nullable
  private CompilerConfiguration compilerConfiguration;

  @Nullable
  private GroovyClassLoader groovyClassLoader;

  @Nullable
  private Class<?> scriptClass;

  @Nullable
  private Class<?> scriptResultClass;

  @Nullable
  private CachedResultHolder cachedResult;

  private final Object scriptClassMonitor = new Object();

  private boolean wasModifiedForTypeCheck = false;

  /**
   * Create a new GroovyScriptFactory for the given script source.
   * <p>We don't need to specify script interfaces here, since
   * a Groovy script defines its Java interfaces itself.
   *
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   */
  public GroovyScriptFactory(String scriptSourceLocator) {
    Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
    this.scriptSourceLocator = scriptSourceLocator;
  }

  /**
   * Create a new GroovyScriptFactory for the given script source,
   * specifying a strategy interface that can create a custom MetaClass
   * to supply missing methods and otherwise change the behavior of the object.
   *
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   * @param groovyObjectCustomizer a customizer that can set a custom metaclass
   * or make other changes to the GroovyObject created by this factory
   * (may be {@code null})
   * @see GroovyObjectCustomizer#customize
   */
  public GroovyScriptFactory(String scriptSourceLocator, @Nullable GroovyObjectCustomizer groovyObjectCustomizer) {
    this(scriptSourceLocator);
    this.groovyObjectCustomizer = groovyObjectCustomizer;
  }

  /**
   * Create a new GroovyScriptFactory for the given script source,
   * specifying a strategy interface that can create a custom MetaClass
   * to supply missing methods and otherwise change the behavior of the object.
   *
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   * @param compilerConfiguration a custom compiler configuration to be applied
   * to the GroovyClassLoader (may be {@code null})
   * @see GroovyClassLoader#GroovyClassLoader(ClassLoader, CompilerConfiguration)
   */
  public GroovyScriptFactory(String scriptSourceLocator, @Nullable CompilerConfiguration compilerConfiguration) {
    this(scriptSourceLocator);
    this.compilerConfiguration = compilerConfiguration;
  }

  /**
   * Create a new GroovyScriptFactory for the given script source,
   * specifying a strategy interface that can customize Groovy's compilation
   * process within the underlying GroovyClassLoader.
   *
   * @param scriptSourceLocator a locator that points to the source of the script.
   * Interpreted by the post-processor that actually creates the script.
   * @param compilationCustomizers one or more customizers to be applied to the
   * GroovyClassLoader compiler configuration
   * @see CompilerConfiguration#addCompilationCustomizers
   * @see org.codehaus.groovy.control.customizers.ImportCustomizer
   */
  public GroovyScriptFactory(String scriptSourceLocator, CompilationCustomizer... compilationCustomizers) {
    this(scriptSourceLocator);
    if (ObjectUtils.isNotEmpty(compilationCustomizers)) {
      this.compilerConfiguration = new CompilerConfiguration();
      this.compilerConfiguration.addCompilationCustomizers(compilationCustomizers);
    }
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (beanFactory instanceof ConfigurableBeanFactory) {
      ((ConfigurableBeanFactory) beanFactory).ignoreDependencyType(MetaClass.class);
    }
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    if (classLoader instanceof GroovyClassLoader &&
            (this.compilerConfiguration == null ||
                    ((GroovyClassLoader) classLoader).hasCompatibleConfiguration(this.compilerConfiguration))) {
      this.groovyClassLoader = (GroovyClassLoader) classLoader;
    }
    else {
      this.groovyClassLoader = buildGroovyClassLoader(classLoader);
    }
  }

  /**
   * Return the GroovyClassLoader used by this script factory.
   */
  public GroovyClassLoader getGroovyClassLoader() {
    synchronized(this.scriptClassMonitor) {
      if (this.groovyClassLoader == null) {
        this.groovyClassLoader = buildGroovyClassLoader(ClassUtils.getDefaultClassLoader());
      }
      return this.groovyClassLoader;
    }
  }

  /**
   * Build a {@link GroovyClassLoader} for the given {@code ClassLoader}.
   *
   * @param classLoader the ClassLoader to build a GroovyClassLoader for
   */
  protected GroovyClassLoader buildGroovyClassLoader(@Nullable ClassLoader classLoader) {
    return compilerConfiguration != null ?
           new GroovyClassLoader(classLoader, this.compilerConfiguration) : new GroovyClassLoader(classLoader);
  }

  @Override
  public String getScriptSourceLocator() {
    return this.scriptSourceLocator;
  }

  /**
   * Groovy scripts determine their interfaces themselves,
   * hence we don't need to explicitly expose interfaces here.
   *
   * @return {@code null} always
   */
  @Override
  @Nullable
  public Class<?>[] getScriptInterfaces() {
    return null;
  }

  /**
   * Groovy scripts do not need a config interface,
   * since they expose their setters as public methods.
   */
  @Override
  public boolean requiresConfigInterface() {
    return false;
  }

  /**
   * Loads and parses the Groovy script via the GroovyClassLoader.
   *
   * @see groovy.lang.GroovyClassLoader
   */
  @Override
  @Nullable
  public Object getScriptedObject(ScriptSource scriptSource, @Nullable Class<?>... actualInterfaces)
          throws IOException, ScriptCompilationException {

    synchronized(this.scriptClassMonitor) {
      try {
        Class<?> scriptClassToExecute;
        this.wasModifiedForTypeCheck = false;

        if (this.cachedResult != null) {
          Object result = this.cachedResult.object;
          this.cachedResult = null;
          return result;
        }

        if (this.scriptClass == null || scriptSource.isModified()) {
          // New script content...
          this.scriptClass = getGroovyClassLoader().parseClass(
                  scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

          if (Script.class.isAssignableFrom(this.scriptClass)) {
            // A Groovy script, probably creating an instance: let's execute it.
            Object result = executeScript(scriptSource, this.scriptClass);
            this.scriptResultClass = (result != null ? result.getClass() : null);
            return result;
          }
          else {
            this.scriptResultClass = this.scriptClass;
          }
        }
        scriptClassToExecute = this.scriptClass;

        // Process re-execution outside of the synchronized block.
        return executeScript(scriptSource, scriptClassToExecute);
      }
      catch (CompilationFailedException ex) {
        this.scriptClass = null;
        this.scriptResultClass = null;
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
        if (this.scriptClass == null || scriptSource.isModified()) {
          // New script content...
          this.wasModifiedForTypeCheck = true;
          this.scriptClass = getGroovyClassLoader().parseClass(
                  scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

          if (Script.class.isAssignableFrom(this.scriptClass)) {
            // A Groovy script, probably creating an instance: let's execute it.
            Object result = executeScript(scriptSource, this.scriptClass);
            this.scriptResultClass = (result != null ? result.getClass() : null);
            this.cachedResult = new CachedResultHolder(result);
          }
          else {
            this.scriptResultClass = this.scriptClass;
          }
        }
        return this.scriptResultClass;
      }
      catch (CompilationFailedException ex) {
        this.scriptClass = null;
        this.scriptResultClass = null;
        this.cachedResult = null;
        throw new ScriptCompilationException(scriptSource, ex);
      }
    }
  }

  @Override
  public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
    synchronized(this.scriptClassMonitor) {
      return scriptSource.isModified() || this.wasModifiedForTypeCheck;
    }
  }

  /**
   * Instantiate the given Groovy script class and run it if necessary.
   *
   * @param scriptSource the source for the underlying script
   * @param scriptClass the Groovy script class
   * @return the result object (either an instance of the script class
   * or the result of running the script instance)
   * @throws ScriptCompilationException in case of instantiation failure
   */
  @Nullable
  protected Object executeScript(ScriptSource scriptSource, Class<?> scriptClass) throws ScriptCompilationException {
    try {
      GroovyObject goo = (GroovyObject) ReflectionUtils.accessibleConstructor(scriptClass).newInstance();

      if (groovyObjectCustomizer != null) {
        // Allow metaclass and other customization.
        groovyObjectCustomizer.customize(goo);
      }

      if (goo instanceof Script) {
        // A Groovy script, probably creating an instance: let's execute it.
        return ((Script) goo).run();
      }
      else {
        // An instance of the scripted class: let's return it as-is.
        return goo;
      }
    }
    catch (ConstructorNotFoundException ex) {
      throw new ScriptCompilationException(
              "No default constructor on Groovy script class: " + scriptClass.getName(), ex);
    }
    catch (InstantiationException ex) {
      throw new ScriptCompilationException(
              scriptSource, "Unable to instantiate Groovy script class: " + scriptClass.getName(), ex);
    }
    catch (IllegalAccessException ex) {
      throw new ScriptCompilationException(
              scriptSource, "Could not access Groovy script constructor: " + scriptClass.getName(), ex);
    }
    catch (InvocationTargetException ex) {
      throw new ScriptCompilationException(
              "Failed to invoke Groovy script constructor: " + scriptClass.getName(), ex.getTargetException());
    }
  }

  @Override
  public String toString() {
    return "GroovyScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
  }

  /**
   * Wrapper that holds a temporarily cached result object.
   */
  private record CachedResultHolder(@Nullable Object object) {

    private CachedResultHolder(@Nullable Object object) {
      this.object = object;
    }
  }

}
