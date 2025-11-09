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

package infra.web.view;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.http.HttpStatusCode;
import infra.ui.ModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 20:32
 */
class ModelAndViewTests {

  @Test
  void defaultConstructorCreatesEmptyModelAndView() {
    ModelAndView mav = new ModelAndView();

    assertThat(mav.getView()).isNull();
    assertThat(mav.getViewName()).isNull();
    assertThat(mav.getModel()).isNotNull();
    assertThat(mav.getModelMap()).isNotNull();
    assertThat(mav.getStatus()).isNull();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(mav.wasCleared()).isFalse();
    assertThat(mav.hasView()).isFalse();
    assertThat(mav.isReference()).isFalse();
  }

  @Test
  void constructorWithViewName() {
    String viewName = "testView";
    ModelAndView mav = new ModelAndView(viewName);

    assertThat(mav.getViewName()).isEqualTo(viewName);
    assertThat(mav.getView()).isNull();
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isTrue();
    assertThat(mav.isEmpty()).isFalse();
  }

  @Test
  void constructorWithView() {
    View view = mock(View.class);
    ModelAndView mav = new ModelAndView(view);

    assertThat(mav.getView()).isSameAs(view);
    assertThat(mav.getViewName()).isNull();
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isFalse();
    assertThat(mav.isEmpty()).isFalse();
  }

  @Test
  void constructorWithViewNameAndModel() {
    String viewName = "testView";
    Map<String, Object> model = Map.of("key1", "value1", "key2", "value2");
    ModelAndView mav = new ModelAndView(viewName, model);

    assertThat(mav.getViewName()).isEqualTo(viewName);
    assertThat(mav.getModel()).containsEntry("key1", "value1")
            .containsEntry("key2", "value2")
            .hasSize(2);
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isTrue();
  }

  @Test
  void constructorWithViewAndModel() {
    View view = mock(View.class);
    Map<String, Object> model = Map.of("key1", "value1", "key2", "value2");
    ModelAndView mav = new ModelAndView(view, model);

    assertThat(mav.getView()).isSameAs(view);
    assertThat(mav.getModel()).containsEntry("key1", "value1")
            .containsEntry("key2", "value2")
            .hasSize(2);
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isFalse();
  }

  @Test
  void constructorWithViewNameAndStatus() {
    String viewName = "testView";
    HttpStatusCode status = HttpStatusCode.valueOf(404);
    ModelAndView mav = new ModelAndView(viewName, status);

    assertThat(mav.getViewName()).isEqualTo(viewName);
    assertThat(mav.getStatus()).isEqualTo(status);
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isTrue();
  }

  @Test
  void constructorWithViewNameModelAndStatus() {
    String viewName = "testView";
    Map<String, Object> model = Map.of("key", "value");
    HttpStatusCode status = HttpStatusCode.valueOf(404);
    ModelAndView mav = new ModelAndView(viewName, model, status);

    assertThat(mav.getViewName()).isEqualTo(viewName);
    assertThat(mav.getModel()).containsEntry("key", "value");
    assertThat(mav.getStatus()).isEqualTo(status);
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isTrue();
  }

  @Test
  void constructorWithViewNameModelNameAndModelObject() {
    String viewName = "testView";
    String modelName = "testModel";
    Object modelObject = "testValue";
    ModelAndView mav = new ModelAndView(viewName, modelName, modelObject);

    assertThat(mav.getViewName()).isEqualTo(viewName);
    assertThat(mav.getModel()).containsEntry(modelName, modelObject);
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isTrue();
  }

  @Test
  void constructorWithViewModelNameAndModelObject() {
    View view = mock(View.class);
    String modelName = "testModel";
    Object modelObject = "testValue";
    ModelAndView mav = new ModelAndView(view, modelName, modelObject);

    assertThat(mav.getView()).isSameAs(view);
    assertThat(mav.getModel()).containsEntry(modelName, modelObject);
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isFalse();
  }

  @Test
  void setViewName() {
    ModelAndView mav = new ModelAndView();
    String viewName = "testView";

    mav.setViewName(viewName);

    assertThat(mav.getViewName()).isEqualTo(viewName);
    assertThat(mav.getView()).isNull();
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isTrue();
  }

  @Test
  void setView() {
    ModelAndView mav = new ModelAndView();
    View view = mock(View.class);

    mav.setView(view);

    assertThat(mav.getView()).isSameAs(view);
    assertThat(mav.getViewName()).isNull();
    assertThat(mav.hasView()).isTrue();
    assertThat(mav.isReference()).isFalse();
  }

