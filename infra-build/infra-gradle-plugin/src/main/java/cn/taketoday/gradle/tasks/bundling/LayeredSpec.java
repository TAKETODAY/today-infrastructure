/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import cn.taketoday.app.loader.tools.Layer;
import cn.taketoday.app.loader.tools.Layers;
import cn.taketoday.app.loader.tools.Library;
import cn.taketoday.app.loader.tools.layer.ApplicationContentFilter;
import cn.taketoday.app.loader.tools.layer.ContentFilter;
import cn.taketoday.app.loader.tools.layer.ContentSelector;
import cn.taketoday.app.loader.tools.layer.CustomLayers;
import cn.taketoday.app.loader.tools.layer.IncludeExcludeContentSelector;
import cn.taketoday.app.loader.tools.layer.LibraryContentFilter;
import cn.taketoday.lang.Assert;

/**
 * Encapsulates the configuration for a layered archive.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class LayeredSpec {

  private ApplicationSpec application;

  private DependenciesSpec dependencies;

  private Layers layers;

  @Inject
  public LayeredSpec(ObjectFactory objects) {
    this.application = objects.newInstance(ApplicationSpec.class);
    this.dependencies = objects.newInstance(DependenciesSpec.class);
    getEnabled().convention(true);
    getIncludeLayerTools().convention(true);
  }

  /**
   * Returns whether the layer tools should be included as a dependency in the layered
   * archive.
   *
   * @return whether the layer tools should be included
   */
  @Input
  public abstract Property<Boolean> getIncludeLayerTools();

  /**
   * Returns whether the layers.idx should be included in the archive.
   *
   * @return whether the layers.idx should be included
   */
  @Input
  public abstract Property<Boolean> getEnabled();

  /**
   * Returns the {@link ApplicationSpec} that controls the layers to which application
   * classes and resources belong.
   *
   * @return the application spec
   */
  @Input
  public ApplicationSpec getApplication() {
    return this.application;
  }

  /**
   * Sets the {@link ApplicationSpec} that controls the layers to which application
   * classes are resources belong.
   *
   * @param spec the application spec
   */
  public void setApplication(ApplicationSpec spec) {
    this.application = spec;
  }

  /**
   * Customizes the {@link ApplicationSpec} using the given {@code action}.
   *
   * @param action the action
   */
  public void application(Action<ApplicationSpec> action) {
    action.execute(this.application);
  }

  /**
   * Returns the {@link DependenciesSpec} that controls the layers to which dependencies
   * belong.
   *
   * @return the dependencies spec
   */
  @Input
  public DependenciesSpec getDependencies() {
    return this.dependencies;
  }

  /**
   * Sets the {@link DependenciesSpec} that controls the layers to which dependencies
   * belong.
   *
   * @param spec the dependencies spec
   */
  public void setDependencies(DependenciesSpec spec) {
    this.dependencies = spec;
  }

  /**
   * Customizes the {@link DependenciesSpec} using the given {@code action}.
   *
   * @param action the action
   */
  public void dependencies(Action<DependenciesSpec> action) {
    action.execute(this.dependencies);
  }

  /**
   * Returns the order of the layers in the archive from least to most frequently
   * changing.
   *
   * @return the layer order
   */
  @Input
  @Optional
  public abstract ListProperty<String> getLayerOrder();

  /**
   * Return this configuration as a {@link Layers} instance. This method should only be
   * called when the configuration is complete and will no longer be changed.
   *
   * @return the layers
   */
  Layers asLayers() {
    Layers layers = this.layers;
    if (layers == null) {
      layers = createLayers();
      this.layers = layers;
    }
    return layers;
  }

  private Layers createLayers() {
    List<String> layerOrder = getLayerOrder().getOrNull();
    if (layerOrder == null || layerOrder.isEmpty()) {
      Assert.state(this.application.isEmpty() && this.dependencies.isEmpty(),
              "The 'layerOrder' must be defined when using custom layering");
      return Layers.IMPLICIT;
    }
    List<Layer> layers = layerOrder.stream().map(Layer::new).toList();
    return new CustomLayers(layers, this.application.asSelectors(), this.dependencies.asSelectors());
  }

  /**
   * Base class for specs that control the layers to which a category of content should
   * belong.
   *
   * @param <S> the type of {@link IntoLayerSpec} used by this spec
   */
  public abstract static class IntoLayersSpec<S extends IntoLayerSpec> implements Serializable {

    private final List<IntoLayerSpec> intoLayers;

    private final Function<String, S> specFactory;

    boolean isEmpty() {
      return this.intoLayers.isEmpty();
    }

    IntoLayersSpec(Function<String, S> specFactory, IntoLayerSpec... spec) {
      this.intoLayers = new ArrayList<>(Arrays.asList(spec));
      this.specFactory = specFactory;
    }

    public void intoLayer(String layer) {
      this.intoLayers.add(this.specFactory.apply(layer));
    }

    public void intoLayer(String layer, Action<S> action) {
      S spec = this.specFactory.apply(layer);
      action.execute(spec);
      this.intoLayers.add(spec);
    }

    <T> List<ContentSelector<T>> asSelectors(Function<IntoLayerSpec, ContentSelector<T>> selectorFactory) {
      return this.intoLayers.stream().map(selectorFactory).toList();
    }

  }

  /**
   * Spec that controls the content that should be part of a particular layer.
   */
  public static class IntoLayerSpec implements Serializable {

    private final String intoLayer;

    private final List<String> includes = new ArrayList<>();

    private final List<String> excludes = new ArrayList<>();

    /**
     * Creates a new {@code IntoLayerSpec} that will control the content of the given
     * layer.
     *
     * @param intoLayer the layer
     */
    public IntoLayerSpec(String intoLayer) {
      this.intoLayer = intoLayer;
    }

    /**
     * Adds patterns that control the content that is included in the layer. If no
     * includes are specified then all content is included. If includes are specified
     * then content must match an inclusion and not match any exclusions to be
     * included.
     *
     * @param patterns the patterns to be included
     */
    public void include(String... patterns) {
      this.includes.addAll(Arrays.asList(patterns));
    }

    /**
     * Adds patterns that control the content that is excluded from the layer. If no
     * excludes a specified no content is excluded. If exclusions are specified then
     * any content that matches an exclusion will be excluded irrespective of whether
     * it matches an include.
     *
     * @param patterns the patterns to be excluded
     */
    public void exclude(String... patterns) {
      this.includes.addAll(Arrays.asList(patterns));
    }

    <T> ContentSelector<T> asSelector(Function<String, ContentFilter<T>> filterFactory) {
      Layer layer = new Layer(this.intoLayer);
      return new IncludeExcludeContentSelector<>(layer, this.includes, this.excludes, filterFactory);
    }

    String getIntoLayer() {
      return this.intoLayer;
    }

    List<String> getIncludes() {
      return this.includes;
    }

    List<String> getExcludes() {
      return this.excludes;
    }

  }

  /**
   * Spec that controls the dependencies that should be part of a particular layer.
   *
   * @since 4.0
   */
  public static class DependenciesIntoLayerSpec extends IntoLayerSpec {

    private boolean includeProjectDependencies;

    private boolean excludeProjectDependencies;

    /**
     * Creates a new {@code IntoLayerSpec} that will control the content of the given
     * layer.
     *
     * @param intoLayer the layer
     */
    public DependenciesIntoLayerSpec(String intoLayer) {
      super(intoLayer);
    }

    /**
     * Configures the layer to include project dependencies. If no includes are
     * specified then all content is included. If includes are specified then content
     * must match an inclusion and not match any exclusions to be included.
     */
    public void includeProjectDependencies() {
      this.includeProjectDependencies = true;
    }

    /**
     * Configures the layer to exclude project dependencies. If no excludes a
     * specified no content is excluded. If exclusions are specified then any content
     * that matches an exclusion will be excluded irrespective of whether it matches
     * an include.
     */
    public void excludeProjectDependencies() {
      this.excludeProjectDependencies = true;
    }

    ContentSelector<Library> asLibrarySelector(Function<String, ContentFilter<Library>> filterFactory) {
      Layer layer = new Layer(getIntoLayer());
      List<ContentFilter<Library>> includeFilters = getIncludes().stream()
              .map(filterFactory)
              .collect(Collectors.toCollection(ArrayList::new));
      if (this.includeProjectDependencies) {
        includeFilters.add(Library::isLocal);
      }
      List<ContentFilter<Library>> excludeFilters = getExcludes().stream()
              .map(filterFactory)
              .collect(Collectors.toCollection(ArrayList::new));
      if (this.excludeProjectDependencies) {
        excludeFilters.add(Library::isLocal);
      }
      return new IncludeExcludeContentSelector<>(layer, includeFilters, excludeFilters);
    }

  }

  /**
   * An {@link IntoLayersSpec} that controls the layers to which application classes and
   * resources belong.
   */
  public static class ApplicationSpec extends IntoLayersSpec<IntoLayerSpec> {

    @Inject
    public ApplicationSpec() {
      super(new IntoLayerSpecFactory());
    }

    /**
     * Creates a new {@code ApplicationSpec} with the given {@code contents}.
     *
     * @param contents specs for the layers in which application content should be
     * included
     */
    public ApplicationSpec(IntoLayerSpec... contents) {
      super(new IntoLayerSpecFactory(), contents);
    }

    List<ContentSelector<String>> asSelectors() {
      return asSelectors((spec) -> spec.asSelector(ApplicationContentFilter::new));
    }

    private static final class IntoLayerSpecFactory implements Function<String, IntoLayerSpec>, Serializable {

      @Override
      public IntoLayerSpec apply(String layer) {
        return new IntoLayerSpec(layer);
      }

    }

  }

  /**
   * An {@link IntoLayersSpec} that controls the layers to which dependencies belong.
   */
  public static class DependenciesSpec extends IntoLayersSpec<DependenciesIntoLayerSpec> implements Serializable {

    @Inject
    public DependenciesSpec() {
      super(new IntoLayerSpecFactory());
    }

    /**
     * Creates a new {@code DependenciesSpec} with the given {@code contents}.
     *
     * @param contents specs for the layers in which dependencies should be included
     */
    public DependenciesSpec(DependenciesIntoLayerSpec... contents) {
      super(new IntoLayerSpecFactory(), contents);
    }

    List<ContentSelector<Library>> asSelectors() {
      return asSelectors(
              (spec) -> ((DependenciesIntoLayerSpec) spec).asLibrarySelector(LibraryContentFilter::new));
    }

    private static final class IntoLayerSpecFactory
            implements Function<String, DependenciesIntoLayerSpec>, Serializable {

      @Override
      public DependenciesIntoLayerSpec apply(String layer) {
        return new DependenciesIntoLayerSpec(layer);
      }

    }

  }

}
