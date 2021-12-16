/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.weaving;

import org.aspectj.weaver.loadtime.ClassPreProcessorAgentAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactoryPostProcessor;
import cn.taketoday.beans.factory.BeansException;
import cn.taketoday.beans.factory.ConfigurableBeanFactory;
import cn.taketoday.context.classloading.InstrumentationLoadTimeWeaver;
import cn.taketoday.context.classloading.LoadTimeWeaver;
import cn.taketoday.core.Ordered;
import cn.taketoday.lang.Nullable;

/**
 * Post-processor that registers AspectJ's
 * {@link ClassPreProcessorAgentAdapter}
 * with the Spring application context's default
 * {@link cn.taketoday.context.classloading.LoadTimeWeaver}.
 *
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 4.0
 */
public class AspectJWeavingEnabler
        implements BeanFactoryPostProcessor, BeanClassLoaderAware, LoadTimeWeaverAware, Ordered {

  /**
   * The {@code aop.xml} resource location.
   */
  public static final String ASPECTJ_AOP_XML_RESOURCE = "META-INF/aop.xml";

  @Nullable
  private ClassLoader beanClassLoader;

  @Nullable
  private LoadTimeWeaver loadTimeWeaver;

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  @Override
  public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
    this.loadTimeWeaver = loadTimeWeaver;
  }

  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) throws BeansException {
    enableAspectJWeaving(this.loadTimeWeaver, this.beanClassLoader);
  }

  /**
   * Enable AspectJ weaving with the given {@link LoadTimeWeaver}.
   *
   * @param weaverToUse the LoadTimeWeaver to apply to (or {@code null} for a default weaver)
   * @param beanClassLoader the class loader to create a default weaver for (if necessary)
   */
  public static void enableAspectJWeaving(
          @Nullable LoadTimeWeaver weaverToUse, @Nullable ClassLoader beanClassLoader) {

    if (weaverToUse == null) {
      if (InstrumentationLoadTimeWeaver.isInstrumentationAvailable()) {
        weaverToUse = new InstrumentationLoadTimeWeaver(beanClassLoader);
      }
      else {
        throw new IllegalStateException("No LoadTimeWeaver available");
      }
    }
    weaverToUse.addTransformer(
            new AspectJClassBypassingClassFileTransformer(new ClassPreProcessorAgentAdapter()));
  }

  /**
   * ClassFileTransformer decorator that suppresses processing of AspectJ
   * classes in order to avoid potential LinkageErrors.
   *
   * @see cn.taketoday.context.annotation.LoadTimeWeavingConfiguration
   */
  private record AspectJClassBypassingClassFileTransformer(ClassFileTransformer delegate)
          implements ClassFileTransformer {

    @Override
    public byte[] transform(
            ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

      if (className.startsWith("org.aspectj") || className.startsWith("org/aspectj")) {
        return classfileBuffer;
      }
      return this.delegate.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }
  }

}
