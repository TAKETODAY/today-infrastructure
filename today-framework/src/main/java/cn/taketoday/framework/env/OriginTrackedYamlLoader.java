/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework.env;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.CollectionNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.core.YamlProcessor;
import cn.taketoday.core.io.Resource;
import cn.taketoday.origin.Origin;
import cn.taketoday.origin.OriginTrackedValue;
import cn.taketoday.origin.TextResourceOrigin;
import cn.taketoday.origin.TextResourceOrigin.Location;

/**
 * Class to load {@code .yml} files into a map of {@code String} to
 * {@link OriginTrackedValue}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 4.0
 */
class OriginTrackedYamlLoader extends YamlProcessor {

  private final Resource resource;

  OriginTrackedYamlLoader(Resource resource) {
    this.resource = resource;
    setResources(resource);
  }

  @Override
  protected Yaml createYaml() {
    LoaderOptions loaderOptions = new LoaderOptions();
    loaderOptions.setAllowDuplicateKeys(false);
    loaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
    loaderOptions.setAllowRecursiveKeys(true);
    return createYaml(loaderOptions);
  }

  private Yaml createYaml(LoaderOptions loaderOptions) {
    BaseConstructor constructor = new OriginTrackingConstructor(loaderOptions);
    Representer representer = new Representer();
    DumperOptions dumperOptions = new DumperOptions();
    LimitedResolver resolver = new LimitedResolver();
    return new Yaml(constructor, representer, dumperOptions, loaderOptions, resolver);
  }

  List<Map<String, Object>> load() {
    final List<Map<String, Object>> result = new ArrayList<>();
    process((properties, map) -> result.add(getFlattenedMap(map)));
    return result;
  }

  /**
   * {@link Constructor} that tracks property origins.
   */
  private class OriginTrackingConstructor extends SafeConstructor {

    OriginTrackingConstructor(LoaderOptions loadingConfig) {
      super(loadingConfig);
    }

    @Override
    public Object getData() throws NoSuchElementException {
      Object data = super.getData();
      if (data instanceof CharSequence && ((CharSequence) data).length() == 0) {
        return null;
      }
      return data;
    }

    @Override
    protected Object constructObject(Node node) {
      if (node instanceof CollectionNode && ((CollectionNode<?>) node).getValue().isEmpty()) {
        return constructTrackedObject(node, super.constructObject(node));
      }
      if (node instanceof ScalarNode) {
        if (!(node instanceof KeyScalarNode)) {
          return constructTrackedObject(node, super.constructObject(node));
        }
      }
      if (node instanceof MappingNode) {
        replaceMappingNodeKeys((MappingNode) node);
      }
      return super.constructObject(node);
    }

    private void replaceMappingNodeKeys(MappingNode node) {
      node.setValue(node.getValue().stream().map(KeyScalarNode::get).collect(Collectors.toList()));
    }

    private Object constructTrackedObject(Node node, Object value) {
      Origin origin = getOrigin(node);
      return OriginTrackedValue.of(getValue(value), origin);
    }

    private Object getValue(Object value) {
      return (value != null) ? value : "";
    }

    private Origin getOrigin(Node node) {
      Mark mark = node.getStartMark();
      Location location = new Location(mark.getLine(), mark.getColumn());
      return new TextResourceOrigin(OriginTrackedYamlLoader.this.resource, location);
    }

  }

  /**
   * {@link ScalarNode} that replaces the key node in a {@link NodeTuple}.
   */
  private static class KeyScalarNode extends ScalarNode {

    KeyScalarNode(ScalarNode node) {
      super(node.getTag(), node.getValue(), node.getStartMark(), node.getEndMark(), node.getScalarStyle());
    }

    static NodeTuple get(NodeTuple nodeTuple) {
      Node keyNode = nodeTuple.getKeyNode();
      Node valueNode = nodeTuple.getValueNode();
      return new NodeTuple(KeyScalarNode.get(keyNode), valueNode);
    }

    private static Node get(Node node) {
      if (node instanceof ScalarNode) {
        return new KeyScalarNode((ScalarNode) node);
      }
      return node;
    }

  }

  /**
   * {@link Resolver} that limits {@link Tag#TIMESTAMP} tags.
   */
  private static class LimitedResolver extends Resolver {

    @Override
    public void addImplicitResolver(Tag tag, Pattern regexp, String first) {
      if (tag == Tag.TIMESTAMP) {
        return;
      }
      super.addImplicitResolver(tag, regexp, first);
    }

  }

}
