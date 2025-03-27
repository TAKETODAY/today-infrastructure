# Infra Build

This folder contains the custom plugins and conventions for the Infra build.
They are declared in the `build.gradle` file in this folder.

## Build Conventions

The `infra.building.conventions` plugin applies all conventions to the Framework build:

* Configuring the Java compiler, see `JavaConventions`
* Configuring testing in the build with `TestConventions` 


## Build Plugins

### Optional dependencies

The `infra.building.optional-dependencies` plugin creates a new `optional`
Gradle configuration - it adds the dependencies to the project's compile and runtime classpath
but doesn't affect the classpath of dependent projects.
This plugin does not provide a `provided` configuration, as the native `compileOnly` and `testCompileOnly`
configurations are preferred.

### API Diff

This plugin uses the [Gradle JApiCmp](https://github.com/melix/japicmp-gradle-plugin) plugin
to generate API Diff reports for each Infra module. This plugin is applied once on the root
project and creates tasks in each framework module. Unlike previous versions of this part of the build,
there is no need for checking out a specific tag. The plugin will fetch the JARs we want to compare the
current working version with. You can generate the reports for all modules or a single module:

```
./gradlew apiDiff -PbaselineVersion=5.1.0.RELEASE
./gradlew :today-core:apiDiff -PbaselineVersion=5.1.0.RELEASE
```      

The reports are located under `build/reports/api-diff/$OLDVERSION_to_$NEWVERSION/`.

### MultiRelease Jar

The `infra.building.multiReleaseJar` plugin configures the project with MultiRelease JAR support.
It creates a new SourceSet and dedicated tasks for each Java variant considered.
This can be configured with the DSL, by setting a list of Java variants to configure:

```groovy
plugins {
    id 'infra.building.multiReleaseJar'
}

multiRelease {
	releaseVersions 21, 24
}
```

Note, Java classes will be compiled with the toolchain pre-configured by the project, assuming that its
Java language version is equal or higher than all variants we consider. Each compilation task will only
set the "-release" compilation option accordingly to produce the expected bytecode version.


### RuntimeHints Java Agent

The `today-core-test` project module contributes the `RuntimeHintsAgent` Java agent.

The `RuntimeHintsAgentPlugin` Gradle plugin creates a dedicated `"runtimeHintsTest"` test task for each project.
This task will detect and execute [tests tagged](https://junit.org/junit5/docs/current/user-guide/#running-tests-build-gradle)
with the `"RuntimeHintsTests"` [JUnit tag](https://junit.org/junit5/docs/current/user-guide/#running-tests-tags).
In the Infra test suite, those are usually annotated with the `@EnabledIfRuntimeHintsAgent` annotation.

By default, the agent will instrument all classes located in the `"infra"` package, as they are loaded.
The `RuntimeHintsAgentExtension` allows to customize this using a DSL:

```groovy
// this applies the `RuntimeHintsAgentPlugin` to the project
plugins {
	id 'infra.building.runtimehints-agent'
}

// You can configure the agent to include and exclude packages from the instrumentation process.
runtimeHintsAgent {
	includedPackages = ["infra", "io.spring"]
	excludedPackages = ["org.example"]
}

dependencies {
    // to use the test infrastructure, the project should also depend on the "today-core-test" module
	testImplementation(project(":today-core-test"))
}
```

With this configuration, `./gradlew runtimeHintsTest` will run all tests instrumented by this java agent.
The global `./gradlew check` task depends on `runtimeHintsTest`.            

NOTE: the "today-core-test" module doesn't shade "today-core" by design, so the agent should never instrument
code that doesn't have "today-core" on its classpath.