/*
 * Copyright 2002-2018 the original author or authors.
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

package cn.taketoday.web.context.support;

import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.servlet.ServletContextAware;
import jakarta.servlet.ServletContext;

/**
 * Exporter that takes Frameworkdefined objects and exposes them as
 * ServletContext attributes. Usually, bean references will be used
 * to export Framework-defined beans as ServletContext attributes.
 *
 * <p>Useful to make Frameworkdefined beans available to code that is
 * not aware of Framework at all, but rather just of the Servlet API.
 * Client code can then use plain ServletContext attribute lookups
 * to access those objects, despite them being defined in a Spring
 * application context.
 *
 * <p>Alternatively, consider using the WebApplicationContextUtils
 * class to access Framework-defined beans via the WebServletApplicationContext
 * interface. This makes client code aware of Framework API, of course.
 *
 * @author Juergen Hoeller
 * @see ServletContext#getAttribute
 * @see WebApplicationContextUtils#getWebApplicationContext
 * @since 4.0
 */
public class ServletContextAttributeExporter implements ServletContextAware {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Nullable
  private Map<String, Object> attributes;

  /**
   * Set the ServletContext attributes to expose as key-value pairs.
   * Each key will be considered a ServletContext attributes key,
   * and each value will be used as corresponding attribute value.
   * <p>Usually, you will use bean references for the values,
   * to export Frameworkdefined beans as ServletContext attributes.
   * Of course, it is also possible to define plain values to export.
   */
  public void setAttributes(@Nullable Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    if (this.attributes != null) {
      for (Map.Entry<String, Object> entry : this.attributes.entrySet()) {
        String attributeName = entry.getKey();
        if (logger.isDebugEnabled()) {
          if (servletContext.getAttribute(attributeName) != null) {
            logger.debug("Replacing existing ServletContext attribute with name '{}'", attributeName);
          }
        }
        servletContext.setAttribute(attributeName, entry.getValue());
        if (logger.isTraceEnabled()) {
          logger.trace("Exported ServletContext attribute with name '{}'", attributeName);
        }
      }
    }
  }

}
