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

package infra.beans.factory.xml;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ResourceUtils;

/**
 * {@code EntityResolver} implementation that tries to resolve entity references
 * through a {@link ResourceLoader} (usually,
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
 * @see ResourceLoader
 * @see infra.context.ApplicationContext
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
        source = resolveSchemaEntity(publicId, systemId);
      }
    }

    return source;
  }

  /**
   * A fallback method for {@link #resolveEntity(String, String)} that is used when a
   * "schema" entity (DTD or XSD) cannot be resolved as a local resource. The default
   * behavior is to perform remote resolution over HTTPS.
   * <p>Subclasses can override this method to change the default behavior.
   * <ul>
   * <li>Return {@code null} to fall back to the parser's
   * {@linkplain org.xml.sax.EntityResolver#resolveEntity(String, String) default behavior}.</li>
   * <li>Throw an exception to prevent remote resolution of the DTD or XSD.</li>
   * </ul>
   *
   * @param publicId the public identifier of the external entity being referenced,
   * or null if none was supplied
   * @param systemId the system identifier of the external entity being referenced,
   * representing the URL of the DTD or XSD
   * @return an InputSource object describing the new input source, or null to request
   * that the parser open a regular URI connection to the system identifier
   */
  @Nullable
  protected InputSource resolveSchemaEntity(@Nullable String publicId, String systemId) {
    InputSource source;
    // External dtd/xsd lookup via https even for canonical http declaration
    String url = systemId;
    if (url.startsWith("http:")) {
      url = "https:" + url.substring(5);
    }
    if (log.isWarnEnabled()) {
      log.warn("DTD/XSD XML entity [{}] not found, falling back to remote https resolution", systemId);
    }
    try {
      source = new InputSource(ResourceUtils.toURL(url).openStream());
      source.setPublicId(publicId);
      source.setSystemId(systemId);
    }
    catch (IOException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Could not resolve XML entity [{}] through URL [{}]", systemId, url, ex);
      }
      // Fall back to the parser's default behavior.
      source = null;
    }
    return source;
  }

}
