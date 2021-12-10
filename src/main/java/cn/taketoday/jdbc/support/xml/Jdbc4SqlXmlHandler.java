/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.support.xml;

import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.lang.Nullable;

/**
 * Default implementation of the {@link SqlXmlHandler} interface.
 * Provides database-specific implementations for storing and
 * retrieving XML documents to and from fields in a database,
 * relying on the JDBC 4.0 {@code java.sql.SQLXML} facility.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @see SQLXML
 * @see ResultSet#getSQLXML
 * @see PreparedStatement#setSQLXML
 * @since 4.0
 */
public class Jdbc4SqlXmlHandler implements SqlXmlHandler {

  //-------------------------------------------------------------------------
  // Convenience methods for accessing XML content
  //-------------------------------------------------------------------------

  @Override
  @Nullable
  public String getXmlAsString(ResultSet rs, String columnName) throws SQLException {
    SQLXML xmlObject = rs.getSQLXML(columnName);
    return (xmlObject != null ? xmlObject.getString() : null);
  }

  @Override
  @Nullable
  public String getXmlAsString(ResultSet rs, int columnIndex) throws SQLException {
    SQLXML xmlObject = rs.getSQLXML(columnIndex);
    return (xmlObject != null ? xmlObject.getString() : null);
  }

  @Override
  @Nullable
  public InputStream getXmlAsBinaryStream(ResultSet rs, String columnName) throws SQLException {
    SQLXML xmlObject = rs.getSQLXML(columnName);
    return (xmlObject != null ? xmlObject.getBinaryStream() : null);
  }

  @Override
  @Nullable
  public InputStream getXmlAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
    SQLXML xmlObject = rs.getSQLXML(columnIndex);
    return (xmlObject != null ? xmlObject.getBinaryStream() : null);
  }

  @Override
  @Nullable
  public Reader getXmlAsCharacterStream(ResultSet rs, String columnName) throws SQLException {
    SQLXML xmlObject = rs.getSQLXML(columnName);
    return (xmlObject != null ? xmlObject.getCharacterStream() : null);
  }

  @Override
  @Nullable
  public Reader getXmlAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
    SQLXML xmlObject = rs.getSQLXML(columnIndex);
    return (xmlObject != null ? xmlObject.getCharacterStream() : null);
  }

  @Override
  @Nullable
  public Source getXmlAsSource(ResultSet rs, String columnName, @Nullable Class<? extends Source> sourceClass)
          throws SQLException {

    SQLXML xmlObject = rs.getSQLXML(columnName);
    if (xmlObject == null) {
      return null;
    }
    return (sourceClass != null ? xmlObject.getSource(sourceClass) : xmlObject.getSource(DOMSource.class));
  }

  @Override
  @Nullable
  public Source getXmlAsSource(ResultSet rs, int columnIndex, @Nullable Class<? extends Source> sourceClass)
          throws SQLException {

    SQLXML xmlObject = rs.getSQLXML(columnIndex);
    if (xmlObject == null) {
      return null;
    }
    return (sourceClass != null ? xmlObject.getSource(sourceClass) : xmlObject.getSource(DOMSource.class));
  }

  //-------------------------------------------------------------------------
  // Convenience methods for building XML content
  //-------------------------------------------------------------------------

  @Override
  public SqlXmlValue newSqlXmlValue(final String value) {
    return new AbstractJdbc4SqlXmlValue() {
      @Override
      protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
        xmlObject.setString(value);
      }
    };
  }

  @Override
  public SqlXmlValue newSqlXmlValue(final XmlBinaryStreamProvider provider) {
    return new AbstractJdbc4SqlXmlValue() {
      @Override
      protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
        provider.provideXml(xmlObject.setBinaryStream());
      }
    };
  }

  @Override
  public SqlXmlValue newSqlXmlValue(final XmlCharacterStreamProvider provider) {
    return new AbstractJdbc4SqlXmlValue() {
      @Override
      protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
        provider.provideXml(xmlObject.setCharacterStream());
      }
    };
  }

  @Override
  public SqlXmlValue newSqlXmlValue(final Class<? extends Result> resultClass, final XmlResultProvider provider) {
    return new AbstractJdbc4SqlXmlValue() {
      @Override
      protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
        provider.provideXml(xmlObject.setResult(resultClass));
      }
    };
  }

  @Override
  public SqlXmlValue newSqlXmlValue(final Document document) {
    return new AbstractJdbc4SqlXmlValue() {
      @Override
      protected void provideXml(SQLXML xmlObject) throws SQLException, IOException {
        xmlObject.setResult(DOMResult.class).setNode(document);
      }
    };
  }

  /**
   * Internal base class for {@link SqlXmlValue} implementations.
   */
  private abstract static class AbstractJdbc4SqlXmlValue implements SqlXmlValue {

    @Nullable
    private SQLXML xmlObject;

    @Override
    public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
      this.xmlObject = ps.getConnection().createSQLXML();
      try {
        provideXml(this.xmlObject);
      }
      catch (IOException ex) {
        throw new DataAccessResourceFailureException("Failure encountered while providing XML", ex);
      }
      ps.setSQLXML(paramIndex, this.xmlObject);
    }

    @Override
    public void cleanup() {
      if (this.xmlObject != null) {
        try {
          this.xmlObject.free();
        }
        catch (SQLException ex) {
          throw new DataAccessResourceFailureException("Could not free SQLXML object", ex);
        }
      }
    }

    protected abstract void provideXml(SQLXML xmlObject) throws SQLException, IOException;
  }

}
