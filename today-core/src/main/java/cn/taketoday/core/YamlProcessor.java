/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.core;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.inspector.TagInspector;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Base class for YAML factories.
 *
 * <p>Requires SnakeYAML 2.0 or higher
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Brian Clozel
 * @author TODAY 2021/9/30 23:33
 * @since 4.0
 */
public class YamlProcessor {
  private static final Logger log = LoggerFactory.getLogger(YamlProcessor.class);

  private boolean matchDefault = true;
  private Resource[] resources = Resource.EMPTY_ARRAY;
  private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;
  private List<DocumentMatcher> documentMatchers = Collections.emptyList();

  private Set<String> supportedTypes = Collections.emptySet();

  /**
   * A map of document matchers allowing callers to selectively use only
   * some of the documents in a YAML resource. In YAML documents are
   * separated by {@code ---} lines, and each document is converted
   * to properties before the match is made. E.g.
   * <pre class="code">
   * environment: dev
   * url: https://dev.bar.com
   * name: Developer Setup
   * ---
   * environment: prod
   * url:https://foo.bar.com
   * name: My Cool App
   * </pre>
   * when mapped with
   * <pre class="code">
   * setDocumentMatchers(properties -&gt;
   *     ("prod".equals(properties.getProperty("environment")) ? MatchStatus.FOUND : MatchStatus.NOT_FOUND));
   * </pre>
   * would end up as
   * <pre class="code">
   * environment=prod
   * url=https://foo.bar.com
   * name=My Cool App
   * </pre>
   */
  public void setDocumentMatchers(DocumentMatcher... matchers) {
    this.documentMatchers = Arrays.asList(matchers);
  }

  /**
   * Flag indicating that a document for which all the
   * {@link #setDocumentMatchers(DocumentMatcher...) document matchers} abstain will
   * nevertheless match. Default is {@code true}.
   */
  public void setMatchDefault(boolean matchDefault) {
    this.matchDefault = matchDefault;
  }

  /**
   * Method to use for resolving resources. Each resource will be converted to a Map,
   * so this property is used to decide which map entries to keep in the final output
   * from this factory. Default is {@link ResolutionMethod#OVERRIDE}.
   */
  public void setResolutionMethod(ResolutionMethod resolutionMethod) {
    Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
    this.resolutionMethod = resolutionMethod;
  }

  /**
   * Set locations of YAML {@link Resource resources} to be loaded.
   *
   * @see ResolutionMethod
   */
  public void setResources(Resource... resources) {
    this.resources = resources;
  }

