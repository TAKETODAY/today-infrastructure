<<<<<<< HEAD
/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * @author Today
 * @date 2018年6月30日 下午6:21:37
 */
public final class ParamList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

	private static final long			serialVersionUID	= 8673264195747942595L;

	private transient volatile Object[]	array;


	public ParamList() {
		array = new Object[0];
	}

	public int size() {
		return array.length;
	}

	/**
	 * Returns {@code true} if this list contains no elements.
	 * 
	 * @return {@code true} if this list contains no elements
	 */
	public boolean isEmpty() {
		return array.length == 0;
	}

	/**
	 * Tests for equality, coping with nulls.
	 */
	private static boolean eq(Object o1, Object o2) {
		return (o1 == null) ? o2 == null : o1.equals(o2);
	}

	/**
	 * static version of indexOf, to allow repeated calls without needing to
	 * re-acquire array each time.
	 * 
	 * @param o
	 *            element to search for
	 * @param elements
	 *            the array
	 * @param index
	 *            first index to search
	 * @param fence
	 *            one past last index to search
	 * @return index of element, or -1 if absent
	 */
	private static int indexOf(Object o, Object[] elements, int index, int fence) {
		if (o == null) {
			for (int i = index; i < fence; i++)
				if (elements[i] == null)
					return i;
		} else {
			for (int i = index; i < fence; i++)
				if (o.equals(elements[i]))
					return i;
		}
		return -1;
	}

	/**
	 * static version of lastIndexOf.
	 * 
	 * @param o
	 *            element to search for
	 * @param elements
	 *            the array
	 * @param index
	 *            first index to search
	 * @return index of element, or -1 if absent
	 */
	private static int lastIndexOf(Object o, Object[] elements, int index) {
		if (o == null) {
			for (int i = index; i >= 0; i--)
				if (elements[i] == null)
					return i;
		} else {
			for (int i = index; i >= 0; i--)
				if (o.equals(elements[i]))
					return i;
		}
		return -1;
	}

	/**
	 * Returns {@code true} if this list contains the specified element. More
	 * formally, returns {@code true} if and only if this list contains at least one
	 * element {@code e} such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	 *
	 * @param o
	 *            element whose presence in this list is to be tested
	 * @return {@code true} if this list contains the specified element
	 */
	public boolean contains(Object o) {
		return indexOf(o, array, 0, array.length) >= 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public int indexOf(Object o) {
		return indexOf(o, array, 0, array.length);
	}

	/**
	 * Returns the index of the first occurrence of the specified element in this
	 * list, searching forwards from {@code index}, or returns -1 if the element is
	 * not found. More formally, returns the lowest index {@code i} such that
	 * <tt>(i&nbsp;&gt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(e==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;e.equals(get(i))))</tt>,
	 * or -1 if there is no such index.
	 *
	 * @param e
	 *            element to search for
	 * @param index
	 *            index to start searching from
	 * @return the index of the first occurrence of the element in this list at
	 *         position {@code index} or later in the list; {@code -1} if the
	 *         element is not found.
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public int indexOf(E e, int index) {
		return indexOf(e, array, index, array.length);
	}

	/**
	 * {@inheritDoc}
	 */
	public int lastIndexOf(Object o) {
		return lastIndexOf(o, array, array.length - 1);
	}

	/**
	 * Returns the index of the last occurrence of the specified element in this
	 * list, searching backwards from {@code index}, or returns -1 if the element is
	 * not found. More formally, returns the highest index {@code i} such that
	 * <tt>(i&nbsp;&lt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(e==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;e.equals(get(i))))</tt>,
	 * or -1 if there is no such index.
	 *
	 * @param e
	 *            element to search for
	 * @param index
	 *            index to start searching backwards from
	 * @return the index of the last occurrence of the element at position less than
	 *         or equal to {@code index} in this list; -1 if the element is not
	 *         found.
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is greater than or equal to the current
	 *             size of this list
	 */
	public int lastIndexOf(E e, int index) {
		return lastIndexOf(e, array, index);
	}

	/**
	 * Returns a shallow copy of this list. (The elements themselves are not
	 * copied.)
	 *
	 * @return a clone of this list
	 */
	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			return (ParamList<E>) super.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
	}

	public Object[] toArray() {
		return array;
	}

	@SuppressWarnings("unchecked")
	public E get(int index) {
		return index > array.length - 1 ? null : (E) array[index];
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element.
	 *
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public E set(int index, E element) {

		if (index <= array.length - 1) {
			array[index] = element;
			return element;
		}

		Object[] newArray = new Object[index + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[index] = element;

		array = newArray;

		return element;
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e
	 *            element to be appended to this list
	 * @return {@code true} (as specified by {@link Collection#add})
	 */
	public boolean add(E e) {
		Object[] newArray = new Object[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = e;
		array = newArray;

		return true;
	}

	public void add(int index, E element) {
		this.set(index, element);
	}

	public E remove(int index) {
		Object[] elements = array;
		E oldValue = get(index);
		int numMoved = array.length - index - 1;
		if (numMoved == 0)
			array = Arrays.copyOf(elements, array.length - 1);
		else {
			Object[] newElements = new Object[array.length - 1];
			System.arraycopy(elements, 0, newElements, 0, index);
			System.arraycopy(elements, index + 1, newElements, index, numMoved);
			array = newElements;
		}
		return oldValue;
	}

	public E remove_(int index) {
		Object[] elements = array;
		E oldValue = get(index);
		int numMoved = array.length - index - 1;
		if (numMoved == 0)
			array = Arrays.copyOf(elements, array.length - 1);
		else {
			Object[] newElements = new Object[array.length - 1];
			System.arraycopy(elements, 0, newElements, 0, index);
			System.arraycopy(elements, index + 1, newElements, index, numMoved);
			array = newElements;
		}
		return oldValue;
	}

	public boolean remove(Object o) {
		Object[] snapshot = array;
		int index = indexOf(o, snapshot, 0, snapshot.length);
		return (index < 0) ? false : remove(o, snapshot, index);
	}

	private boolean remove(Object o, Object[] snapshot, int index) {

		if (snapshot != array)
			findIndex: {
				int prefix = Math.min(index, array.length);
				for (int i = 0; i < prefix; i++) {
					if (array[i] != snapshot[i] && eq(o, array[i])) {
						index = i;
						break findIndex;
					}
				}
				if (index >= array.length)
					return false;
				if (array[index] == o)
					break findIndex;
				index = indexOf(o, array, index, array.length);
				if (index < 0)
					return false;
			}

		Object[] newElements = new Object[array.length - 1];
		System.arraycopy(array, 0, newElements, 0, index);
		System.arraycopy(array, index + 1, newElements, index, array.length - index - 1);
		array = newElements;
		return true;
	}

	/**
	 * Returns {@code true} if this list contains all of the elements of the
	 * specified collection.
	 *
	 * @param c
	 *            collection to be checked for containment in this list
	 * @return {@code true} if this list contains all of the elements of the
	 *         specified collection
	 * @throws NullPointerException
	 *             if the specified collection is null
	 * @see #contains(Object)
	 */
	public boolean containsAll(Collection<?> c) {
		for (Object e : c) {
			if (indexOf(e, array, 0, array.length) < 0)
				return false;
		}
		return true;
	}

	public void clear() {
		array = new Object[0];
	}

	public boolean addAll(Collection<? extends E> c) {
		Object[] cs = (c.getClass() == ParamList.class) ? ((ParamList<?>) c).array : c.toArray();
		if (cs.length == 0)
			return false;
		if (array.length == 0 && cs.getClass() == Object[].class)
			array = cs;
		else {
			Object[] newElements = Arrays.copyOf(array, array.length + cs.length);
			System.arraycopy(cs, 0, newElements, array.length, cs.length);
			array = newElements;
		}
		return true;
	}

	/**
	 * Inserts all of the elements in the specified collection into this list,
	 * starting at the specified position. Shifts the element currently at that
	 * position (if any) and any subsequent elements to the right (increases their
	 * indices). The new elements will appear in this list in the order that they
	 * are returned by the specified collection's iterator.
	 *
	 * @param index
	 *            index at which to insert the first element from the specified
	 *            collection
	 * @param c
	 *            collection containing elements to be added to this list
	 * @return {@code true} if this list changed as a result of the call
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified collection is null
	 * @see #add(int,Object)
	 */
	public boolean addAll(int index, Collection<? extends E> c) {
		Object[] cs = c.toArray();
		if (index > array.length || index < 0)
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + array.length);
		if (cs.length == 0)
			return false;
		int numMoved = array.length - index;
		Object[] newElements;
		if (numMoved == 0)
			newElements = Arrays.copyOf(array, array.length + cs.length);
		else {
			newElements = new Object[array.length + cs.length];
			System.arraycopy(array, 0, newElements, 0, index);
			System.arraycopy(array, index, newElements, index + cs.length, numMoved);
		}
		System.arraycopy(cs, 0, newElements, index, cs.length);
		array = newElements;
		return true;
	}

	@SuppressWarnings("unchecked")
	public void forEach(Consumer<? super E> action) {
		if (action == null)
			throw new NullPointerException();

		for (int i = 0; i < array.length; ++i) {
			E e = (E) array[i];
			action.accept(e);
		}
	}

	public boolean removeIf(Predicate<? super E> filter) {
		if (filter == null)
			throw new NullPointerException();
		if (array.length != 0) {
			int newlen = 0;
			Object[] temp = new Object[array.length];
			for (int i = 0; i < array.length; ++i) {
				@SuppressWarnings("unchecked")
				E e = (E) array[i];
				if (!filter.test(e))
					temp[newlen++] = e;
			}
			if (newlen != array.length) {
				array = Arrays.copyOf(temp, newlen);
				return true;
			}
		}
		return false;
	}

	public void replaceAll(UnaryOperator<E> operator) {
		if (operator == null)
			throw new NullPointerException();
		Object[] newElements = Arrays.copyOf(array, array.length);
		for (int i = 0; i < array.length; ++i) {
			@SuppressWarnings("unchecked")
			E e = (E) array[i];
			newElements[i] = operator.apply(e);
		}
		array = newElements;
	}

	@SuppressWarnings("unchecked")
	public void sort(Comparator<? super E> c) {
		Object[] newElements = Arrays.copyOf(array, array.length);
		E[] es = (E[]) newElements;
		Arrays.sort(es, c);
		array = newElements;
	}

	/**
	 * Returns a string representation of this list. The string representation
	 * consists of the string representations of the list's elements in the order
	 * they are returned by its iterator, enclosed in square brackets
	 * ({@code "[]"}). Adjacent elements are separated by the characters
	 * {@code ", "} (comma and space). Elements are converted to strings as by
	 * {@link String#valueOf(Object)}.
	 *
	 * @return a string representation of this list
	 */
	public String toString() {
		return Arrays.toString(array);
	}

	/**
	 * Compares the specified object with this list for equality. Returns
	 * {@code true} if the specified object is the same object as this object, or if
	 * it is also a {@link List} and the sequence of elements returned by an
	 * {@linkplain List#iterator() iterator} over the specified list is the same as
	 * the sequence returned by an iterator over this list. The two sequences are
	 * considered to be the same if they have the same length and corresponding
	 * elements at the same position in the sequence are <em>equal</em>. Two
	 * elements {@code e1} and {@code e2} are considered <em>equal</em> if
	 * {@code (e1==null ? e2==null : e1.equals(e2))}.
	 *
	 * @param o
	 *            the object to be compared for equality with this list
	 * @return {@code true} if the specified object is equal to this list
	 */
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof List))
			return false;

		List<?> list = (List<?>) (o);
		Iterator<?> it = list.iterator();
		for (int i = 0; i < array.length; ++i)
			if (!it.hasNext() || !eq(array[i], it.next()))
				return false;
		if (it.hasNext())
			return false;
		return true;
	}

	/**
	 * Returns the hash code value for this list.
	 *
	 * <p>
	 * This implementation uses the definition in {@link List#hashCode}.
	 *
	 * @return the hash code value for this list
	 */
	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < array.length; ++i) {
			Object obj = array[i];
			hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
		}
		return hashCode;
	}

	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 *
	 * <p>
	 * The returned iterator provides a snapshot of the state of the list when the
	 * iterator was constructed. No synchronization is needed while traversing the
	 * iterator. The iterator does <em>NOT</em> support the {@code remove} method.
	 *
	 * @return an iterator over the elements in this list in proper sequence
	 */
	public Iterator<E> iterator() {
		return new COWIterator<E>(array, 0);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The returned iterator provides a snapshot of the state of the list when the
	 * iterator was constructed. No synchronization is needed while traversing the
	 * iterator. The iterator does <em>NOT</em> support the {@code remove},
	 * {@code set} or {@code add} methods.
	 */
	public ListIterator<E> listIterator() {
		return new COWIterator<E>(array, 0);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The returned iterator provides a snapshot of the state of the list when the
	 * iterator was constructed. No synchronization is needed while traversing the
	 * iterator. The iterator does <em>NOT</em> support the {@code remove},
	 * {@code set} or {@code add} methods.
	 *
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public ListIterator<E> listIterator(int index) {
		if (index < 0 || index > array.length)
			throw new IndexOutOfBoundsException("Index: " + index);
		return new COWIterator<E>(array, index);
	}

	public Spliterator<E> spliterator() {
		return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
	}

	static final class COWIterator<E> implements ListIterator<E> {
		/** Snapshot of the array */
		private final Object[]	snapshot;
		/** Index of element to be returned by subsequent call to next. */
		private int				cursor;

		private COWIterator(Object[] elements, int initialCursor) {
			cursor = initialCursor;
			snapshot = elements;
		}

		public final boolean hasNext() {
			return cursor < snapshot.length;
		}

		public final boolean hasPrevious() {
			return cursor > 0;
		}

		@SuppressWarnings("unchecked")
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			return (E) snapshot[cursor++];
		}

		@SuppressWarnings("unchecked")
		public E previous() {
			if (!hasPrevious())
				throw new NoSuchElementException();
			return (E) snapshot[--cursor];
		}

		public int nextIndex() {
			return cursor;
		}

		public int previousIndex() {
			return cursor - 1;
		}

		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * 
		 * @throws UnsupportedOperationException
		 *             always; {@code remove} is not supported by this iterator.
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * 
		 * @throws UnsupportedOperationException
		 *             always; {@code set} is not supported by this iterator.
		 */
		public void set(E e) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * 
		 * @throws UnsupportedOperationException
		 *             always; {@code add} is not supported by this iterator.
		 */
		public void add(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			Objects.requireNonNull(action);
			Object[] elements = snapshot;
			final int size = elements.length;
			for (int i = cursor; i < size; i++) {
				@SuppressWarnings("unchecked")
				E e = (E) elements[i];
				action.accept(e);
			}
			cursor = size;
		}
	}

	public List<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();// The current operation is not supported yet
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException(); // The current operation is not supported yet
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();// The current operation is not supported yet
	}

	public int addAllAbsent(Collection<? extends E> c) {
		throw new UnsupportedOperationException(); // The current operation is not supported yet
	}

	public <T> T[] toArray(T a[]) {
		throw new UnsupportedOperationException(); // The current operation is not supported yet
	}

}
=======
/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * @author Today
 * @date 2018年6月30日 下午6:21:37
 */
