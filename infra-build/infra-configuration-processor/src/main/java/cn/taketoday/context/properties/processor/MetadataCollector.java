/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.properties.processor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import cn.taketoday.context.properties.processor.metadata.ConfigurationMetadata;
import cn.taketoday.context.properties.processor.metadata.ItemMetadata;

/**
 * Used by {@link ConfigurationMetadataAnnotationProcessor} to collect
 * {@link ConfigurationMetadata}.
 *
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MetadataCollector {

  private final Set<ItemMetadata> metadataItems = new LinkedHashSet<>();

  private final ProcessingEnvironment processingEnvironment;

  private final ConfigurationMetadata previousMetadata;

  private final TypeUtils typeUtils;

  private final Set<String> processedSourceTypes = new HashSet<>();

  /**
   * Creates a new {@code MetadataProcessor} instance.
   *
   * @param processingEnvironment the processing environment of the build
   * @param previousMetadata any previous metadata or {@code null}
   */
  public MetadataCollector(ProcessingEnvironment processingEnvironment, ConfigurationMetadata previousMetadata) {
    this.processingEnvironment = processingEnvironment;
    this.previousMetadata = previousMetadata;
    this.typeUtils = new TypeUtils(processingEnvironment);
  }

  public void processing(RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getRootElements()) {
      markAsProcessed(element);
    }
  }

  private void markAsProcessed(Element element) {
    if (element instanceof TypeElement) {
      this.processedSourceTypes.add(this.typeUtils.getQualifiedName(element));
    }
  }

  public void add(ItemMetadata metadata) {
    this.metadataItems.add(metadata);
  }

  public boolean hasSimilarGroup(ItemMetadata metadata) {
    if (!metadata.isOfItemType(ItemMetadata.ItemType.GROUP)) {
      throw new IllegalStateException("item " + metadata + " must be a group");
    }
    for (ItemMetadata existing : this.metadataItems) {
      if (existing.isOfItemType(ItemMetadata.ItemType.GROUP) && existing.getName().equals(metadata.getName())
              && existing.getType().equals(metadata.getType())) {
        return true;
      }
    }
    return false;
  }

  public ConfigurationMetadata getMetadata() {
    ConfigurationMetadata metadata = new ConfigurationMetadata();
    for (ItemMetadata item : this.metadataItems) {
      metadata.add(item);
    }
    if (this.previousMetadata != null) {
      List<ItemMetadata> items = this.previousMetadata.getItems();
      for (ItemMetadata item : items) {
        if (shouldBeMerged(item)) {
          metadata.addIfMissing(item);
        }
      }
    }
    return metadata;
  }

  private boolean shouldBeMerged(ItemMetadata itemMetadata) {
    String sourceType = itemMetadata.getSourceType();
    return (sourceType != null && !deletedInCurrentBuild(sourceType) && !processedInCurrentBuild(sourceType));
  }

  private boolean deletedInCurrentBuild(String sourceType) {
    return this.processingEnvironment.getElementUtils().getTypeElement(sourceType.replace('$', '.')) == null;
  }

  private boolean processedInCurrentBuild(String sourceType) {
    return this.processedSourceTypes.contains(sourceType);
  }

}
