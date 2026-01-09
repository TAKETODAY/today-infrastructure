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

package infra.jmx.export.annotation;

import infra.beans.factory.BeanFactory;
import infra.jmx.export.MBeanExporter;
import infra.jmx.export.assembler.MetadataMBeanInfoAssembler;
import infra.jmx.export.naming.MetadataNamingStrategy;

/**
 * Convenient subclass of Framework's standard {@link MBeanExporter},
 * activating Java 5 annotation usage for JMX exposure of Framework beans:
 * {@link ManagedResource}, {@link ManagedAttribute}, {@link ManagedOperation}, etc.
 *
 * <p>Sets a {@link MetadataNamingStrategy} and a {@link MetadataMBeanInfoAssembler}
 * with an {@link AnnotationJmxAttributeSource}, and activates the
 * {@link #AUTODETECT_ALL} mode by default.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public class AnnotationMBeanExporter extends MBeanExporter {

  private final AnnotationJmxAttributeSource annotationSource =
          new AnnotationJmxAttributeSource();

  private final MetadataNamingStrategy metadataNamingStrategy =
          new MetadataNamingStrategy(this.annotationSource);

  public AnnotationMBeanExporter() {
    setAutodetectMode(AUTODETECT_ALL);
    setNamingStrategy(this.metadataNamingStrategy);
    setAssembler(new MetadataMBeanInfoAssembler(annotationSource));
    setAutodetect(true);
  }

  /**
   * Specify the default domain to be used for generating ObjectNames
   * when no source-level metadata has been specified.
   * <p>The default is to use the domain specified in the bean name
   * (if the bean name follows the JMX ObjectName syntax); else,
   * the package name of the managed bean class.
   *
   * @see MetadataNamingStrategy#setDefaultDomain
   */
  public void setDefaultDomain(String defaultDomain) {
    this.metadataNamingStrategy.setDefaultDomain(defaultDomain);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    super.setBeanFactory(beanFactory);
    this.annotationSource.setBeanFactory(beanFactory);
  }

}