public final class ParamList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable {

	private static final long			serialVersionUID	= 8673264195747942595L;

	private transient volatile Object[]	array;


	public ParamList() {
		array = new Object[0];
	}

	public int size() {
		return array.length;
	}

	/**
	 * Returns {@code true} if this list contains no elements.
	 * 
	 * @return {@code true} if this list contains no elements
	 */
	public boolean isEmpty() {
		return array.length == 0;
	}

	/**
	 * Tests for equality, coping with nulls.
	 */
	private static boolean eq(Object o1, Object o2) {
		return (o1 == null) ? o2 == null : o1.equals(o2);
	}

	/**
	 * static version of indexOf, to allow repeated calls without needing to
	 * re-acquire array each time.
	 * 
	 * @param o
	 *            element to search for
	 * @param elements
	 *            the array
	 * @param index
	 *            first index to search
	 * @param fence
	 *            one past last index to search
	 * @return index of element, or -1 if absent
	 */
	private static int indexOf(Object o, Object[] elements, int index, int fence) {
		if (o == null) {
			for (int i = index; i < fence; i++)
				if (elements[i] == null)
					return i;
		} else {
			for (int i = index; i < fence; i++)
				if (o.equals(elements[i]))
					return i;
		}
		return -1;
	}

	/**
	 * static version of lastIndexOf.
	 * 
	 * @param o
	 *            element to search for
	 * @param elements
	 *            the array
	 * @param index
	 *            first index to search
	 * @return index of element, or -1 if absent
	 */
	private static int lastIndexOf(Object o, Object[] elements, int index) {
		if (o == null) {
			for (int i = index; i >= 0; i--)
				if (elements[i] == null)
					return i;
		} else {
			for (int i = index; i >= 0; i--)
				if (o.equals(elements[i]))
					return i;
		}
		return -1;
	}

