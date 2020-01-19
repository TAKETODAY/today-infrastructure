/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
 *
 * @author Kin-man Chung
 */

package cn.taketoday.expression.stream;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.PriorityQueue;

import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.LambdaExpression;
import cn.taketoday.expression.lang.ExpressionArithmetic;
import cn.taketoday.expression.lang.ExpressionSupport;

/**
 * @author TODAY <br>
 *         2019-02-20 16:48
 */
public class Stream {

    private final Operator op;
    private final Stream upstream;
    private final Iterator<Object> source;

    Stream(Iterator<Object> source) {
        this.op = null;
        this.source = source;
        this.upstream = null;
    }

    Stream(Stream upstream, Operator op) {
        this.op = op;
        this.source = null;
        this.upstream = upstream;
    }

    public Iterator<Object> iterator() {
        if (source != null) {
            return source;
        }
        return op.iterator(upstream.iterator());
    }

    public Stream filter(final LambdaExpression predicate) {
        return new Stream(this, (upstream) -> {
            return new Iterator2(upstream) {
                @Override
                public void doItem(Object item) {
                    if ((Boolean) predicate.invoke(item)) {
                        yield(item);
                    }
                }
            };
        });
    }

    public Stream map(final LambdaExpression mapper) {
        return new Stream(this, (up) -> {
            return new Iterator1(up) {
                @Override
                public Object next() {
                    return mapper.invoke(iter.next());
                }
            };
        });
    }

    public Stream peek(final LambdaExpression comsumer) {
        return new Stream(this, up -> {
            return new Iterator2(up) {
                @Override
                protected void doItem(Object item) {
                    comsumer.invoke(item);
                    yield(item);
                }
            };
        });
    }

    public Stream limit(final long n) {
        if (n < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        return new Stream(this, up -> {
            return new Iterator0() {
                long limit = n;

                @Override
                public boolean hasNext() {
                    return (limit > 0) ? up.hasNext() : false;
                }

                @Override
                public Object next() {
                    limit--;
                    return up.next();
                }
            };
        });
    }

    public Stream substream(final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("substream index must be non-negative");
        }
        return new Stream(this, new Operator() {
            long skip = startIndex;

            @Override
            public Iterator<Object> iterator(final Iterator<Object> up) {
                while (skip > 0 && up.hasNext()) {
                    up.next();
                    skip--;
                }
                return up;
            }
        });
    }

    public Stream substream(long startIndex, long endIndex) {
        return substream(startIndex).limit(endIndex - startIndex);
    }

    public Stream distinct() {
        return new Stream(this, new Operator() {
            @Override
            public Iterator<Object> iterator(final Iterator<Object> up) {
                return new Iterator2(up) {
                    private HashSet<Object> set = new HashSet<Object>();

                    @Override
                    public void doItem(Object item) {
                        if (set.add(item)) {
                            yield(item);
                        }
                    }
                };
            }
        });
    }

    private static final Comparator<Object> COMPARATOR = new Comparator<Object>() {
        @Override
        @SuppressWarnings("unchecked")
        public int compare(Object o1, Object o2) {
            return ((Comparable<Object>) o1).compareTo(o2);
        }
    };

    public Stream sorted() {
        return new Stream(this, new Operator() {

            private final PriorityQueue<Object> queue = new PriorityQueue<Object>(16, COMPARATOR);

            @Override
            public Iterator<Object> iterator(final Iterator<Object> up) {

                while (up.hasNext()) {
                    queue.add(up.next());
                }
                return new Iterator0() {
                    @Override
                    public boolean hasNext() {
                        return !queue.isEmpty();
                    }

                    @Override
                    public Object next() {
                        return queue.remove();
                    }
                };
            }
        });
    }

    public Stream sorted(final LambdaExpression comparator) {
        return new Stream(this, new Operator() {

            private PriorityQueue<Object> queue = null;

            @Override
            public Iterator<Object> iterator(final Iterator<Object> up) {
                if (queue == null) {
                    queue = new PriorityQueue<Object>(16, new Comparator<Object>() {
                        @Override
                        public int compare(Object o1, Object o2) {
                            return (Integer) ExpressionSupport.coerceToType(comparator.invoke(o1, o2), Integer.class);
                        }
                    });

                    while (up.hasNext()) {
                        queue.add(up.next());
                    }
                }

                return new Iterator0() {
                    @Override
                    public boolean hasNext() {
                        return !queue.isEmpty();
                    }

                    @Override
                    public Object next() {
                        return queue.remove();
                    }
                };
            }
        });
    }

    public Stream flatMap(final LambdaExpression mapper) {
        return new Stream(this, new Operator() {
            @Override
            public Iterator<Object> iterator(final Iterator<Object> upstream) {
                return new Iterator0() {
                    Iterator<Object> iter = null;

                    @Override
                    public boolean hasNext() {
                        while (true) {
                            if (iter == null) {
                                if (!upstream.hasNext()) {
                                    return false;
                                }
                                Object mapped = mapper.invoke(upstream.next());
                                if (!(mapped instanceof Stream)) {
                                    throw new ExpressionException("Expecting a Stream " + "from flatMap's mapper function.");
                                }
                                iter = ((Stream) mapped).iterator();
                            }
                            else {
                                if (iter.hasNext()) {
                                    return true;
                                }
                                iter = null;
                            }
                        }
                    }

                    @Override
                    public Object next() {
                        if (iter == null) {
                            return null;
                        }
                        return iter.next();
                    }
                };
            }
        });
    }

