/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.util.xml;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link TransformerUtils}.
 *
 * @author Rick Evans
 * @author Arjen Poutsma
 */
class TransformerUtilsTests {

  @Test
  void enableIndentingSunnyDay() throws Exception {
    Transformer transformer = new StubTransformer();
    TransformerUtils.enableIndenting(transformer);
    String indent = transformer.getOutputProperty(OutputKeys.INDENT);
    assertThat(indent).isNotNull();
    assertThat(indent).isEqualTo("yes");
    String indentAmount = transformer.getOutputProperty("{http://xml.apache.org/xalan}indent-amount");
    assertThat(indentAmount).isNotNull();
    assertThat(indentAmount).isEqualTo(String.valueOf(TransformerUtils.DEFAULT_INDENT_AMOUNT));
  }

  @Test
  void enableIndentingSunnyDayWithCustomKosherIndentAmount() throws Exception {
    final String indentAmountProperty = "10";
    Transformer transformer = new StubTransformer();
    TransformerUtils.enableIndenting(transformer, Integer.parseInt(indentAmountProperty));
    String indent = transformer.getOutputProperty(OutputKeys.INDENT);
    assertThat(indent).isNotNull();
    assertThat(indent).isEqualTo("yes");
    String indentAmount = transformer.getOutputProperty("{http://xml.apache.org/xalan}indent-amount");
    assertThat(indentAmount).isNotNull();
    assertThat(indentAmount).isEqualTo(indentAmountProperty);
  }

  @Test
  void disableIndentingSunnyDay() throws Exception {
    Transformer transformer = new StubTransformer();
    TransformerUtils.disableIndenting(transformer);
    String indent = transformer.getOutputProperty(OutputKeys.INDENT);
    assertThat(indent).isNotNull();
    assertThat(indent).isEqualTo("no");
  }

  @Test
  void enableIndentingWithNullTransformer() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            TransformerUtils.enableIndenting(null));
  }

  @Test
  void disableIndentingWithNullTransformer() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            TransformerUtils.disableIndenting(null));
  }

  @Test
  void enableIndentingWithNegativeIndentAmount() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            TransformerUtils.enableIndenting(new StubTransformer(), -21938));
  }

  @Test
  void enableIndentingWithZeroIndentAmount() throws Exception {
    TransformerUtils.enableIndenting(new StubTransformer(), 0);
  }

  private static class StubTransformer extends Transformer {

    private Properties outputProperties = new Properties();

    @Override
    public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setParameter(String name, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Object getParameter(String name) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void clearParameters() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setURIResolver(URIResolver resolver) {
      throw new UnsupportedOperationException();
    }

    @Override
    public URIResolver getURIResolver() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setOutputProperties(Properties oformat) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Properties getOutputProperties() {
      return this.outputProperties;
    }

    @Override
    public void setOutputProperty(String name, String value) throws IllegalArgumentException {
      this.outputProperties.setProperty(name, value);
    }

    @Override
    public String getOutputProperty(String name) throws IllegalArgumentException {
      return this.outputProperties.getProperty(name);
    }

    @Override
    public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
      throw new UnsupportedOperationException();
    }

    @Override
    public ErrorListener getErrorListener() {
      throw new UnsupportedOperationException();
    }
  }

}