	/**
	 * Returns {@code true} if this list contains the specified element. More
	 * formally, returns {@code true} if and only if this list contains at least one
	 * element {@code e} such that
	 * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	 *
	 * @param o
	 *            element whose presence in this list is to be tested
	 * @return {@code true} if this list contains the specified element
	 */
	public boolean contains(Object o) {
		return indexOf(o, array, 0, array.length) >= 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public int indexOf(Object o) {
		return indexOf(o, array, 0, array.length);
	}

	/**
	 * Returns the index of the first occurrence of the specified element in this
	 * list, searching forwards from {@code index}, or returns -1 if the element is
	 * not found. More formally, returns the lowest index {@code i} such that
	 * <tt>(i&nbsp;&gt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(e==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;e.equals(get(i))))</tt>,
	 * or -1 if there is no such index.
	 *
	 * @param e
	 *            element to search for
	 * @param index
	 *            index to start searching from
	 * @return the index of the first occurrence of the element in this list at
	 *         position {@code index} or later in the list; {@code -1} if the
	 *         element is not found.
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is negative
	 */
	public int indexOf(E e, int index) {
		return indexOf(e, array, index, array.length);
	}

	/**
	 * {@inheritDoc}
	 */
	public int lastIndexOf(Object o) {
		return lastIndexOf(o, array, array.length - 1);
	}

	/**
	 * Returns the index of the last occurrence of the specified element in this
	 * list, searching backwards from {@code index}, or returns -1 if the element is
	 * not found. More formally, returns the highest index {@code i} such that
	 * <tt>(i&nbsp;&lt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(e==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;e.equals(get(i))))</tt>,
	 * or -1 if there is no such index.
	 *
	 * @param e
	 *            element to search for
	 * @param index
	 *            index to start searching backwards from
	 * @return the index of the last occurrence of the element at position less than
	 *         or equal to {@code index} in this list; -1 if the element is not
	 *         found.
	 * @throws IndexOutOfBoundsException
	 *             if the specified index is greater than or equal to the current
	 *             size of this list
	 */
	public int lastIndexOf(E e, int index) {
		return lastIndexOf(e, array, index);
	}

	/**
	 * Returns a shallow copy of this list. (The elements themselves are not
	 * copied.)
	 *
	 * @return a clone of this list
	 */
	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			return (ParamList<E>) super.clone();
		} catch (CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
	}

