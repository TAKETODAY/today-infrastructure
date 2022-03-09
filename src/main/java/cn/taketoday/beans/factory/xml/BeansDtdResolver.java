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

package cn.taketoday.beans.factory.xml;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.FileNotFoundException;
import java.io.IOException;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * {@link EntityResolver} implementation for the Framework beans DTD,
 * to load the DTD from the Framework class path (or JAR file).
 *
 * <p>Fetches "spring-beans.dtd" from the class path resource
 * "/cn/taketoday/beans/factory/xml/spring-beans.dtd",
 * no matter whether specified as some local URL that includes "spring-beans"
 * in the DTD name or as "https://www.springframework.org/dtd/spring-beans-2.0.dtd".
 *
 * @author Juergen Hoeller
 * @author Colin Sampaleanu
 * @see ResourceEntityResolver
 * @since 04.06.2003
 */
public class BeansDtdResolver implements EntityResolver {

  private static final String DTD_EXTENSION = ".dtd";

  private static final String DTD_NAME = "spring-beans";

  private static final Logger logger = LoggerFactory.getLogger(BeansDtdResolver.class);

  @Override
  @Nullable
  public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId) throws IOException {
    if (logger.isTraceEnabled()) {
      logger.trace("Trying to resolve XML entity with public ID [" + publicId +
              "] and system ID [" + systemId + "]");
    }

    if (systemId != null && systemId.endsWith(DTD_EXTENSION)) {
      int lastPathSeparator = systemId.lastIndexOf('/');
      int dtdNameStart = systemId.indexOf(DTD_NAME, lastPathSeparator);
      if (dtdNameStart != -1) {
        String dtdFile = DTD_NAME + DTD_EXTENSION;
        if (logger.isTraceEnabled()) {
          logger.trace("Trying to locate [" + dtdFile + "] in Framework jar on classpath");
        }
        try {
          Resource resource = new ClassPathResource(dtdFile, getClass());
          InputSource source = new InputSource(resource.getInputStream());
          source.setPublicId(publicId);
          source.setSystemId(systemId);
          if (logger.isTraceEnabled()) {
            logger.trace("Found beans DTD [" + systemId + "] in classpath: " + dtdFile);
          }
          return source;
        }
        catch (FileNotFoundException ex) {
          if (logger.isDebugEnabled()) {
            logger.debug("Could not resolve beans DTD [" + systemId + "]: not found in classpath", ex);
          }
        }
      }
    }

    // Fall back to the parser's default behavior.
    return null;
  }

  @Override
  public String toString() {
    return "EntityResolver for spring-beans DTD";
  }

}
