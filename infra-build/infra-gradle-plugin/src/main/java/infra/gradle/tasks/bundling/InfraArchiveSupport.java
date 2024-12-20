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

package infra.gradle.tasks.bundling;

import org.gradle.api.GradleException;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.java.archives.Manifest;
import org.gradle.api.provider.Property;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

import infra.lang.Version;

/**
 * Support class for implementations of {@link InfraArchive}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see InfraJar
 * @see InfraWar
 * @since 4.0
 */
class InfraArchiveSupport {

  private static final byte[] ZIP_FILE_HEADER = new byte[] { 'P', 'K', 3, 4 };

  private static final String UNSPECIFIED_VERSION = "unspecified";

  private static final Set<String> DEFAULT_LAUNCHER_CLASSES = Set.of(
          "infra.app.loader.JarLauncher",
          "infra.app.loader.PropertiesLauncher",
          "infra.app.loader.WarLauncher"
  );

  private final PatternSet requiresUnpack = new PatternSet();

  private final PatternSet exclusions = new PatternSet();

  private final String loaderMainClass;

  private final Spec<FileCopyDetails> librarySpec;

  private final Function<FileCopyDetails, ZipCompression> compressionResolver;

  private LaunchScriptConfiguration launchScript;

  InfraArchiveSupport(String loaderMainClass, Spec<FileCopyDetails> librarySpec,
          Function<FileCopyDetails, ZipCompression> compressionResolver) {
    this.loaderMainClass = loaderMainClass;
    this.librarySpec = librarySpec;
    this.compressionResolver = compressionResolver;
    this.requiresUnpack.include(Specs.satisfyNone());
  }

  void configureManifest(Manifest manifest, String mainClass, String classes, String lib, String classPathIndex,
          String layersIndex, String jdkVersion, String implementationTitle, Object implementationVersion) {
    Attributes attributes = manifest.getAttributes();
    attributes.putIfAbsent("Main-Class", this.loaderMainClass);
    attributes.putIfAbsent("Start-Class", mainClass);
    attributes.computeIfAbsent("Infra-Version", (name) -> determineInfraVersion());
    attributes.putIfAbsent("Infra-App-Classes", classes);
    attributes.putIfAbsent("Infra-App-Lib", lib);
    if (classPathIndex != null) {
      attributes.putIfAbsent("Infra-App-Classpath-Index", classPathIndex);
    }
    if (layersIndex != null) {
      attributes.putIfAbsent("Infra-App-Layers-Index", layersIndex);
    }
    attributes.putIfAbsent("Build-Jdk-Spec", jdkVersion);
    attributes.putIfAbsent("Implementation-Title", implementationTitle);
    if (implementationVersion != null) {
      String versionString = implementationVersion.toString();
      if (!UNSPECIFIED_VERSION.equals(versionString)) {
        attributes.putIfAbsent("Implementation-Version", versionString);
      }
    }
  }

  private String determineInfraVersion() {
    return Version.get().implementationVersion();
  }

  CopyAction createCopyAction(Jar jar, ResolvedDependencies resolvedDependencies) {
    return createCopyAction(jar, resolvedDependencies, null, null);
  }

  CopyAction createCopyAction(Jar jar, ResolvedDependencies resolvedDependencies,
          LayerResolver layerResolver, String layerToolsLocation) {
    File output = jar.getArchiveFile().get().getAsFile();
    Manifest manifest = jar.getManifest();
    boolean preserveFileTimestamps = jar.isPreserveFileTimestamps();
    Integer dirMode = getDirMode(jar);
    Integer fileMode = getFileMode(jar);
    boolean includeDefaultLoader = isUsingDefaultLoader(jar);
    Spec<FileTreeElement> requiresUnpack = this.requiresUnpack.getAsSpec();
    Spec<FileTreeElement> exclusions = this.exclusions.getAsExcludeSpec();
    LaunchScriptConfiguration launchScript = this.launchScript;
    Spec<FileCopyDetails> librarySpec = this.librarySpec;
    Function<FileCopyDetails, ZipCompression> compressionResolver = this.compressionResolver;
    String encoding = jar.getMetadataCharset();
    CopyAction action = new InfraZipCopyAction(output, manifest, preserveFileTimestamps, dirMode, fileMode,
            includeDefaultLoader, layerToolsLocation, requiresUnpack, exclusions, launchScript, librarySpec,
            compressionResolver, encoding, resolvedDependencies, layerResolver);
    return jar.isReproducibleFileOrder() ? new ReproducibleOrderingCopyAction(action) : action;
  }

