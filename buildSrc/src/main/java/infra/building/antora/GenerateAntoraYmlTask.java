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

package infra.building.antora;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unchecked")
public abstract class GenerateAntoraYmlTask extends DefaultTask {

  @OutputFile
  abstract RegularFileProperty getOutputFile();

  @Internal
  abstract Property<String> getComponentName();

  /**
   * The version of the documentation used to determine Antora's version and prerelease
   * attributes. Defaults to the project version.
   */
  @Internal
  public abstract Property<String> getVersion();

  @Internal
  public abstract MapProperty<String, Object> getAsciidocAttributes();

  @Internal
  public abstract MapProperty<String, Object> getAsciidoc();

  @Internal
  abstract MapProperty<String, Object> getYml();

  /**
   * The base yml file to use. If antora.yml exists, then it is used for the default. If
   * nothing is configured, then no base yml file is used.
   */
  @Internal
  abstract RegularFileProperty getBaseAntoraYmlFile();

  @Input
  final Map<String, Object> getAntoraYmlMap() throws FileNotFoundException {
    String componentName = getComponentName().getOrElse(null);
    Map<String, Object> versionAttributes = getAntoraVersionAttributes();
    Map<String, Object> asciidocAttributes = getAsciidocAttributes().getOrElse(Collections.emptyMap());
    Map<String, Object> asciidoc = getAsciidoc().getOrElse(Collections.emptyMap());
    Map<String, Object> yml = getYml().getOrElse(Collections.emptyMap());
    Map<String, Object> baseAntoraYml = loadBaseAntoraYmlFile();

    Map<String, Object> mergedAsciidocAttributes = mergeMaps(asciidocAttributes,
            fromMapGetNestedMap(asciidoc, "attributes"),
            fromMapGetNestedMap(yml, "asciidoc", "attributes"),
            fromMapGetNestedMap(baseAntoraYml, "asciidoc", "attributes"));
    Map<String, Object> mergedAsciidoc = mergeMaps(asciidoc,
            fromMapGetNestedMap(yml, "asciidoc"),
            fromMapGetNestedMap(baseAntoraYml, "asciidoc"));
    Map<String, Object> mergedYml = mergeMaps(yml, baseAntoraYml);
    if (!mergedAsciidocAttributes.isEmpty()) {
      mergedAsciidoc.put("attributes", mergedAsciidocAttributes);
    }
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    Object mergedName = mergedYml.get("name");
    result.put("name", Objects.requireNonNullElse(mergedName, componentName));
    intoMapPutAllFromMapIfAbsent(result, versionAttributes);
    intoMapPutAllFromMapIfAbsent(result, mergedYml);
    if (!mergedAsciidoc.isEmpty()) {
      result.put("asciidoc", mergedAsciidoc);
    }
    return result;
  }

  private static Map<String, Object> fromMapGetNestedMap(Map<String, Object> map, String... keys) {
    if (map == null) {
      return new LinkedHashMap<>();
    }
    for (String key : keys) {
      map = (Map<String, Object>) map.get(key);
      if (map == null) {
        return new LinkedHashMap<>();
      }
    }
    return map;
  }

  @SafeVarargs
  private static Map<String, Object> mergeMaps(Map<String, Object>... maps) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (Map<String, Object> map : maps) {
      intoMapPutAllFromMapIfAbsent(result, map);
    }
    return result;
  }

  private static void intoMapPutAllFromMapIfAbsent(Map<String, Object> intoMap, Map<String, Object> fromMap) {
    for (Map.Entry<String, Object> entry : fromMap.entrySet()) {
      intoMap.putIfAbsent(entry.getKey(), entry.getValue());
    }
  }

  @TaskAction
  public void writeAntoraYml() throws IOException {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    Yaml yaml = new Yaml(options);
    File generatedAntoraYmlFile = getOutputFile().get().getAsFile();
    Map<String, Object> data = getAntoraYmlMap();
    if (data.isEmpty()) {
      generatedAntoraYmlFile.createNewFile();
      return;
    }
    yaml.dump(data, new FileWriter(generatedAntoraYmlFile));
  }

  private Map<String, Object> getAntoraVersionAttributes() {
    String configuredVersion = getVersion().getOrNull();
    if (configuredVersion == null) {
      return Collections.emptyMap();
    }
    String antoraVersion = configuredVersion;
    Object prerelease = null;
    String[] versionComponents = configuredVersion.split("(?=-)");
    if (versionComponents.length > 1) {
      if ("-SNAPSHOT".equals(versionComponents[1])) {
        antoraVersion = versionComponents[0];
        prerelease = "-SNAPSHOT";
      }
      else {
        prerelease = Boolean.TRUE;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>(2);
    result.put("version", antoraVersion);
    if (prerelease != null) {
      result.put("prerelease", prerelease);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> loadBaseAntoraYmlFile() throws FileNotFoundException {
    RegularFile baseAntoraYmlRegularFile = getBaseAntoraYmlFile().getOrNull();
    File antoraYmlFile = baseAntoraYmlRegularFile == null ? null : baseAntoraYmlRegularFile.getAsFile();
    if (antoraYmlFile == null || !antoraYmlFile.exists()) {
      return Collections.emptyMap();
    }
    Yaml yaml = new Yaml();
    return yaml.loadAs(new FileInputStream(antoraYmlFile), LinkedHashMap.class);
  }
//
//	/**
//	 * Returns the output file to write the properties to.
//	 */
//	@OutputFile
//	public File getOutputFile() {
//		return getServices().get(FileOperations.class).file(this.outputFile);
//	}
}