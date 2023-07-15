/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jmx.export.annotation;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.jmx.export.MBeanExporter;
import cn.taketoday.jmx.export.assembler.MetadataMBeanInfoAssembler;
import cn.taketoday.jmx.export.naming.MetadataNamingStrategy;

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