	public Object[] toArray() {
		return array;
	}

	@SuppressWarnings("unchecked")
	public E get(int index) {
		return index > array.length - 1 ? null : (E) array[index];
	}

	/**
	 * Replaces the element at the specified position in this list with the
	 * specified element.
	 *
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public E set(int index, E element) {

		if (index <= array.length - 1) {
			array[index] = element;
			return element;
		}

		Object[] newArray = new Object[index + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[index] = element;

		array = newArray;

		return element;
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e
	 *            element to be appended to this list
	 * @return {@code true} (as specified by {@link Collection#add})
	 */
	public boolean add(E e) {
		Object[] newArray = new Object[array.length + 1];
		System.arraycopy(array, 0, newArray, 0, array.length);
		newArray[array.length] = e;
		array = newArray;

		return true;
	}

	public void add(int index, E element) {
		this.set(index, element);
	}

	public E remove(int index) {
		Object[] elements = array;
		E oldValue = get(index);
		int numMoved = array.length - index - 1;
		if (numMoved == 0)
			array = Arrays.copyOf(elements, array.length - 1);
		else {
			Object[] newElements = new Object[array.length - 1];
			System.arraycopy(elements, 0, newElements, 0, index);
			System.arraycopy(elements, index + 1, newElements, index, numMoved);
			array = newElements;
		}
		return oldValue;
	}