    public Object reduce(Object base, LambdaExpression op) {
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            base = op.invoke(base, iter.next());
        }
        return base;
    }

    public Optional<?> reduce(LambdaExpression op) {
        final Iterator<Object> iter = iterator();
        if (iter.hasNext()) {
            Object base = iter.next();
            while (iter.hasNext()) {
                base = op.invoke(base, iter.next());
            }
            return Optional.of(base);
        }
        return Optional.empty();
    }

    /*
     * public Map<Object,Object> reduceBy(LambdaExpression classifier,
     * LambdaExpression seed, LambdaExpression reducer) { Map<Object,Object> map =
     * new HashMap<Object,Object>(); Iterator<Object> iter = iterator(); while
     * (iter.hasNext()) { Object item = iter.next(); Object key =
     * classifier.invoke(item); Object value = map.get(key); if (value == null) {
     * value = seed.invoke(); } map.put(key, reducer.invoke(value, item)); } return
     * map; }
     */

    public void forEach(LambdaExpression comsumer) {
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            comsumer.invoke(iter.next());
        }
    }

    /*
     * public Map<Object,Collection<Object>> groupBy(LambdaExpression classifier) {
     * Map<Object, Collection<Object>> map = new HashMap<Object,
     * Collection<Object>>(); Iterator<Object> iter = iterator(); while
     * (iter.hasNext()) { Object item = iter.next(); Object key =
     * classifier.invoke(item); if (key == null) { throw new
     * ELException("null key"); } Collection<Object> c = map.get(key); if (c ==
     * null) { c = new ArrayList<Object>(); map.put(key, c); } c.add(item); } return
     * map; }
     */
    public boolean anyMatch(LambdaExpression predicate) {
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            if ((Boolean) predicate.invoke(iter.next())) {
                return true;
            }
        }
        return false;
    }

    public boolean allMatch(LambdaExpression predicate) {
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            if (!(Boolean) predicate.invoke(iter.next())) {
                return false;
            }
        }
        return true;
    }

    public boolean noneMatch(LambdaExpression predicate) {
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            if ((Boolean) predicate.invoke(iter.next())) {
                return false;
            }
        }
        return true;
    }

    public Object[] toArray() {
        final Iterator<Object> iter = iterator();
        final ArrayList<Object> al = new ArrayList<>();
        while (iter.hasNext()) {
            al.add(iter.next());
        }
        return al.toArray();
    }

    public Object toList() {
        final Iterator<Object> iter = iterator();
        final ArrayList<Object> al = new ArrayList<>();
        while (iter.hasNext()) {
            al.add(iter.next());
        }
        return al;
    }

    public Optional<?> findFirst() {
        final Iterator<Object> iter = iterator();
        if (iter.hasNext()) {
            return Optional.of(iter.next());
        }
        return Optional.empty();
    }

    public Object sum() {
        Number sum = Long.valueOf(0);
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            sum = ExpressionArithmetic.add(sum, iter.next());
        }
        return sum;
    }

    public Object count() {
        long count = 0;
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            count++;
            iter.next();
        }
        return Long.valueOf(count);
    }

    public Optional<?> min() {
        Object min = null;
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            if (min == null || ExpressionSupport.compare(min, item) > 0) {
                min = item;
            }
        }
        return ofNullable(min);
    }

    public Optional<?> max() {
        Object max = null;
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            if (max == null || ExpressionSupport.compare(max, item) < 0) {
                max = item;
            }
        }
        return ofNullable(max);
    }

    public Optional<?> min(final LambdaExpression comparator) {
        Object min = null;
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            Object item = iter.next();
            if (min == null || ExpressionSupport.compare(comparator.invoke(item, min), Long.valueOf(0)) < 0) {
                min = item;
            }
        }
        return ofNullable(min);
    }

    public Optional<?> max(final LambdaExpression comparator) {
        Object max = null;
        final Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            final Object item = iter.next();
            if (max == null || ExpressionSupport.compare(comparator.invoke(max, item), Long.valueOf(0)) < 0) {
                max = item;
            }
        }
        return ofNullable(max);
    }

    public Optional<?> average() {
        Number sum = Long.valueOf(0);
        long count = 0;
        Iterator<Object> iter = iterator();
        while (iter.hasNext()) {
            count++;
            sum = ExpressionArithmetic.add(sum, iter.next());
        }
        if (count == 0) {
            return Optional.empty();
        }
        return Optional.of(ExpressionArithmetic.divide(sum, count));
    }

    abstract class Iterator0 implements Iterator<Object> {
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    abstract class Iterator1 extends Iterator0 {

        protected final Iterator<Object> iter;

        protected Iterator1(Iterator<Object> iter) {
            this.iter = iter;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }
    }

    abstract class Iterator2 extends Iterator1 {
        private Object current;
        private boolean yielded;

        protected Iterator2(Iterator<Object> upstream) {
            super(upstream);
        }

        @Override
        public Object next() {
            yielded = false;
            return current;
        }

        @Override
        public boolean hasNext() {
            while ((!yielded) && iter.hasNext()) {
                doItem(iter.next());
            }
            return yielded;
        }

        void yield(Object current) {
            this.current = current;
            yielded = true;
        }

        protected abstract void doItem(Object item);
    }
}
