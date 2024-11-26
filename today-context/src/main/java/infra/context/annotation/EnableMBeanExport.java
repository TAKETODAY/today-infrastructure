/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.jmx.export.MBeanExporter;
import infra.jmx.export.annotation.AnnotationMBeanExporter;
import infra.jmx.support.RegistrationPolicy;

/**
 * Enables default exporting of all standard {@code MBean}s from the Framework context, as
 * well as well all {@code @ManagedResource} annotated beans.
 *
 * <p>The resulting {@link MBeanExporter MBeanExporter}
 * bean is defined under the name "mbeanExporter". Alternatively, consider defining a
 * custom {@link AnnotationMBeanExporter} bean explicitly.
 *
 * <p>This annotation is modeled after and functionally equivalent to Framework XML's
 * {@code <context:mbean-export/>} element.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MBeanExportConfiguration
 * @since 4.0 2022/3/7 23:06
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MBeanExportConfiguration.class)
public @interface EnableMBeanExport {

  /**
   * The default domain to use when generating JMX ObjectNames.
   */
  String defaultDomain() default "";

  /**
   * The bean name of the MBeanServer to which MBeans should be exported. Default is to
   * use the platform's default MBeanServer.
   */
  String server() default "";

  /**
   * The policy to use when attempting to register an MBean under an
   * {@link javax.management.ObjectName} that already exists. Defaults to
   * {@link RegistrationPolicy#FAIL_ON_EXISTING}.
   */
  RegistrationPolicy registration() default RegistrationPolicy.FAIL_ON_EXISTING;
}