	public E remove_(int index) {
		Object[] elements = array;
		E oldValue = get(index);
		int numMoved = array.length - index - 1;
		if (numMoved == 0)
			array = Arrays.copyOf(elements, array.length - 1);
		else {
			Object[] newElements = new Object[array.length - 1];
			System.arraycopy(elements, 0, newElements, 0, index);
			System.arraycopy(elements, index + 1, newElements, index, numMoved);
			array = newElements;
		}
		return oldValue;
	}

	public boolean remove(Object o) {
		Object[] snapshot = array;
		int index = indexOf(o, snapshot, 0, snapshot.length);
		return (index < 0) ? false : remove(o, snapshot, index);
	}

	private boolean remove(Object o, Object[] snapshot, int index) {

		if (snapshot != array)
			findIndex: {
				int prefix = Math.min(index, array.length);
				for (int i = 0; i < prefix; i++) {
					if (array[i] != snapshot[i] && eq(o, array[i])) {
						index = i;
						break findIndex;
					}
				}
				if (index >= array.length)
					return false;
				if (array[index] == o)
					break findIndex;
				index = indexOf(o, array, index, array.length);
				if (index < 0)
					return false;
			}

		Object[] newElements = new Object[array.length - 1];
		System.arraycopy(array, 0, newElements, 0, index);
		System.arraycopy(array, index + 1, newElements, index, array.length - index - 1);
		array = newElements;
		return true;
	}

