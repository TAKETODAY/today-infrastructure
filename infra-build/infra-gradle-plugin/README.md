# TODAY Infrastructure Gradle Plugins

Gradle plugins for building and running TODAY Infrastructure applications.

## Plugin Portal IDs

- `cn.taketoday.application`: runs applications and builds executable archives.
- `cn.taketoday.application.aot`: adds ahead-of-time processing support.

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
IDs `infra.application` and `infra.application.aot` remain available.
Normal builds declare these short IDs. The release workflow passes
`-PportalPublishing=true` to declare the namespaced Portal IDs and include the
short IDs as compatibility descriptors in the published jar.

The application plugin provides `infraRun` and `infraJar` tasks and automatically
applies dependency management with the matching Infrastructure BOM.
Configuration Cache support is not currently declared as supported.

See the [application documentation](../../infra-docs/modules/application/pages/using/non-web-applications.adoc)
and the [release publishing guide](../../.github/actions/publish-gradle-plugin/README.md).
