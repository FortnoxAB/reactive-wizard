package se.fortnox.reactivewizard.util;

import rx.Observable;

import static rx.Observable.empty;

abstract class AccessorTest {
    protected class GenericMethodSubclass extends GenericMethodSuper<String, Integer> {
        private Integer          value;
        private Observable<Long> longObservable;

        GenericMethodSubclass(Integer value) {
            super(String.valueOf(value), value);
            this.value = value;
            this.longObservable = empty();
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Observable<Long> getLongObservable() {
            return longObservable;
        }

        public void setLongObservable(Observable<Long> longObservable) {
            this.longObservable = longObservable;
        }

        @Override
        public String getSuperKey() {
            return super.getSuperKey();
        }

        @Override
        public void setSuperKey(String superKey) {
            super.setSuperKey(superKey);
        }
    }

    protected class GenericMethodSuper<K, V> {
        private K superKey;
        private V superValue;

        GenericMethodSuper(K key, V value) {
            this.superKey = key;
            this.superValue = value;
        }

        public K getSuperKey() {
            return superKey;
        }

        public void setSuperKey(K superKey) {
            this.superKey = superKey;
        }

        public V getSuperValue() {
            return superValue;
        }

        public void setSuperValue(V superValue) {
            this.superValue = superValue;
        }
    }

    protected class MethodSubclass extends MethodSuper {
        MethodSubclass(String superKey, Integer superValue) {
            super(superKey, superValue);
        }

        @Override
        public String getSuperKey() {
            return super.getSuperKey();
        }

        @Override
        public void setSuperKey(String superKey) {
            super.setSuperKey(superKey);
        }
    }

    protected class MethodSuper {
        private String superKey;
        private Integer superValue;

        MethodSuper(String superKey, Integer superValue) {
            this.superKey = superKey;
            this.superValue = superValue;
        }

        public String getSuperKey() {
            return superKey;
        }

        public void setSuperKey(String superKey) {
            this.superKey = superKey;
        }

        public Integer getSuperValue() {
            return superValue;
        }

        public void setSuperValue(Integer superValue) {
            this.superValue = superValue;
        }
    }

    protected class GenericFieldSubclass extends GenericFieldSuper<String, Integer> {
        final Integer          value;
        final Observable<Long> longObservable;

        GenericFieldSubclass(Integer value) {
            super(String.valueOf(value), value);
            this.value = value;
            longObservable = empty();
        }
    }

    protected class GenericFieldSuper<K, V> {
        final K superKey;
        final V superValue;

        GenericFieldSuper(K key, V value) {
            this.superKey = key;
            this.superValue = value;
        }
    }

    protected class FieldSubclass extends FieldSuper {
        final Integer          value;
        final Observable<Long> longObservable;

        FieldSubclass(Integer value) {
            super(String.valueOf(value), value);
            this.value = value;
            longObservable = empty();
        }
    }

    protected class FieldSuper {
        final String superKey;
        final Integer superValue;

        FieldSuper(String key, Integer value) {
            this.superKey = key;
            this.superValue = value;
        }
    }
}
