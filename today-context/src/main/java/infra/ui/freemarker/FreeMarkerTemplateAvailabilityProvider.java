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

package infra.ui.freemarker;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import infra.aot.hint.RuntimeHints;
import infra.context.properties.bind.BindableRuntimeHintsRegistrar;
import infra.lang.TodayStrategies;
import infra.ui.template.PathBasedTemplateAvailabilityProvider;
import infra.ui.template.TemplateAvailabilityProvider;
import infra.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * FreeMarker view templates.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class FreeMarkerTemplateAvailabilityProvider extends PathBasedTemplateAvailabilityProvider {

  private static final String REQUIRED_CLASS_NAME = "freemarker.template.Configuration";

  public static final String DEFAULT_PREFIX = TodayStrategies.getProperty("template.freemarker.default.prefix", "");

  public static final String DEFAULT_SUFFIX = TodayStrategies.getProperty("template.freemarker.default.suffix", ".ftl");

  public FreeMarkerTemplateAvailabilityProvider() {
    super(REQUIRED_CLASS_NAME, FreeMarkerTemplateAvailabilityProperties.class, "freemarker");
  }

  protected static final class FreeMarkerTemplateAvailabilityProperties extends TemplateAvailabilityProperties {

    private List<String> templateLoaderPath = new ArrayList<>(
            List.of(DEFAULT_TEMPLATE_LOADER_PATH));

    FreeMarkerTemplateAvailabilityProperties() {
      super(DEFAULT_PREFIX, DEFAULT_SUFFIX);
    }

    @Override
    protected List<String> getLoaderPath() {
      return this.templateLoaderPath;
    }

    public List<String> getTemplateLoaderPath() {
      return this.templateLoaderPath;
    }

    public void setTemplateLoaderPath(List<String> templateLoaderPath) {
      this.templateLoaderPath = templateLoaderPath;
    }

  }

  static class Hints extends BindableRuntimeHintsRegistrar {

    Hints() {
      super(FreeMarkerTemplateAvailabilityProperties.class);
    }

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      if (ClassUtils.isPresent(REQUIRED_CLASS_NAME, classLoader)) {
        super.registerHints(hints, classLoader);
      }
    }

  }

}
