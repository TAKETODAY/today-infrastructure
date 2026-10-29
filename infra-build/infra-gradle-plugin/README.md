# TODAY Infrastructure Gradle Plugins

Gradle plugins for building and running TODAY Infrastructure applications.

## Plugin Portal IDs

- `cn.taketoday.application`: runs applications, builds executable archives, and provides opt-in AOT processing and GraalVM native-image integration.

After a version has been published and approved on the Gradle Plugin Portal:

```groovy
plugins {
  id 'java'
  id 'cn.taketoday.application' version '<published-version>'
}
```

The implementation artifact uses Maven coordinates
`cn.taketoday:infra-gradle-plugin:<version>`.
For builds loading that artifact through `buildscript` or `buildSrc`, the legacy
ID `infra.application` remains available. The separate `infra.application.aot` ID
has been removed; use `infraApplication.enableAheadOfTimeProcessing()` instead.
Only the application ID is declared for publication; the short application ID remains
a compatibility descriptor in the published jar. The release workflow passes
`-PportalPublishing=true` to enable Portal publishing.

Enable AOT through the application plugin without applying a separate plugin ID:

```groovy
infraApplication {
  enableAheadOfTimeProcessing()
}
```

AOT is disabled by default and is enabled automatically when the GraalVM Native
Image plugin is applied. Enabling AOT adds generated outputs to the main and test
runtime classpaths, so tasks consuming them may trigger AOT processing.

GraalVM Native Image support can also be enabled without applying
`org.graalvm.buildtools.native` yourself:

```groovy
infraApplication {
  enableNativeImage()
}
```

The Native Build Tools version is provided by the plugin, so the build does not
need to declare it.

The application plugin provides `infraRun` and `infraJar` tasks and automatically
applies dependency management with the matching Infrastructure BOM.
Configuration Cache support is not currently declared as supported.

See the [Gradle plugin reference guide](../../infra-docs/modules/gradle-plugin/pages/index.adoc),
the [application documentation](../../infra-docs/modules/application/pages/using/non-web-applications.adoc),
and the [release publishing guide](../../.github/actions/publish-gradle-plugin/README.md).
