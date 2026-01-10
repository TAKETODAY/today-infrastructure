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

package infra.context.properties.bind.handler;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import infra.context.properties.bind.AbstractBindHandler;
import infra.context.properties.bind.BindContext;
import infra.context.properties.bind.BindHandler;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.UnboundConfigurationPropertiesException;
import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.context.properties.source.IterableConfigurationPropertySource;

/**
 * {@link BindHandler} to enforce that all configuration properties under the root name
 * have been bound.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class NoUnboundElementsBindHandler extends AbstractBindHandler {

  private final Set<ConfigurationPropertyName> boundNames = new HashSet<>();

  private final Set<ConfigurationPropertyName> attemptedNames = new HashSet<>();

  private final Function<ConfigurationPropertySource, Boolean> filter;

  NoUnboundElementsBindHandler() {
    this(DEFAULT, (configurationPropertySource) -> true);
  }

  public NoUnboundElementsBindHandler(BindHandler parent, Function<ConfigurationPropertySource, Boolean> filter) {
    super(parent);
    this.filter = filter;
  }

  @Nullable
  @Override
  public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
    this.attemptedNames.add(name);
    return super.onStart(name, target, context);
  }

  @Nullable
  @Override
  public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
    this.boundNames.add(name);
    return super.onSuccess(name, target, context, result);
  }

  @Nullable
  @Override
  public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
          throws Exception {
    if (error instanceof UnboundConfigurationPropertiesException) {
      throw error;
    }
    return super.onFailure(name, target, context, error);
  }

  @Override
  public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, @Nullable Object result) {
    if (context.getDepth() == 0) {
      checkNoUnboundElements(name, context);
    }
  }

  private void checkNoUnboundElements(ConfigurationPropertyName name, BindContext context) {
    TreeSet<ConfigurationProperty> unbound = new TreeSet<>();
    for (ConfigurationPropertySource source : context.getSources()) {
      if (source instanceof IterableConfigurationPropertySource iterableSource && filter.apply(source)) {
        collectUnbound(name, unbound, iterableSource);
      }
    }
    if (!unbound.isEmpty()) {
      throw new UnboundConfigurationPropertiesException(unbound);
    }
  }

  private void collectUnbound(ConfigurationPropertyName name,
          Set<ConfigurationProperty> unbound, IterableConfigurationPropertySource source) {
    IterableConfigurationPropertySource filtered = source.filter((candidate) -> isUnbound(name, candidate));
    for (ConfigurationPropertyName unboundName : filtered) {
      try {
        unbound.add(source.filter((candidate) -> isUnbound(name, candidate)).getConfigurationProperty(unboundName));
      }
      catch (Exception ignored) {
      }
    }
  }

  private boolean isUnbound(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
    if (name.isAncestorOf(candidate)) {
      return !this.boundNames.contains(candidate) && !isOverriddenCollectionElement(candidate);
    }
    return false;
  }

  private boolean isOverriddenCollectionElement(ConfigurationPropertyName candidate) {
    int lastIndex = candidate.getNumberOfElements() - 1;
    if (candidate.isLastElementIndexed()) {
      ConfigurationPropertyName propertyName = candidate.chop(lastIndex);
      return this.boundNames.contains(propertyName);
    }
    Indexed indexed = getIndexed(candidate);
    if (indexed != null) {
      String zeroethProperty = indexed.name + "[0]";
      if (this.boundNames.contains(ConfigurationPropertyName.of(zeroethProperty))) {
        String nestedZeroethProperty = zeroethProperty + "." + indexed.nestedPropertyName;
        return isCandidateValidPropertyName(nestedZeroethProperty);
      }
    }
    return false;
  }

  private boolean isCandidateValidPropertyName(String nestedZeroethProperty) {
    return this.attemptedNames.contains(ConfigurationPropertyName.of(nestedZeroethProperty));
  }

  @Nullable
  private Indexed getIndexed(ConfigurationPropertyName candidate) {
    for (int i = 0; i < candidate.getNumberOfElements(); i++) {
      if (candidate.isNumericIndex(i)) {
        return new Indexed(candidate.chop(i).toString(),
                candidate.getElement(i + 1, ConfigurationPropertyName.Form.UNIFORM));
      }
    }
    return null;
  }

  private record Indexed(String name, String nestedPropertyName) {

  }

}
