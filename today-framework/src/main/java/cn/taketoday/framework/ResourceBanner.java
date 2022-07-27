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

package cn.taketoday.framework;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.ansi.AnsiPropertySource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Version;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StreamUtils;

/**
 * Banner implementation that prints from a source text {@link Resource}.
 *
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Toshiaki Maki
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/29 18:01
 */
public class ResourceBanner implements Banner {

  private static final Logger logger = LoggerFactory.getLogger(ResourceBanner.class);

  private final Resource resource;

  public ResourceBanner(Resource resource) {
    Assert.notNull(resource, "Resource must not be null");
    Assert.isTrue(resource.exists(), "Resource must exist");
    this.resource = resource;
  }

  @Override
  public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
    try {
      String banner = StreamUtils.copyToString(this.resource.getInputStream(),
              environment.getProperty("banner.charset", Charset.class, StandardCharsets.UTF_8));

      for (PropertyResolver resolver : getPropertyResolvers(environment, sourceClass)) {
        banner = resolver.resolvePlaceholders(banner);
      }
      out.println(banner);
    }
    catch (Exception ex) {
      logger.warn("Banner not printable: %s (%s: '%s')", this.resource, ex.getClass(),
              ex.getMessage(), ex);
    }
  }

  protected List<PropertyResolver> getPropertyResolvers(Environment environment, Class<?> sourceClass) {
    List<PropertyResolver> resolvers = new ArrayList<>();
    resolvers.add(environment);
    resolvers.add(getVersionResolver(sourceClass));
    resolvers.add(getAnsiResolver());
    resolvers.add(getTitleResolver(sourceClass));
    return resolvers;
  }

  private PropertyResolver getVersionResolver(Class<?> sourceClass) {
    PropertySources propertySources = new PropertySources();
    propertySources.addLast(new MapPropertySource("version", getVersionsMap(sourceClass)));
    return new PropertySourcesPropertyResolver(propertySources);
  }

  private Map<String, Object> getVersionsMap(Class<?> sourceClass) {
    String appVersion = getApplicationVersion(sourceClass);
    String version = getVersion();
    Map<String, Object> versions = new HashMap<>();
    versions.put("application.version", getVersionString(appVersion, false));
    versions.put("framework.version", getVersionString(version, false));
    versions.put("application.formatted-version", getVersionString(appVersion, true));
    versions.put("framework.formatted-version", getVersionString(version, true));
    return versions;
  }

  protected String getApplicationVersion(Class<?> sourceClass) {
    Package sourcePackage = (sourceClass != null) ? sourceClass.getPackage() : null;
    return (sourcePackage != null) ? sourcePackage.getImplementationVersion() : null;
  }

  protected String getVersion() {
    return Version.instance.toString();
  }

  private String getVersionString(String version, boolean format) {
    if (version == null) {
      return "";
    }
    return format ? " (v" + version + ")" : version;
  }

  private PropertyResolver getAnsiResolver() {
    PropertySources sources = new PropertySources();
    sources.addFirst(new AnsiPropertySource("ansi", true));
    return new PropertySourcesPropertyResolver(sources);
  }

  private PropertyResolver getTitleResolver(Class<?> sourceClass) {
    PropertySources sources = new PropertySources();
    String applicationTitle = getApplicationTitle(sourceClass);
    Map<String, Object> titleMap = Collections.singletonMap("application.title",
            (applicationTitle != null) ? applicationTitle : "");
    sources.addFirst(new MapPropertySource("title", titleMap));
    return new PropertySourcesPropertyResolver(sources);
  }

  protected String getApplicationTitle(Class<?> sourceClass) {
    Package sourcePackage = (sourceClass != null) ? sourceClass.getPackage() : null;
    return (sourcePackage != null) ? sourcePackage.getImplementationTitle() : null;
  }

}
