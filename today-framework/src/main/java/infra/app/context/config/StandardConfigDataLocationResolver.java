/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.app.context.config;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import infra.app.context.config.LocationResourceLoader.ResourceType;
import infra.app.env.PropertySourceLoader;
import infra.context.properties.bind.Binder;
import infra.core.Ordered;
import infra.core.env.Environment;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.ResourceUtils;
import infra.util.StringUtils;

/**
 * {@link ConfigDataLocationResolver} for standard locations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class StandardConfigDataLocationResolver implements ConfigDataLocationResolver<StandardConfigDataResource>, Ordered {

  private static final String PREFIX = "resource:";

  static final String CONFIG_NAME_PROPERTY = "app.config.name";

  static final String[] DEFAULT_CONFIG_NAMES = { "application" };

  private static final Pattern URL_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z0-9*]*?:)(.*$)");

  private static final Pattern EXTENSION_HINT_PATTERN = Pattern.compile("^(.*)\\[(\\.\\w+)](?!\\[)$");

  @Nullable
  private static final String NO_PROFILE = null;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final List<PropertySourceLoader> propertySourceLoaders;

  private final String[] configNames;

  private final LocationResourceLoader resourceLoader;

  /**
   * Create a new {@link StandardConfigDataLocationResolver} instance.
   *
   * @param binder a binder backed by the initial {@link Environment}
   * @param resourceLoader a {@link ResourceLoader} used to load resources
   */
  public StandardConfigDataLocationResolver(Binder binder, ResourceLoader resourceLoader) {
    this.propertySourceLoaders = TodayStrategies.find(PropertySourceLoader.class, resourceLoader.getClassLoader());
    this.configNames = getConfigNames(binder);
    this.resourceLoader = new LocationResourceLoader(resourceLoader);
  }

  private String[] getConfigNames(Binder binder) {
    String[] configNames = binder.bind(CONFIG_NAME_PROPERTY, String[].class).orRequired(DEFAULT_CONFIG_NAMES);
    for (String configName : configNames) {
      validateConfigName(configName);
    }
    return configNames;
  }

  private void validateConfigName(String name) {
    if (name.contains("*")) {
      throw new IllegalStateException("Config name '%s' cannot contain '*'".formatted(name));
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
    return true;
  }

  @Override
  public List<StandardConfigDataResource> resolve(
          ConfigDataLocationResolverContext context, ConfigDataLocation location) throws ConfigDataNotFoundException {
    return resolve(getReferences(context, location.split()));
  }

  private Set<StandardConfigDataReference> getReferences(
          ConfigDataLocationResolverContext context, ConfigDataLocation[] configDataLocations) {
    var references = new LinkedHashSet<StandardConfigDataReference>();
    for (ConfigDataLocation configDataLocation : configDataLocations) {
      references.addAll(getReferences(context, configDataLocation));
    }
    return references;
  }

  private Set<StandardConfigDataReference> getReferences(
          ConfigDataLocationResolverContext context, ConfigDataLocation configDataLocation) {
    String resourceLocation = getResourceLocation(context, configDataLocation);
    try {
      if (isDirectory(resourceLocation)) {
        return getReferencesForDirectory(configDataLocation, resourceLocation, NO_PROFILE);
      }
      return getReferencesForFile(configDataLocation, resourceLocation, NO_PROFILE);
    }
    catch (RuntimeException ex) {
      throw new IllegalStateException("Unable to load config data from '" + configDataLocation + "'", ex);
    }
  }

  @Override
  public List<StandardConfigDataResource> resolveProfileSpecific(
          ConfigDataLocationResolverContext context, ConfigDataLocation location, Profiles profiles) {
    validateProfiles(profiles);
    return resolve(getProfileSpecificReferences(context, location.split(), profiles));
  }

  private Set<StandardConfigDataReference> getProfileSpecificReferences(
          ConfigDataLocationResolverContext context, ConfigDataLocation[] configDataLocations, Profiles profiles) {
    var references = new LinkedHashSet<StandardConfigDataReference>();
    for (String profile : profiles) {
      for (ConfigDataLocation configDataLocation : configDataLocations) {
        String resourceLocation = getResourceLocation(context, configDataLocation);
        references.addAll(getReferences(configDataLocation, resourceLocation, profile));
      }
    }
    return references;
  }

  private void validateProfiles(Profiles profiles) {
    for (String profile : profiles) {
      validateProfile(profile);
    }
  }

  private void validateProfile(String profile) {
    Assert.hasText(profile, "'profile' must contain text");
    if (profile.startsWith("-") || profile.startsWith("_")) {
      throw new IllegalStateException("Invalid profile '%s': must not start with '-' or '_'".formatted(profile));
    }

    if (profile.endsWith("-") || profile.endsWith("_")) {
      throw new IllegalStateException("Invalid profile '%s': must not end with '-' or '_'".formatted(profile));
    }

    profile.codePoints().forEach(codePoint -> {
      if (codePoint == '-' || codePoint == '_' || Character.isLetterOrDigit(codePoint)) {
        return;
      }
      throw new IllegalStateException("Invalid profile '%s': must contain only letters, digits, '-', or '_'".formatted(profile));
    });
  }

  private String getResourceLocation(ConfigDataLocationResolverContext context, ConfigDataLocation configDataLocation) {
    String resourceLocation = configDataLocation.getNonPrefixedValue(PREFIX);
    boolean isAbsolute = resourceLocation.startsWith("/") || URL_PREFIX.matcher(resourceLocation).matches();
    if (isAbsolute) {
      return resourceLocation;
    }
    ConfigDataResource parent = context.getParent();
    if (parent instanceof StandardConfigDataResource std) {
      String parentResourceLocation = std.getReference().resourceLocation;
      String parentDirectory = parentResourceLocation.substring(0, parentResourceLocation.lastIndexOf("/") + 1);
      return parentDirectory + resourceLocation;
    }
    return resourceLocation;
  }

  private Set<StandardConfigDataReference> getReferences(
          ConfigDataLocation configDataLocation, String resourceLocation, String profile) {
    if (isDirectory(resourceLocation)) {
      return getReferencesForDirectory(configDataLocation, resourceLocation, profile);
    }
    return getReferencesForFile(configDataLocation, resourceLocation, profile);
  }

  private Set<StandardConfigDataReference> getReferencesForDirectory(
          ConfigDataLocation configDataLocation, String directory, @Nullable String profile) {
    var references = new LinkedHashSet<StandardConfigDataReference>();
    for (String name : configNames) {
      var referencesForName = getReferencesForConfigName(name, configDataLocation, directory, profile);
      references.addAll(referencesForName);
    }
    return references;
  }

  private Deque<StandardConfigDataReference> getReferencesForConfigName(String name,
          ConfigDataLocation configDataLocation, String directory, @Nullable String profile) {
    var references = new ArrayDeque<StandardConfigDataReference>();
    for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
      for (String extension : propertySourceLoader.getFileExtensions()) {
        var reference = new StandardConfigDataReference(configDataLocation, directory,
                directory + name, profile, extension, propertySourceLoader);
        if (!references.contains(reference)) {
          references.addFirst(reference);
        }
      }
    }
    return references;
  }

  private Set<StandardConfigDataReference> getReferencesForFile(
          ConfigDataLocation configDataLocation, String file, @Nullable String profile) {
    Matcher extensionHintMatcher = EXTENSION_HINT_PATTERN.matcher(file);
    boolean extensionHintLocation = extensionHintMatcher.matches();
    if (extensionHintLocation) {
      file = extensionHintMatcher.group(1) + extensionHintMatcher.group(2);
    }
    for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
      String extension = getLoadableFileExtension(propertySourceLoader, file);
      if (extension != null) {
        String root = file.substring(0, file.length() - extension.length() - 1);
        var reference = new StandardConfigDataReference(configDataLocation, null, root,
                profile, (!extensionHintLocation) ? extension : null, propertySourceLoader);
        return Collections.singleton(reference);
      }
    }
    if (configDataLocation.isOptional()) {
      return Collections.emptySet();
    }
    if (configDataLocation.hasPrefix(PREFIX)
            || configDataLocation.hasPrefix(ResourceUtils.FILE_URL_PREFIX)
            || configDataLocation.hasPrefix(ResourceUtils.CLASSPATH_URL_PREFIX)
            || configDataLocation.toString().indexOf(':') == -1) {
      throw new IllegalStateException("File extension is not known to any PropertySourceLoader. "
              + "If the location is meant to reference a directory, it must end in '/' or File.separator");
    }
    throw new IllegalStateException(
            "Incorrect ConfigDataLocationResolver chosen or file extension is not known to any PropertySourceLoader. "
                    + "If the location is meant to reference a directory, it must end in '/' or File.separator. "
                    + "The location is being resolved using the StandardConfigDataLocationResolver, "
                    + "check the location prefix if a different resolver is expected");
  }

  @Nullable
  private String getLoadableFileExtension(PropertySourceLoader loader, String file) {
    for (String fileExtension : loader.getFileExtensions()) {
      if (StringUtils.endsWithIgnoreCase(file, fileExtension)) {
        return fileExtension;
      }
    }
    return null;
  }

  private boolean isDirectory(String resourceLocation) {
    return resourceLocation.endsWith("/") || resourceLocation.endsWith(File.separator);
  }

  private List<StandardConfigDataResource> resolve(Set<StandardConfigDataReference> references) {
    var resolved = new ArrayList<StandardConfigDataResource>();
    for (StandardConfigDataReference reference : references) {
      resolved.addAll(resolve(reference));
    }
    if (resolved.isEmpty()) {
      resolved.addAll(resolveEmptyDirectories(references));
    }
    return resolved;
  }

  private Collection<StandardConfigDataResource> resolveEmptyDirectories(
          Set<StandardConfigDataReference> references) {
    var empty = new LinkedHashSet<StandardConfigDataResource>();
    for (StandardConfigDataReference reference : references) {
      if (reference.directory != null) {
        empty.addAll(resolveEmptyDirectories(reference));
      }
    }
    return empty;
  }

  private Set<StandardConfigDataResource> resolveEmptyDirectories(StandardConfigDataReference reference) {
    if (!resourceLoader.isPattern(reference.resourceLocation)) {
      return resolveNonPatternEmptyDirectories(reference);
    }
    return resolvePatternEmptyDirectories(reference);
  }

  private Set<StandardConfigDataResource> resolveNonPatternEmptyDirectories(StandardConfigDataReference reference) {
    Resource resource = resourceLoader.getResource(reference.directory);
    return (resource instanceof ClassPathResource || !resource.exists())
            ? Collections.emptySet()
            : Collections.singleton(new StandardConfigDataResource(reference, resource, true));
  }

  private Set<StandardConfigDataResource> resolvePatternEmptyDirectories(StandardConfigDataReference reference) {
    List<Resource> subdirectories = resourceLoader.getResources(reference.directory, ResourceType.DIRECTORY);
    ConfigDataLocation location = reference.configDataLocation;
    if (!location.isOptional() && CollectionUtils.isEmpty(subdirectories)) {
      String message = String.format("Config data location '%s' contains no subdirectories", location);
      throw new ConfigDataLocationNotFoundException(location, message, null);
    }
    return subdirectories.stream()
            .filter(Resource::exists)
            .map(resource -> new StandardConfigDataResource(reference, resource, true))
            .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private List<StandardConfigDataResource> resolve(StandardConfigDataReference reference) {
    if (!resourceLoader.isPattern(reference.resourceLocation)) {
      return resolveNonPattern(reference);
    }
    return resolvePattern(reference);
  }

  private List<StandardConfigDataResource> resolveNonPattern(StandardConfigDataReference reference) {
    Resource resource = resourceLoader.getResource(reference.resourceLocation);
    if (!resource.exists() && reference.isSkippable()) {
      logSkippingResource(reference);
      return Collections.emptyList();
    }
    return Collections.singletonList(createConfigResourceLocation(reference, resource));
  }

  private List<StandardConfigDataResource> resolvePattern(StandardConfigDataReference reference) {
    var resolved = new ArrayList<StandardConfigDataResource>();
    for (Resource resource : resourceLoader.getResources(reference.resourceLocation, ResourceType.FILE)) {
      if (!resource.exists() && reference.isSkippable()) {
        logSkippingResource(reference);
      }
      else {
        resolved.add(createConfigResourceLocation(reference, resource));
      }
    }
    return resolved;
  }

  private void logSkippingResource(StandardConfigDataReference reference) {
    this.logger.trace("Skipping missing resource {}", reference);
  }

  private StandardConfigDataResource createConfigResourceLocation(
          StandardConfigDataReference reference, Resource resource) {
    return new StandardConfigDataResource(reference, resource);
  }

}
