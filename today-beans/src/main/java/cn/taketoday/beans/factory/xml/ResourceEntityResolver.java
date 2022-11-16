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

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ResourceUtils;

/**
 * {@code EntityResolver} implementation that tries to resolve entity references
 * through a {@link cn.taketoday.core.io.ResourceLoader} (usually,
 * relative to the resource base of an {@code ApplicationContext}), if applicable.
 * Extends {@link DelegatingEntityResolver} to also provide DTD and XSD lookup.
 *
 * <p>Allows to use standard XML entities to include XML snippets into an
 * application context definition, for example to split a large XML file
 * into various modules. The include paths can be relative to the
 * application context's resource base as usual, instead of relative
 * to the JVM working directory (the XML parser's default).
 *
 * <p>Note: In addition to relative paths, every URL that specifies a
 * file in the current system root, i.e. the JVM working directory,
 * will be interpreted relative to the application context too.
 *
 * @author Juergen Hoeller
 * @see cn.taketoday.core.io.ResourceLoader
 * @see cn.taketoday.context.ApplicationContext
 * @since 4.0
 */
public class ResourceEntityResolver extends DelegatingEntityResolver {

  private static final Logger log = LoggerFactory.getLogger(ResourceEntityResolver.class);

  private final ResourceLoader resourceLoader;

  /**
   * Create a ResourceEntityResolver for the specified ResourceLoader
   * (usually, an ApplicationContext).
   *
   * @param resourceLoader the ResourceLoader (or ApplicationContext)
   * to load XML entity includes with
   */
  public ResourceEntityResolver(ResourceLoader resourceLoader) {
    super(resourceLoader.getClassLoader());
    this.resourceLoader = resourceLoader;
  }

  @Override
  @Nullable
  public InputSource resolveEntity(@Nullable String publicId, @Nullable String systemId)
          throws SAXException, IOException {

    InputSource source = super.resolveEntity(publicId, systemId);

    if (source == null && systemId != null) {
      String resourcePath = null;
      try {
        String decodedSystemId = URLDecoder.decode(systemId, StandardCharsets.UTF_8);
        String givenUrl = ResourceUtils.toURL(decodedSystemId).toString();
        String systemRootUrl = new File("").toURI().toURL().toString();
        // Try relative to resource base if currently in system root.
        if (givenUrl.startsWith(systemRootUrl)) {
          resourcePath = givenUrl.substring(systemRootUrl.length());
        }
      }
      catch (Exception ex) {
        // Typically a MalformedURLException or AccessControlException.
        log.debug("Could not resolve XML entity [{}] against system root URL", systemId, ex);
        // No URL (or no resolvable URL) -> try relative to resource base.
        resourcePath = systemId;
      }
      if (resourcePath != null) {
        if (log.isTraceEnabled()) {
          log.trace("Trying to locate XML entity [{}] as resource [{}]", systemId, resourcePath);
        }
        Resource resource = this.resourceLoader.getResource(resourcePath);
        source = new InputSource(resource.getInputStream());
        source.setPublicId(publicId);
        source.setSystemId(systemId);
        if (log.isDebugEnabled()) {
          log.debug("Found XML entity [{}]: {}", systemId, resource);
        }
      }
      else if (systemId.endsWith(DTD_SUFFIX) || systemId.endsWith(XSD_SUFFIX)) {
        // External dtd/xsd lookup via https even for canonical http declaration
        String url = systemId;
        if (url.startsWith("http:")) {
          url = "https:" + url.substring(5);
        }
        try {
          source = new InputSource(new URL(url).openStream());
          source.setPublicId(publicId);
          source.setSystemId(systemId);
        }
        catch (IOException ex) {
          log.debug("Could not resolve XML entity [{}] through URL [{}]", systemId, url, ex);
          // Fall back to the parser's default behavior.
          source = null;
        }
      }
    }

    return source;
  }

}
