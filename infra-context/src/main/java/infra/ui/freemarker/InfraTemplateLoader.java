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

package infra.ui.freemarker;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import freemarker.cache.TemplateLoader;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * FreeMarker {@link TemplateLoader} adapter that loads via a Framework {@link ResourceLoader}.
 * Used by {@link FreeMarkerConfigurationFactory} for any resource loader path that cannot
 * be resolved to a {@link java.io.File}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FreeMarkerConfigurationFactory#setTemplateLoaderPath
 * @see freemarker.template.Configuration#setDirectoryForTemplateLoading
 * @since 4.0 2022/2/5 13:03
 */
public class InfraTemplateLoader implements TemplateLoader {

  private static final Logger log = LoggerFactory.getLogger(InfraTemplateLoader.class);

  private final String templateLoaderPath;

  private final ResourceLoader resourceLoader;

  /**
   * Create a new InfraTemplateLoader.
   *
   * @param resourceLoader the ResourceLoader to use
   * @param templateLoaderPath the template loader path to use
   */
  public InfraTemplateLoader(ResourceLoader resourceLoader, String templateLoaderPath) {
    this.resourceLoader = resourceLoader;
    if (!templateLoaderPath.endsWith("/")) {
      templateLoaderPath += "/";
    }
    this.templateLoaderPath = templateLoaderPath;
    if (log.isDebugEnabled()) {
      log.debug("TemplateLoader for FreeMarker: using resource loader [{}] and template loader path [{}]",
              resourceLoader, templateLoaderPath);
    }
  }

  @Override
  @Nullable
  public Object findTemplateSource(String name) {
    if (log.isDebugEnabled()) {
      log.debug("Looking for FreeMarker template with name [{}]", name);
    }
    Resource resource = resourceLoader.getResource(this.templateLoaderPath + name);
    return resource.exists() ? resource : null;
  }

  @Override
  public Reader getReader(Object templateSource, String encoding) throws IOException {
    Resource resource = (Resource) templateSource;
    try {
      return new InputStreamReader(resource.getInputStream(), encoding);
    }
    catch (IOException ex) {
      log.debug("Could not find FreeMarker template: {}", resource);
      throw ex;
    }
  }

  @Override
  public long getLastModified(Object templateSource) {
    Resource resource = (Resource) templateSource;
    try {
      return resource.lastModified();
    }
    catch (IOException ex) {
      log.debug("Could not obtain last-modified timestamp for FreeMarker template in {} : {}",
              resource, ex.toString());
      return -1;
    }
  }

  @Override
  public void closeTemplateSource(Object templateSource) {
  }

}

