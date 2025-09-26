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

package infra.web.view.xml;

import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import javax.xml.transform.stream.StreamResult;

import infra.lang.Assert;
import infra.oxm.Marshaller;
import infra.validation.BindingResult;
import infra.web.RequestContext;
import infra.web.view.AbstractView;
import infra.web.view.View;
import jakarta.xml.bind.JAXBElement;

/**
 * Web-MVC {@link View} that allows for response context to be rendered as the result
 * of marshalling by a {@link Marshaller}.
 *
 * <p>The Object to be marshalled is supplied as a parameter in the model and then
 * {@linkplain #locateToBeMarshalled(Map) detected} during response rendering. Users can
 * either specify a specific entry in the model via the {@link #setModelKey(String) sourceKey}
 * property or have Infra locate the Source object.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 4.0
 */
public class MarshallingView extends AbstractView {

  /**
   * Default content type. Overridable as bean property.
   */
  public static final String DEFAULT_CONTENT_TYPE = "application/xml";

  @Nullable
  private Marshaller marshaller;

  @Nullable
  private String modelKey;

  /**
   * Construct a new {@code MarshallingView} with no {@link Marshaller} set.
   * The marshaller must be set after construction by invoking {@link #setMarshaller}.
   */
  public MarshallingView() {
    setContentType(DEFAULT_CONTENT_TYPE);
    setExposePathVariables(false);
  }

  /**
   * Constructs a new {@code MarshallingView} with the given {@link Marshaller} set.
   */
  public MarshallingView(Marshaller marshaller) {
    this();
    Assert.notNull(marshaller, "Marshaller is required");
    this.marshaller = marshaller;
  }

  /**
   * Set the {@link Marshaller} to be used by this view.
   */
  public void setMarshaller(Marshaller marshaller) {
    this.marshaller = marshaller;
  }

  /**
   * Set the name of the model key that represents the object to be marshalled.
   * If not specified, the model map will be searched for a supported value type.
   *
   * @see Marshaller#supports(Class)
   */
  public void setModelKey(String modelKey) {
    this.modelKey = modelKey;
  }

  @Override
  protected void initApplicationContext() {
    Assert.state(this.marshaller != null, "Property 'marshaller' is required");
  }

  @Override
  protected void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {

    Object toBeMarshalled = locateToBeMarshalled(model);
    if (toBeMarshalled == null) {
      throw new IllegalStateException("Unable to locate object to be marshalled in model: " + model);
    }

    Assert.state(this.marshaller != null, "No Marshaller set");
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    this.marshaller.marshal(toBeMarshalled, new StreamResult(baos));

    setResponseContentType(request);
    request.setContentLength(baos.size());
    baos.writeTo(request.getOutputStream());
  }

  /**
   * Locate the object to be marshalled.
   * <p>The default implementation first attempts to look under the configured
   * {@linkplain #setModelKey(String) model key}, if any, before attempting to
   * locate an object of {@linkplain Marshaller#supports(Class) supported type}.
   *
   * @param model the model Map
   * @return the Object to be marshalled (or {@code null} if none found)
   * @throws IllegalStateException if the model object specified by the
   * {@linkplain #setModelKey(String) model key} is not supported by the marshaller
   * @see #setModelKey(String)
   */
  @Nullable
  protected Object locateToBeMarshalled(Map<String, Object> model) throws IllegalStateException {
    if (this.modelKey != null) {
      Object value = model.get(this.modelKey);
      if (value == null) {
        throw new IllegalStateException("Model contains no object with key [" + this.modelKey + "]");
      }
      if (!isEligibleForMarshalling(this.modelKey, value)) {
        throw new IllegalStateException("Model object [" + value + "] retrieved via key [" +
                this.modelKey + "] is not supported by the Marshaller");
      }
      return value;
    }
    for (Map.Entry<String, Object> entry : model.entrySet()) {
      Object value = entry.getValue();
      if (value != null && (model.size() == 1 || !(value instanceof BindingResult)) &&
              isEligibleForMarshalling(entry.getKey(), value)) {
        return value;
      }
    }
    return null;
  }

  /**
   * Check whether the given value from the current view's model is eligible
   * for marshalling through the configured {@link Marshaller}.
   * <p>The default implementation calls {@link Marshaller#supports(Class)},
   * unwrapping a given {@link JAXBElement} first if applicable.
   *
   * @param modelKey the value's key in the model (never {@code null})
   * @param value the value to check (never {@code null})
   * @return whether the given value is to be considered as eligible
   * @see Marshaller#supports(Class)
   */
  protected boolean isEligibleForMarshalling(String modelKey, Object value) {
    Assert.state(this.marshaller != null, "No Marshaller set");
    Class<?> classToCheck = value.getClass();
    if (value instanceof JAXBElement<?> jaxbElement) {
      classToCheck = jaxbElement.getDeclaredType();
    }
    return this.marshaller.supports(classToCheck);
  }

}
