/*
 * Copyright 2012-present the original author or authors.
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

package infra.groovy.template.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.context.properties.bind.BindableRuntimeHintsRegistrar;
import infra.ui.template.PathBasedTemplateAvailabilityProvider;
import infra.ui.template.TemplateAvailabilityProvider;
import infra.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for Groovy
 * view templates.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class GroovyTemplateAvailabilityProvider extends PathBasedTemplateAvailabilityProvider {

  private static final String REQUIRED_CLASS_NAME = "groovy.text.TemplateEngine";

  public GroovyTemplateAvailabilityProvider() {
    super(REQUIRED_CLASS_NAME, GroovyTemplateAvailabilityProperties.class, "groovy.template");
  }

  protected static final class GroovyTemplateAvailabilityProperties extends TemplateAvailabilityProperties {

    private List<String> resourceLoaderPath = new ArrayList<>(
            List.of(GroovyTemplateProperties.DEFAULT_RESOURCE_LOADER_PATH));

    GroovyTemplateAvailabilityProperties() {
      super(GroovyTemplateProperties.DEFAULT_PREFIX, GroovyTemplateProperties.DEFAULT_SUFFIX);
    }

    @Override
    protected List<String> getLoaderPath() {
      return this.resourceLoaderPath;
    }

    public List<String> getResourceLoaderPath() {
      return this.resourceLoaderPath;
    }

    public void setResourceLoaderPath(List<String> resourceLoaderPath) {
      this.resourceLoaderPath = resourceLoaderPath;
    }

  }

  static class GroovyTemplateAvailabilityRuntimeHints implements RuntimeHintsRegistrar {

    public GroovyTemplateAvailabilityRuntimeHints() {
    }

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      if (ClassUtils.isPresent(REQUIRED_CLASS_NAME, classLoader)) {
        BindableRuntimeHintsRegistrar.forTypes(GroovyTemplateAvailabilityProperties.class)
                .registerHints(hints, classLoader);
      }
    }

  }

}