	/**
	 * Returns {@code true} if this list contains all of the elements of the
	 * specified collection.
	 *
	 * @param c
	 *            collection to be checked for containment in this list
	 * @return {@code true} if this list contains all of the elements of the
	 *         specified collection
	 * @throws NullPointerException
	 *             if the specified collection is null
	 * @see #contains(Object)
	 */
	public boolean containsAll(Collection<?> c) {
		for (Object e : c) {
			if (indexOf(e, array, 0, array.length) < 0)
				return false;
		}
		return true;
	}

	public void clear() {
		array = new Object[0];
	}

	public boolean addAll(Collection<? extends E> c) {
		Object[] cs = (c.getClass() == ParamList.class) ? ((ParamList<?>) c).array : c.toArray();
		if (cs.length == 0)
			return false;
		if (array.length == 0 && cs.getClass() == Object[].class)
			array = cs;
		else {
			Object[] newElements = Arrays.copyOf(array, array.length + cs.length);
			System.arraycopy(cs, 0, newElements, array.length, cs.length);
			array = newElements;
		}
		return true;
	}

	/**
	 * Inserts all of the elements in the specified collection into this list,
	 * starting at the specified position. Shifts the element currently at that
	 * position (if any) and any subsequent elements to the right (increases their
	 * indices). The new elements will appear in this list in the order that they
	 * are returned by the specified collection's iterator.
	 *
	 * @param index
	 *            index at which to insert the first element from the specified
	 *            collection
	 * @param c
	 *            collection containing elements to be added to this list
	 * @return {@code true} if this list changed as a result of the call
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified collection is null
	 * @see #add(int,Object)
	 */
	public boolean addAll(int index, Collection<? extends E> c) {
		Object[] cs = c.toArray();
		if (index > array.length || index < 0)
			throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + array.length);
		if (cs.length == 0)
			return false;
		int numMoved = array.length - index;
		Object[] newElements;
		if (numMoved == 0)
			newElements = Arrays.copyOf(array, array.length + cs.length);
		else {
			newElements = new Object[array.length + cs.length];
			System.arraycopy(array, 0, newElements, 0, index);
			System.arraycopy(array, index, newElements, index + cs.length, numMoved);
		}
		System.arraycopy(cs, 0, newElements, index, cs.length);
		array = newElements;
		return true;
	}

	@SuppressWarnings("unchecked")
	public void forEach(Consumer<? super E> action) {
		if (action == null)
			throw new NullPointerException();

		for (int i = 0; i < array.length; ++i) {
			E e = (E) array[i];
			action.accept(e);
		}
	}

	public boolean removeIf(Predicate<? super E> filter) {
		if (filter == null)
			throw new NullPointerException();
		if (array.length != 0) {
			int newlen = 0;
			Object[] temp = new Object[array.length];
			for (int i = 0; i < array.length; ++i) {
				@SuppressWarnings("unchecked")
				E e = (E) array[i];
				if (!filter.test(e))
					temp[newlen++] = e;
			}
			if (newlen != array.length) {
				array = Arrays.copyOf(temp, newlen);
				return true;
			}
		}
		return false;
	}

	public void replaceAll(UnaryOperator<E> operator) {
		if (operator == null)
			throw new NullPointerException();
		Object[] newElements = Arrays.copyOf(array, array.length);
		for (int i = 0; i < array.length; ++i) {
			@SuppressWarnings("unchecked")
			E e = (E) array[i];
			newElements[i] = operator.apply(e);
		}
		array = newElements;
	}

	@SuppressWarnings("unchecked")
	public void sort(Comparator<? super E> c) {
		Object[] newElements = Arrays.copyOf(array, array.length);
		E[] es = (E[]) newElements;
		Arrays.sort(es, c);
		array = newElements;
	}

	/**
	 * Returns a string representation of this list. The string representation
	 * consists of the string representations of the list's elements in the order
	 * they are returned by its iterator, enclosed in square brackets
	 * ({@code "[]"}). Adjacent elements are separated by the characters
	 * {@code ", "} (comma and space). Elements are converted to strings as by
	 * {@link String#valueOf(Object)}.
	 *
	 * @return a string representation of this list
	 */
	public String toString() {
		return Arrays.toString(array);
	}

	/**
	 * Compares the specified object with this list for equality. Returns
	 * {@code true} if the specified object is the same object as this object, or if
	 * it is also a {@link List} and the sequence of elements returned by an
	 * {@linkplain List#iterator() iterator} over the specified list is the same as
	 * the sequence returned by an iterator over this list. The two sequences are
	 * considered to be the same if they have the same length and corresponding
	 * elements at the same position in the sequence are <em>equal</em>. Two
	 * elements {@code e1} and {@code e2} are considered <em>equal</em> if
	 * {@code (e1==null ? e2==null : e1.equals(e2))}.
	 *
	 * @param o
	 *            the object to be compared for equality with this list
	 * @return {@code true} if the specified object is equal to this list
	 */
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof List))
			return false;

		List<?> list = (List<?>) (o);
		Iterator<?> it = list.iterator();
		for (int i = 0; i < array.length; ++i)
			if (!it.hasNext() || !eq(array[i], it.next()))
				return false;
		if (it.hasNext())
			return false;
		return true;
	}

	/**
	 * Returns the hash code value for this list.
	 *
	 * <p>
	 * This implementation uses the definition in {@link List#hashCode}.
	 *
	 * @return the hash code value for this list
	 */
	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < array.length; ++i) {
			Object obj = array[i];
			hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
		}
		return hashCode;
	}

	/**
	 * Returns an iterator over the elements in this list in proper sequence.
	 *
	 * <p>
	 * The returned iterator provides a snapshot of the state of the list when the
	 * iterator was constructed. No synchronization is needed while traversing the
	 * iterator. The iterator does <em>NOT</em> support the {@code remove} method.
	 *
	 * @return an iterator over the elements in this list in proper sequence
	 */
	public Iterator<E> iterator() {
		return new COWIterator<E>(array, 0);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The returned iterator provides a snapshot of the state of the list when the
	 * iterator was constructed. No synchronization is needed while traversing the
	 * iterator. The iterator does <em>NOT</em> support the {@code remove},
	 * {@code set} or {@code add} methods.
	 */
	public ListIterator<E> listIterator() {
		return new COWIterator<E>(array, 0);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * The returned iterator provides a snapshot of the state of the list when the
	 * iterator was constructed. No synchronization is needed while traversing the
	 * iterator. The iterator does <em>NOT</em> support the {@code remove},
	 * {@code set} or {@code add} methods.
	 *
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public ListIterator<E> listIterator(int index) {
		if (index < 0 || index > array.length)
			throw new IndexOutOfBoundsException("Index: " + index);
		return new COWIterator<E>(array, index);
	}

	public Spliterator<E> spliterator() {
		return Spliterators.spliterator(array, Spliterator.IMMUTABLE | Spliterator.ORDERED);
	}

	static final class COWIterator<E> implements ListIterator<E> {
		/** Snapshot of the array */
		private final Object[]	snapshot;
		/** Index of element to be returned by subsequent call to next. */
		private int				cursor;

		private COWIterator(Object[] elements, int initialCursor) {
			cursor = initialCursor;
			snapshot = elements;
		}

		public final boolean hasNext() {
			return cursor < snapshot.length;
		}

		public final boolean hasPrevious() {
			return cursor > 0;
		}

		@SuppressWarnings("unchecked")
		public E next() {
			if (!hasNext())
				throw new NoSuchElementException();
			return (E) snapshot[cursor++];
		}

		@SuppressWarnings("unchecked")
		public E previous() {
			if (!hasPrevious())
				throw new NoSuchElementException();
			return (E) snapshot[--cursor];
		}

		public int nextIndex() {
			return cursor;
		}

		public int previousIndex() {
			return cursor - 1;
		}

		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * 
		 * @throws UnsupportedOperationException
		 *             always; {@code remove} is not supported by this iterator.
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * 
		 * @throws UnsupportedOperationException
		 *             always; {@code set} is not supported by this iterator.
		 */
		public void set(E e) {
			throw new UnsupportedOperationException();
		}

		/**
		 * Not supported. Always throws UnsupportedOperationException.
		 * 
		 * @throws UnsupportedOperationException
		 *             always; {@code add} is not supported by this iterator.
		 */
		public void add(E e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void forEachRemaining(Consumer<? super E> action) {
			Objects.requireNonNull(action);
			Object[] elements = snapshot;
			final int size = elements.length;
			for (int i = cursor; i < size; i++) {
				@SuppressWarnings("unchecked")
				E e = (E) elements[i];
				action.accept(e);
			}
			cursor = size;
		}
	}

	public List<E> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();// The current operation is not supported yet
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException(); // The current operation is not supported yet
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();// The current operation is not supported yet
	}

	public int addAllAbsent(Collection<? extends E> c) {
		throw new UnsupportedOperationException(); // The current operation is not supported yet
	}

	public <T> T[] toArray(T a[]) {
		throw new UnsupportedOperationException(); // The current operation is not supported yet
	}

}
>>>>>>> 2.2.x