  @Test
  void getModelMapCreatesNewModelMapIfNull() {
    ModelAndView mav = new ModelAndView();

    ModelMap modelMap1 = mav.getModelMap();
    ModelMap modelMap2 = mav.getModelMap();

    assertThat(modelMap1).isNotNull();
    assertThat(modelMap1).isSameAs(modelMap2);
  }

  @Test
  void setStatus() {
    ModelAndView mav = new ModelAndView();
    HttpStatusCode status = HttpStatusCode.valueOf(500);

    mav.setStatus(status);

    assertThat(mav.getStatus()).isEqualTo(status);
  }

  @Test
  void addObjectWithAttributeNameAndValue() {
    ModelAndView mav = new ModelAndView();

    ModelAndView result = mav.addObject("key", "value");

    assertThat(result).isSameAs(mav);
    assertThat(mav.getModel()).containsEntry("key", "value");
  }

  @Test
  void addObjectWithObject() {
    ModelAndView mav = new ModelAndView();
    Object modelObject = "testValue";

    ModelAndView result = mav.addObject(modelObject);

    assertThat(result).isSameAs(mav);
    assertThat(mav.getModel()).containsEntry("string", modelObject);
  }

  @Test
  void addAllObjects() {
    ModelAndView mav = new ModelAndView();
    Map<String, Object> modelMap = Map.of("key1", "value1", "key2", "value2");

    ModelAndView result = mav.addAllObjects(modelMap);

    assertThat(result).isSameAs(mav);
    assertThat(mav.getModel()).containsEntry("key1", "value1")
            .containsEntry("key2", "value2");
  }

  @Test
  void clear() {
    ModelAndView mav = new ModelAndView("testView");
    mav.addObject("key", "value");

    mav.clear();

    assertThat(mav.getView()).isNull();
    assertThat(mav.getModelMap()).isEmpty();
    assertThat(mav.isEmpty()).isTrue();
    assertThat(mav.wasCleared()).isTrue();
  }

  @Test
  void isEmptyReturnsTrueWhenNoViewAndNoModel() {
    ModelAndView mav = new ModelAndView();

    boolean empty = mav.isEmpty();

    assertThat(empty).isTrue();
  }

  @Test
  void isEmptyReturnsFalseWhenHasView() {
    ModelAndView mav = new ModelAndView("testView");

    boolean empty = mav.isEmpty();

    assertThat(empty).isFalse();
  }

  @Test
  void isEmptyReturnsFalseWhenHasModel() {
    ModelAndView mav = new ModelAndView();
    mav.addObject("key", "value");

    boolean empty = mav.isEmpty();

    assertThat(empty).isFalse();
  }

  @Test
  void wasClearedReturnsFalseWhenNotCleared() {
    ModelAndView mav = new ModelAndView();

    boolean wasCleared = mav.wasCleared();

    assertThat(wasCleared).isFalse();
  }

  @Test
  void wasClearedReturnsFalseWhenClearedButNotEmpty() {
    ModelAndView mav = new ModelAndView();
    mav.clear();
    // Manually add something after clearing
    mav.setViewName("testView");

    boolean wasCleared = mav.wasCleared();

    assertThat(wasCleared).isFalse();
  }

  @Test
  void toStringWithViewName() {
    ModelAndView mav = new ModelAndView("testView");

    String result = mav.toString();

    assertThat(result).contains("view=\"testView\"");
  }

  @Test
  void toStringWithView() {
    View view = mock(View.class);
    ModelAndView mav = new ModelAndView(view);

    String result = mav.toString();

    assertThat(result).contains("view=[" + view.toString() + "]");
  }

  @Test
  void equalsAndHashCode() {
    ModelAndView mav1 = new ModelAndView("testView");
    mav1.addObject("key", "value");
    mav1.setStatus(HttpStatusCode.valueOf(200));

    ModelAndView mav2 = new ModelAndView("testView");
    mav2.addObject("key", "value");
    mav2.setStatus(HttpStatusCode.valueOf(200));

    ModelAndView mav3 = new ModelAndView("otherView");

    assertThat(mav1).isEqualTo(mav2);
    assertThat(mav1).hasSameHashCodeAs(mav2);
    assertThat(mav1).isNotEqualTo(mav3);
    assertThat(mav1.hashCode()).isNotEqualTo(mav3.hashCode());
  }

}