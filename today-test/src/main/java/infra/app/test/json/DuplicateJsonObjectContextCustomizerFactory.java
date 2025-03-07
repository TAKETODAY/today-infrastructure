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

package infra.app.test.json;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import infra.context.ConfigurableApplicationContext;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.context.ContextConfigurationAttributes;
import infra.test.context.ContextCustomizer;
import infra.test.context.ContextCustomizerFactory;
import infra.test.context.MergedContextConfiguration;

/**
 * A {@link ContextCustomizerFactory} that produces a {@link ContextCustomizer} that warns
 * the user when multiple occurrences of {@code JSONObject} are found on the class path.
 *
 * @author Andy Wilkinson
 */
class DuplicateJsonObjectContextCustomizerFactory implements ContextCustomizerFactory {

  @Override
  public ContextCustomizer createContextCustomizer(Class<?> testClass,
          List<ContextConfigurationAttributes> configAttributes) {
    return new DuplicateJsonObjectContextCustomizer();
  }

  private static final class DuplicateJsonObjectContextCustomizer implements ContextCustomizer {

    private final Logger logger = LoggerFactory.getLogger(DuplicateJsonObjectContextCustomizer.class);

    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
      List<URL> jsonObjects = findJsonObjects();
      if (jsonObjects.size() > 1) {
        logDuplicateJsonObjectsWarning(jsonObjects);
      }
    }

    private List<URL> findJsonObjects() {
      try {
        Enumeration<URL> resources = getClass().getClassLoader().getResources("org/json/JSONObject.class");
        return Collections.list(resources);
      }
      catch (Exception ex) {
        // Continue
      }
      return Collections.emptyList();
    }

    private void logDuplicateJsonObjectsWarning(List<URL> jsonObjects) {
      StringBuilder message = new StringBuilder(
              String.format("%n%nFound multiple occurrences of org.json.JSONObject on the class path:%n%n"));
      for (URL jsonObject : jsonObjects) {
        message.append(String.format("\t%s%n", jsonObject));
      }
      message.append(
              String.format("%nYou may wish to exclude one of them to ensure predictable runtime behavior%n"));
      this.logger.warn(message);
    }

    @Override
    public boolean equals(Object obj) {
      return (obj != null) && (getClass() == obj.getClass());
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }

  }

}
