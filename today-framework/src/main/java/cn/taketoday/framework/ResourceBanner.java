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

package cn.taketoday.framework;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.ansi.AnsiPropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.MapPropertySource;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.env.PropertySourcesPropertyResolver;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Version;
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

  private final Resource resource;

  public ResourceBanner(Resource resource) {
    Assert.notNull(resource, "Resource is required");
    Assert.isTrue(resource.exists(), "Resource must exist");
    this.resource = resource;
  }

  @Override
  public void printBanner(Environment environment, @Nullable Class<?> sourceClass, PrintStream out) {
    try {
      String banner = StreamUtils.copyToString(resource.getInputStream(),
              environment.getProperty(Banner.BANNER_CHARSET, Charset.class, StandardCharsets.UTF_8));

      ArrayList<PropertyResolver> resolvers = new ArrayList<>();
      addPropertyResolvers(resolvers, environment, sourceClass);
      for (PropertyResolver resolver : resolvers) {
        banner = resolver.resolvePlaceholders(banner);
      }
      out.println(banner);
    }
    catch (Exception ex) {
      LoggerFactory.getLogger(ResourceBanner.class)
              .warn("Banner not printable: {} ({}: '{}')", this.resource, ex.getClass(),
                      ex.getMessage(), ex);
    }
  }

  protected void addPropertyResolvers(List<PropertyResolver> resolvers, Environment environment, @Nullable Class<?> sourceClass) {
    PropertySources sources = new PropertySources();

    if (environment instanceof ConfigurableEnvironment ce) {
      for (PropertySource<?> ps : ce.getPropertySources()) {
        sources.addLast(ps);
      }
    }

    sources.addLast(getVersionPropertySource(environment));
    sources.addLast(getAnsiPropertySource());
    sources.addLast(getTitlePropertySource(sourceClass));

    resolvers.add(new PropertySourcesPropertyResolver(sources));
  }

  private MapPropertySource getVersionPropertySource(Environment environment) {
    return new MapPropertySource("version", getVersionsMap(environment));
  }

  private Map<String, Object> getVersionsMap(Environment environment) {
    String appVersion = getApplicationVersion(environment);
    String version = getInfraVersion();
    HashMap<String, Object> versions = new HashMap<>();
    versions.put("app.version", getVersionString(appVersion, false));
    versions.put("infra.version", getVersionString(version, false));
    versions.put("app.formatted-version", getVersionString(appVersion, true));
    versions.put("infra.formatted-version", getVersionString(version, true));
    return versions;
  }

  private MapPropertySource getTitlePropertySource(@Nullable Class<?> sourceClass) {
    String applicationTitle = getApplicationTitle(sourceClass);
    Map<String, Object> titleMap = Collections.singletonMap("app.title",
            (applicationTitle != null) ? applicationTitle : "");
    return new MapPropertySource("title", titleMap);
  }

  @Nullable
  private String getApplicationVersion(Environment environment) {
    return environment.getProperty("app.version");
  }

  protected String getInfraVersion() {
    return Version.instance.implementationVersion();
  }

  private String getVersionString(@Nullable String version, boolean format) {
    if (version == null) {
      return "";
    }
    return format ? " (v" + version + ")" : version;
  }

  private AnsiPropertySource getAnsiPropertySource() {
    return new AnsiPropertySource("ansi", true);
  }

  @Nullable
  protected String getApplicationTitle(@Nullable Class<?> sourceClass) {
    if (sourceClass != null) {
      return sourceClass.getPackage().getImplementationTitle();
    }
    return null;
  }

}
