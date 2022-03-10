/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package cn.taketoday.context.el;

import java.util.ArrayList;
import java.util.List;

public class DataBase {

  private int curCustomer = 100;
  private int curProduct = 200;
  private int curOrder = 10;
  private boolean inited;

  private List<Customer> customers;
  private List<Product> products;
  private List<Order> orders;

  public List<Customer> getCustomers() {
    return this.customers;
  }

  public List<Product> getProducts() {
    return this.products;
  }

  public List<Order> getOrders() {
    return this.orders;
  }

  public void init() {
    if (inited) {
      return;
    }

    inited = true;
    customers = new ArrayList<Customer>();
    orders = new ArrayList<Order>();
    products = new ArrayList<Product>();
    initCustomer();
    initProduct();
    initOrder();
  }

  void initCustomer() {
    c("John Doe", "123 Willow Road", "Menlo Park", "USA",
            "650-734-2187");
    c("Mary Lane", "75 State Street", "Atlanta", "USA", "302-145-8765");
    c("Charlie Yeh", "5 Nathan Road", "Kowlon", "Hong Kong", "11-7565-2323");
  }

  void initProduct() {
    p("Eagle", "book", 12.50, 100); // id: 200
    p("Coming Home", "dvd", 8.00, 50); // id: 201
    p("Greatest Hits", "cd", 6.5, 200); // id: 202
    p("History of Golf", "book", 11.0, 30); // id: 203
    p("Toy Story", "dvd", 10.00, 1000); // id: 204
    p("iSee", "book", 12.50, 150); // 205
  }

  void initOrder() {
    o(100, new Date(2010, 2, 18), 20.80);
    o(100, new Date(2011, 5, 3), 34.50);
    o(100, new Date(2011, 8, 2), 210.75);
    o(101, new Date(2011, 1, 15), 50.23);
    o(101, new Date(2012, 1, 3), 126.77);
    o(102, new Date(2011, 4, 15), 101.20);
  }

  void c(String name, String address, String city,
          String country, String phone) {
    customers.add(new Customer(curCustomer++, name, address, city, country, phone));
  }

  void o(int customerID, Date orderDate, double total) {
    Order order = new Order(curOrder++, customerID, orderDate, total);
    this.orders.add(order);
    findCustomer(customerID).getOrders().add(order);
  }

  void p(String name, String category, double unitPrice, int unitsInStock) {
    products.add(new Product(curProduct++, name, category, unitPrice, unitsInStock));
  }

  private Customer findCustomer(int id) {
    for (Customer customer : customers) {
      if (customer.customerID == id) {
        return customer;
      }
    }
    return null;
  }
}