  private Integer getDirMode(CopySpec copySpec) {
    return getMode(copySpec, "getDirPermissions", copySpec::getDirMode);
  }

  private Integer getFileMode(CopySpec copySpec) {
    return getMode(copySpec, "getFilePermissions", copySpec::getFileMode);
  }

  @SuppressWarnings("unchecked")
  private Integer getMode(CopySpec copySpec, String methodName, Supplier<Integer> fallback) {
    if (GradleVersion.current().compareTo(GradleVersion.version("8.3")) >= 0) {
      try {
        Object filePermissions = ((Property<Object>) copySpec.getClass().getMethod(methodName).invoke(copySpec))
                .getOrNull();
        return (filePermissions != null)
                ? (int) filePermissions.getClass().getMethod("toUnixNumeric").invoke(filePermissions) : null;
      }
      catch (Exception ex) {
        throw new GradleException("Failed to get permissions", ex);
      }
    }
    return fallback.get();
  }

  private boolean isUsingDefaultLoader(Jar jar) {
    return DEFAULT_LAUNCHER_CLASSES.contains(String.valueOf(jar.getManifest().getAttributes().get("Main-Class")));
  }

  LaunchScriptConfiguration getLaunchScript() {
    return this.launchScript;
  }

  void setLaunchScript(LaunchScriptConfiguration launchScript) {
    this.launchScript = launchScript;
  }

  void requiresUnpack(String... patterns) {
    this.requiresUnpack.include(patterns);
  }

  void requiresUnpack(Spec<FileTreeElement> spec) {
    this.requiresUnpack.include(spec);
  }

  void excludeNonZipLibraryFiles(FileCopyDetails details) {
    if (this.librarySpec.isSatisfiedBy(details)) {
      excludeNonZipFiles(details);
    }
  }

  void excludeNonZipFiles(FileCopyDetails details) {
    if (!isZip(details.getFile())) {
      details.exclude();
    }
  }

  private boolean isZip(File file) {
    try {
      try (FileInputStream fileInputStream = new FileInputStream(file)) {
        return isZip(fileInputStream);
      }
    }
    catch (IOException ex) {
      return false;
    }
  }

  private boolean isZip(InputStream inputStream) throws IOException {
    for (byte headerByte : ZIP_FILE_HEADER) {
      if (inputStream.read() != headerByte) {
        return false;
      }
    }
    return true;
  }

  void moveModuleInfoToRoot(CopySpec spec) {
    spec.filesMatching("module-info.class", this::moveToRoot);
  }

  void moveToRoot(FileCopyDetails details) {
    details.setRelativePath(details.getRelativeSourcePath());
  }

  /**
   * {@link CopyAction} variant that sorts entries to ensure reproducible ordering.
   */
  private static final class ReproducibleOrderingCopyAction implements CopyAction {

    private final CopyAction delegate;

    private ReproducibleOrderingCopyAction(CopyAction delegate) {
      this.delegate = delegate;
    }

    @Override
    public WorkResult execute(CopyActionProcessingStream stream) {
      return this.delegate.execute((action) -> {
        Map<RelativePath, FileCopyDetailsInternal> detailsByPath = new TreeMap<>();
        stream.process((details) -> detailsByPath.put(details.getRelativePath(), details));
        detailsByPath.values().forEach(action::processFile);
      });
    }

  }

}
