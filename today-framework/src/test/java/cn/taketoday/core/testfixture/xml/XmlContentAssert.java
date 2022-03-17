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

package cn.taketoday.core.testfixture.xml;

import org.assertj.core.api.AbstractAssert;
import org.w3c.dom.Node;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.NodeMatcher;
import org.xmlunit.util.Predicate;

/**
 * Assertions exposed by {@link XmlContent}.
 *
 * @author Phillip Webb
 */
public class XmlContentAssert extends AbstractAssert<XmlContentAssert, Object> {

  XmlContentAssert(Object actual) {
    super(actual, XmlContentAssert.class);
  }

  public XmlContentAssert isSimilarTo(Object control) {
    XmlAssert.assertThat(super.actual).and(control).areSimilar();
    return this;
  }

  public XmlContentAssert isSimilarTo(Object control, Predicate<Node> nodeFilter) {
    XmlAssert.assertThat(super.actual).and(control).withNodeFilter(nodeFilter).areSimilar();
    return this;
  }

  public XmlContentAssert isSimilarTo(String control,
          DifferenceEvaluator differenceEvaluator) {
    XmlAssert.assertThat(super.actual).and(control).withDifferenceEvaluator(
            differenceEvaluator).areSimilar();
    return this;
  }

  public XmlContentAssert isSimilarToIgnoringWhitespace(Object control) {
    XmlAssert.assertThat(super.actual).and(control).ignoreWhitespace().areSimilar();
    return this;
  }

  public XmlContentAssert isSimilarToIgnoringWhitespace(String control, NodeMatcher nodeMatcher) {
    XmlAssert.assertThat(super.actual).and(control).ignoreWhitespace().withNodeMatcher(nodeMatcher).areSimilar();
    return this;
  }

}