  /**
   * Set the supported types that can be loaded from YAML documents.
   * <p>If no supported types are configured, only Java standard classes
   * (as defined in {@link org.yaml.snakeyaml.constructor.SafeConstructor})
   * encountered in YAML documents will be supported.
   * If an unsupported type is encountered, an {@link IllegalStateException}
   * will be thrown when the corresponding YAML node is processed.
   *
   * @param supportedTypes the supported types, or an empty array to clear the
   * supported types
   * @see #createYaml()
   */
  public void setSupportedTypes(Class<?>... supportedTypes) {
    if (ObjectUtils.isEmpty(supportedTypes)) {
      this.supportedTypes = Collections.emptySet();
    }
    else {
      Assert.noNullElements(supportedTypes, "'supportedTypes' must not contain null elements");
      this.supportedTypes = Arrays.stream(supportedTypes)
              .map(Class::getName)
              .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet));
    }
  }

  /**
   * Provide an opportunity for subclasses to process the Yaml parsed from the supplied
   * resources. Each resource is parsed in turn and the documents inside checked against
   * the {@link #setDocumentMatchers(DocumentMatcher...) matchers}. If a document
   * matches it is passed into the callback, along with its representation as Properties.
   * Depending on the {@link #setResolutionMethod(ResolutionMethod)} not all of the
   * documents will be parsed.
   *
   * @param callback a callback to delegate to once matching documents are found
   * @see #createYaml()
   */
  protected void process(MatchCallback callback) {
    Yaml yaml = createYaml();
    for (Resource resource : this.resources) {
      boolean found = process(callback, yaml, resource);
      if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
        return;
      }
    }
  }

  /**
   * Create the {@link Yaml} instance to use.
   * <p>The default implementation sets the "allowDuplicateKeys" flag to {@code false},
   * enabling built-in duplicate key handling in SnakeYAML 1.18+.
   * <p>If custom {@linkplain #setSupportedTypes supported types}
   * have been configured, the default implementation creates
   * a {@code Yaml} instance that filters out unsupported types encountered in
   * YAML documents. If an unsupported type is encountered, an
   * {@link IllegalStateException} will be thrown when the node is processed.
   *
   * @see LoaderOptions#setAllowDuplicateKeys(boolean)
   */
  protected Yaml createYaml() {
    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setAllowDuplicateKeys(false);
    loaderOptions.setTagInspector(new SupportedTagInspector());
    DumperOptions dumperOptions = new DumperOptions();

    return new Yaml(new Constructor(loaderOptions),
            new Representer(dumperOptions), dumperOptions, loaderOptions);
  }

  private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
    int count = 0;
    try {
      log.debug("Loading from YAML: {}", resource);
      try (UnicodeReader reader = new UnicodeReader(resource.getInputStream())) {
        for (Object object : yaml.loadAll(reader)) {
          if (object != null && process(asMap(object), callback)) {
            count++;
            if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
              break;
            }
          }
        }
        log.debug("Loaded {} document {} from YAML resource: {}", count, (count > 1 ? "s" : Constant.BLANK), resource);
      }
    }
    catch (IOException ex) {
      handleProcessError(resource, ex);
    }
    return (count > 0);
  }

  private void handleProcessError(Resource resource, IOException ex) {
    if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND
            && this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
      throw new IllegalStateException(ex);
    }
    log.warn("Could not load map from {}: {}", resource, ex.getMessage(), ex);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object object) {
    // YAML can have numbers as keys
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    if (!(object instanceof Map)) {
      // A document can be a text literal
      result.put("document", object);
      return result;
    }

    Map<Object, Object> map = (Map<Object, Object>) object;
    for (Map.Entry<Object, Object> entry : map.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof Map) {
        value = asMap(value);
      }
      if (key instanceof CharSequence) {
        result.put(key.toString(), value);
      }
      else {
        // It has to be a map key in this case
        result.put("[" + key.toString() + "]", value);
      }
    }
    return result;
  }

  private boolean process(Map<String, Object> map, MatchCallback callback) {
    Properties properties = CollectionUtils.createStringAdaptingProperties();
    properties.putAll(getFlattenedMap(map));

    if (this.documentMatchers.isEmpty()) {
      log.debug("Merging document (no matchers set): {}", map);
      callback.process(properties, map);
      return true;
    }

    MatchStatus result = MatchStatus.ABSTAIN;
    for (DocumentMatcher matcher : this.documentMatchers) {
      MatchStatus match = matcher.matches(properties);
      result = MatchStatus.getMostSpecific(match, result);
      if (match == MatchStatus.FOUND) {
        log.debug("Matched document with document matcher: {}", properties);
        callback.process(properties, map);
        return true;
      }
    }

    if (result == MatchStatus.ABSTAIN && this.matchDefault) {
      log.debug("Matched document with default matcher: {}", map);
      callback.process(properties, map);
      return true;
    }

    log.debug("Unmatched document: {}", map);
    return false;
  }

  /**
   * Return a flattened version of the given map, recursively following any nested Map
   * or Collection values. Entries from the resulting map retain the same order as the
   * source. When called with the Map from a {@link MatchCallback} the result will
   * contain the same values as the {@link MatchCallback} Properties.
   *
   * @param source the source map
   * @return a flattened map
   */
  protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    buildFlattenedMap(result, source, null);
    return result;
  }

  private void buildFlattenedMap(
          Map<String, Object> result, Map<String, Object> source, @Nullable String path) {

    for (Map.Entry<String, Object> entry : source.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (StringUtils.hasText(path)) {
        if (StringUtils.matchesFirst(key, '[')) {
          key = path + key;
        }
        else {
          key = path + '.' + key;
        }
      }
      if (value instanceof String) {
        result.put(key, value);
      }
      else if (value instanceof Map) {
        // Need a compound key
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        buildFlattenedMap(result, map, key);
      }
      else if (value instanceof Collection) {
        // Need a compound key
        @SuppressWarnings("unchecked")
        Collection<Object> collection = (Collection<Object>) value;
        if (collection.isEmpty()) {
          result.put(key, Constant.BLANK);
        }
        else {
          int count = 0;
          for (Object object : collection) {
            buildFlattenedMap(result, Collections.singletonMap("[" + (count++) + "]", object), key);
          }
        }
      }
      else {
        result.put(key, (value != null ? value : Constant.BLANK));
      }
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  protected void merge(Map<String, Object> output, Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      Object existing = output.get(key);
      if (value instanceof Map && existing instanceof Map) {
        // Inner cast required by Eclipse IDE.
        LinkedHashMap<String, Object> result = new LinkedHashMap<>((Map<String, Object>) existing);
        merge(result, (Map) value);
        output.put(key, result);
      }
      else {
        output.put(key, value);
      }
    }
  }

  /**
   * Callback interface used to process the YAML parsing results.
   */
  @FunctionalInterface
  public interface MatchCallback {

    /**
     * Process the given representation of the parsing results.
     *
     * @param properties the properties to process (as a flattened
     * representation with indexed keys in case of a collection or map)
     * @param map the result map (preserving the original value structure
     * in the YAML document)
     */
    void process(Properties properties, Map<String, Object> map);
  }

  /**
   * Strategy interface used to test if properties match.
   */
  @FunctionalInterface
  public interface DocumentMatcher {

    /**
     * Test if the given properties match.
     *
     * @param properties the properties to test
     * @return the status of the match
     */
    MatchStatus matches(Properties properties);
  }

  /**
   * Status returned from {@link DocumentMatcher#matches(java.util.Properties)}.
   */
  public enum MatchStatus {

    /**
     * A match was found.
     */
    FOUND,

    /**
     * No match was found.
     */
    NOT_FOUND,

    /**
     * The matcher should not be considered.
     */
    ABSTAIN;

    /**
     * Compare two {@link MatchStatus} items, returning the most specific status.
     */
    public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
      return (a.ordinal() < b.ordinal() ? a : b);
    }
  }

  /**
   * Method to use for resolving resources.
   */
  public enum ResolutionMethod {

    /**
     * Replace values from earlier in the list.
     */
    OVERRIDE,

    /**
     * Replace values from earlier in the list, ignoring any failures.
     */
    OVERRIDE_AND_IGNORE,

    /**
     * Take the first resource in the list that exists and use just that.
     */
    FIRST_FOUND
  }

  private class SupportedTagInspector implements TagInspector {

    @Override
    public boolean isGlobalTagAllowed(Tag tag) {
      return supportedTypes.contains(tag.getClassName());
    }
  }

}
