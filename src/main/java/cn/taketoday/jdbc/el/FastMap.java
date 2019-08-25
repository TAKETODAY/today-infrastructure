package cn.taketoday.jdbc.el;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Today <br>
 * 
 *         2018-11-21 18:41
 */
public class FastMap<K, V> {

    static final Logger log = LoggerFactory.getLogger(FastMap.class);

    private int size = 0;

    private Node<K, V>[] objects;

    public FastMap() {
        this(16);
    }

    @SuppressWarnings("unchecked")
    public FastMap(int size) {
        this.objects = new Node[size];
    }

//    static final int hash(Object key) {
//        int h;
//        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
//    }

    static final int hash(Object key) {
        return (key == null) ? 0 : key.hashCode();
    }

    static class Node<K, V> implements Map.Entry<K, V> {
        final int hash;
        final K key;
        V value;
        Node<K, V> next;

        Node(int hash, K key, V value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            return this.value = value;
        }

        public void setNext(Node<K, V> next) {
            this.next = next;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("{\"hash\":\"").append(hash).append("\",\"key\":\"").append(key).append("\",\"value\":\"").append(value).append(
                    "\",\"next\":\"").append(next).append("\"}");
            return builder.toString();
        }

    }

    public V put(K key, V value) {
        return putVal(hash(key), key, value);
    }

    protected Node<K, V> newNode(int hash, K key, V value) {
        return new Node<>(hash, key, value);
    }

    private final V putVal(int hash, K key, V value) {

        final Node<K, V>[] objects = this.objects;
        final int i = (objects.length - 1) & hash;

        Node<K, V> oldN = objects[i];
        if (oldN == null) {
            objects[i] = newNode(hash, key, value);
        }
        else {

            Node<K, V> next;
            K k;
            if (oldN.hash == hash && ((k = oldN.key) == key || (key != null && key.equals(k)))) {
                next = oldN;
            }
            else {

                while (true) {
                    if ((next = oldN.next) == null) {
                        oldN.next = newNode(hash, key, value);
                        break;
                    }
                    if (next.hash == hash && ((k = next.key) == key || (key != null && key.equals(k))))
                        break;
                    oldN = next;
                }
            }

            if (next != null) { // existing mapping for key
                V oldV = next.value;
                if (oldV == null)
                    next.value = value;
                oldN = next;
            }
        }
        ++size;
        return oldN == null ? null : oldN.value;
    }

    public V get(Object key) {
        Node<K, V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    public int size() {
        return size;
    }

    private Node<K, V> getNode(int hash, Object key) {
        Node<K, V> node;
        final Node<K, V>[] objects = this.objects;
        int length = objects.length;
        K k;
        if (objects != null && length > 0 && (node = objects[(length - 1) & hash]) != null) {
            if (node.hash == hash && //
                    ((k = node.key) == key || (key != null && key.equals(k)))) {
                return node;
            }
            if ((node = node.next) != null) {
                do {

                    if (node.hash == hash && ((k = node.key) == key || (key != null && key.equals(k))))
                        return node;
                } while ((node = node.next) != null);
            }
        }
        return null;
    }

    public static void main(String[] args) {

        int size2 = Integer.MAX_VALUE / 10000;
        FastMap<String, Integer> map = new FastMap<>(size2);

        System.err.println(map.size());
//        for (int i = 0; i < 100; i++) {
//            log.debug("key:[{}] -> [{}]", "key_" + i, i);
//            map.put("key_" + i, i);
//        }

        long start = System.currentTimeMillis();
//        for (int i = 0; i < 100; i++) {
//            log.debug("key:[{}] -> [{}]", i, map.get("key_" + i));
//        }
//        System.err.println(map.size());
//        System.err.println(System.currentTimeMillis() - start + "ms");

        for (int i = 0; i < size2; i++) {
            map.put("key_" + i, i);
        }

        log.debug("put over");
//        long start = System.currentTimeMillis();
        for (int i = 0; i < size2; i++) {
            if (map.get("key_" + i) != i) {
                System.err.println("error with: " + i);
            }
        }

        System.err.println("map size: " + map.size);
        System.err.println(System.currentTimeMillis() - start);
        log.debug("get over");
    }
}
