package com.faunadb.client;

import com.faunadb.client.types.*;
import com.faunadb.client.types.Value.*;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.faunadb.client.types.Decoder.decode;
import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;

public class DecoderSpec {

    private static final ObjectV EMPTY_OBJECT = new ObjectV(Collections.<String, Value>emptyMap());
    private static final ArrayV EMPTY_ARRAY = new ArrayV(Collections.<Value>emptyList());

    private Instant parseInstant(String str) {
        return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(str));
    }
    
    @Test
    public void shouldDecodePrimitives() {
        assertEquals("a string", decode(new StringV("a string"), String.class).get());

        assertEquals(true, decode(BooleanV.TRUE, Boolean.class).get());
        assertEquals(true, decode(BooleanV.TRUE, boolean.class).get());

        assertEquals(Long.valueOf(10), decode(new LongV(10), long.class).get());
        assertEquals(Long.valueOf(10), decode(new LongV(10), Long.class).get());

        assertEquals(Integer.valueOf(10), decode(new LongV(10), int.class).get());
        assertEquals(Integer.valueOf(10), decode(new LongV(10), Integer.class).get());

        assertEquals(Short.valueOf((short)10), decode(new LongV(10), short.class).get());
        assertEquals(Short.valueOf((short)10), decode(new LongV(10), Short.class).get());

        assertEquals(Byte.valueOf((byte)10), decode(new LongV(10), byte.class).get());
        assertEquals(Byte.valueOf((byte)10), decode(new LongV(10), Byte.class).get());

        assertEquals(Character.valueOf((char)10), decode(new LongV(10), char.class).get());
        assertEquals(Character.valueOf((char)10), decode(new LongV(10), Character.class).get());

        assertEquals(Float.valueOf(10), decode(new DoubleV(10), float.class).get());
        assertEquals(Float.valueOf(10), decode(new DoubleV(10), Float.class).get());

        assertEquals(Double.valueOf(10), decode(new DoubleV(10), double.class).get());
        assertEquals(Double.valueOf(10), decode(new DoubleV(10), Double.class).get());

        assertEquals(LocalDate.parse("1970-01-01"), decode(new DateV(LocalDate.parse("1970-01-01")), LocalDate.class).get());

        assertEquals(parseInstant("1970-01-01T00:05:02.010000000Z"), decode(new TimeV(parseInstant("1970-01-01T00:05:02.010000000Z")), Instant.class).get());

        assertArrayEquals(new byte[] {1, 2, 3}, decode(new BytesV(new byte[] {1, 2, 3}), byte[].class).get());
    }

    @Test
    public void shouldDecodeNullToFailure() {
        assertEquals(Result.fail("Value is null"), decode(NullV.NULL, Value.class));
        assertEquals(Result.fail("Value is null"), decode(null, Value.class));
    }

    @Test
    public void shouldDecodeValues() {
        assertEquals(new StringV("a string"), decode(new StringV("a string"), StringV.class).get());

        assertEquals(BooleanV.TRUE, decode(BooleanV.TRUE, BooleanV.class).get());
        assertEquals(BooleanV.FALSE, decode(BooleanV.FALSE, BooleanV.class).get());

        assertEquals(new LongV(10), decode(new LongV(10), LongV.class).get());

        assertEquals(new DoubleV(10), decode(new DoubleV(10), DoubleV.class).get());

        assertEquals(new DateV(LocalDate.parse("1970-01-01")), decode(new DateV(LocalDate.parse("1970-01-01")), DateV.class).get());

        assertEquals(new TimeV(parseInstant("1970-01-01T00:05:02.010000000Z")), decode(new TimeV(parseInstant("1970-01-01T00:05:02.010000000Z")), TimeV.class).get());
    }

    @Test
    public void shouldDecodeArrays() {
        assertArrayEquals(new long[] {1, 2, 3}, decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), long[].class).get());
        assertArrayEquals(new int[] {1, 2, 3}, decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), int[].class).get());
        assertArrayEquals(new short[] {1, 2, 3}, decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), short[].class).get());
        assertArrayEquals(new byte[] {1, 2, 3}, decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), byte[].class).get());
        assertArrayEquals(new char[] {1, 2, 3}, decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), char[].class).get());

        assertArrayEquals(new float[] {1, 2, 3}, decode(new ArrayV(asList(new DoubleV(1), new DoubleV(2), new DoubleV(3))), float[].class).get(), 0);
        assertArrayEquals(new double[] {1, 2, 3}, decode(new ArrayV(asList(new DoubleV(1), new DoubleV(2), new DoubleV(3))), double[].class).get(), 0);
    }

    @Test
    public void shouldDecodeCollections() {
        //lists

        assertEquals(
                newArrayList(1L, 2L, 3L),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.arrayListOf(long.class)).get()
        );

        assertEquals(
                newArrayList(1, 2, 3),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.arrayListOf(int.class)).get()
        );

        assertEquals(
                newArrayList((short)1, (short)2, (short)3),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.arrayListOf(short.class)).get()
        );

        assertEquals(
                newArrayList((byte)1, (byte)2, (byte)3),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.arrayListOf(byte.class)).get()
        );

        assertEquals(
                newArrayList((char)1, (char)2, (char)3),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.arrayListOf(char.class)).get()
        );

        //sets

        assertEquals(
                newHashSet(1L, 2L, 3L),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.hashSetOf(long.class)).get()
        );

        assertEquals(
                newHashSet(1, 2, 3),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.hashSetOf(int.class)).get()
        );

        assertEquals(
                newHashSet((short)1, (short)2, (short)3),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.hashSetOf(short.class)).get()
        );

        assertEquals(
                newHashSet((byte)1, (byte)2, (byte)3),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.hashSetOf(byte.class)).get()
        );

        assertEquals(
                newHashSet((char)1, (char)2, (char)3),
                decode(new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))), Types.hashSetOf(char.class)).get()
        );
    }

    @Test
    public void shouldDecodeMaps() {
        assertEquals(
                newHashMap(of("a", 10L)),
                decode(new ObjectV(of("a", new LongV(10))), Types.hashMapOf(long.class)).get()
        );

        assertEquals(
                newHashMap(of("a", 10)),
                decode(new ObjectV(of("a", new LongV(10))), Types.hashMapOf(int.class)).get()
        );
    }

    static class SimpleObject {
        private String strField;
        private long longField;
        private List<String> listStrField;
        private Set<Long> setLongField;
        private Map<String, String> mapStrToStr;

        public String getStrField() {
            return strField;
        }

        public void setStrField(String strField) {
            this.strField = strField;
        }

        public long getLongField() {
            return longField;
        }

        public void setLongField(long longField) {
            this.longField = longField;
        }

        public List<String> getListStrField() {
            return listStrField;
        }

        public void setListStrField(List<String> listStrField) {
            this.listStrField = listStrField;
        }

        public Set<Long> getSetLongField() {
            return setLongField;
        }

        public void setSetLongField(Set<Long> setLongField) {
            this.setLongField = setLongField;
        }

        public Map<String, String> getMapStrToStr() {
            return mapStrToStr;
        }

        public void setMapStrToStr(Map<String, String> mapStrToStr) {
            this.mapStrToStr = mapStrToStr;
        }

        @Override
        public boolean equals(Object obj) {
            SimpleObject other = (SimpleObject) obj;
            return strField.equals(other.strField)
                    && longField == other.longField
                    && listStrField.equals(other.listStrField)
                    && setLongField.equals(other.setLongField)
                    && mapStrToStr.equals(other.mapStrToStr);
        }

        @Override
        public String toString() {
            return format("%s(%s, %s, %s, %s, %s)",
                    getClass().getSimpleName(), strField, longField, listStrField, setLongField, mapStrToStr);
        }
    }

    @Test
    public void shouldDecodeSimpleObjects() {
        SimpleObject obj = new SimpleObject();
        obj.setStrField("value");
        obj.setLongField(10);
        obj.setListStrField(newArrayList("value1", "value2"));
        obj.setSetLongField(newHashSet(1L, 2L, 3L));
        obj.setMapStrToStr(newHashMap(of("key", "value")));

        ObjectV objectV = new ObjectV(of(
                "strField", new StringV("value"),
                "longField", new LongV(10),
                "listStrField", new ArrayV(asList(new StringV("value1"), new StringV("value2"))),
                "setLongField", new ArrayV(asList(new LongV(1), new LongV(2), new LongV(3))),
                "mapStrToStr", new ObjectV(of("key", new StringV("value")))
        ));
        assertEquals(obj, decode(objectV, SimpleObject.class).get());
    }

    static class ObjectWithConstructor {
        private final String strField;

        @FaunaConstructor
        public ObjectWithConstructor(@FaunaField("strField") String strField) {
            this.strField = strField;
        }

        public String getStrField() {
            return strField;
        }

        @Override
        public boolean equals(Object obj) {
            ObjectWithConstructor other = (ObjectWithConstructor) obj;
            return strField.equals(other.strField);
        }

        @Override
        public String toString() {
            return format("%s(%s)", getClass().getSimpleName(), strField);
        }
    }

    @Test
    public void shouldDecodeObjectsWithConstructor() {
        ObjectWithConstructor obj = new ObjectWithConstructor("value");

        ObjectV objectV = new ObjectV(of("strField", new StringV("value")));

        assertEquals(obj, decode(objectV, ObjectWithConstructor.class).get());
    }

    static class ObjectWithStaticCreator {
        private final String strField;
        private final long longField;

        ObjectWithStaticCreator(String strField, long longField) {
            this.strField = strField;
            this.longField = longField;
        }

        @FaunaConstructor
        public static Object creator(@FaunaField("strField") String strField) {
            return new ObjectWithStaticCreator(strField, 10);
        }

        @Override
        public boolean equals(Object obj) {
            ObjectWithStaticCreator other = (ObjectWithStaticCreator) obj;
            return strField.equals(other.strField) && longField == other.longField;
        }

        @Override
        public String toString() {
            return format("%s(%s, %s)", getClass().getSimpleName(), strField, longField);
        }
    }

    @Test
    public void shouldDecodeObjectsWithStaticCreator() {
        ObjectWithStaticCreator obj = new ObjectWithStaticCreator("value", 10);

        ObjectV objectV = new ObjectV(of("strField", new StringV("value")));

        assertEquals(obj, decode(objectV, ObjectWithStaticCreator.class).get());
    }

    static class ObjectWithCreatorAndPropertiesMixed {
        private String strField;
        private long longField;

        @FaunaConstructor
        public static Object creator(@FaunaField("strField") String strField) {
            ObjectWithCreatorAndPropertiesMixed object = new ObjectWithCreatorAndPropertiesMixed();
            object.strField = strField;
            return object;
        }

        public void setLongField(long longField) {
            this.longField = longField;
        }

        @Override
        public boolean equals(Object obj) {
            ObjectWithCreatorAndPropertiesMixed other = (ObjectWithCreatorAndPropertiesMixed) obj;
            return strField.equals(other.strField) && longField == other.longField;
        }

        @Override
        public String toString() {
            return format("%s(%s, %s)", getClass().getSimpleName(), strField, longField);
        }
    }

    @Test
    public void shouldDecodeObjectsWithCreatorAndPropertiesMixed() {
        ObjectWithCreatorAndPropertiesMixed obj = new ObjectWithCreatorAndPropertiesMixed();
        obj.strField = "value";
        obj.longField = 10;

        ObjectV objectV = new ObjectV(of("strField", new StringV("value"), "longField", new LongV(10)));

        assertEquals(obj, decode(objectV, ObjectWithCreatorAndPropertiesMixed.class).get());
    }

    static class ObjectWithMapField {
        @FaunaField
        private Map<Long, String> map;
    }

    @Test
    public void shouldFailForMapKeysOnFields() {
        assertEquals(
          Result.fail("Only string keys are supported for maps"),
          decode(EMPTY_OBJECT, ObjectWithMapField.class)
        );
    }

    static class ObjectWithConstructorNotAnnotated {
        @FaunaConstructor
        public ObjectWithConstructorNotAnnotated(String field) {
        }
    }

    @Test
    public void shouldFailForConstructorNotAnnotated() {
        assertEquals(
          Result.fail("All constructor or factory method arguments must be annotated with @FaunaField"),
          decode(EMPTY_OBJECT, ObjectWithConstructorNotAnnotated.class)
        );
    }

    static class ObjectWithStaticCreatorNotAnnotated {
        @FaunaConstructor
        public static Object creator(String field) {
            return null;
        }
    }

    @Test
    public void shouldFailForStaticCreatorNotAnnotated() {
        assertEquals(
          Result.fail("All constructor or factory method arguments must be annotated with @FaunaField"),
          decode(EMPTY_OBJECT, ObjectWithStaticCreatorNotAnnotated.class)
        );
    }

    static class ObjectWithNoAnnotatedConstructorAndNotDefaultConstructor {
        public ObjectWithNoAnnotatedConstructorAndNotDefaultConstructor(String field) { }
    }

    @Test
    public void shouldFailWhenNotSuitableConstructorAreFound() {
        assertEquals(
          Result.fail("No suitable constructor or factory method found for type com.faunadb.client.DecoderSpec$ObjectWithNoAnnotatedConstructorAndNotDefaultConstructor. Ensure that a factory method or constructor is annotated with @FaunaConstructor"),
          decode(EMPTY_OBJECT, ObjectWithNoAnnotatedConstructorAndNotDefaultConstructor.class)
        );
    }

    static class ObjectWithFieldsIgnored {
        @FaunaIgnore
        @FaunaField
        String fieldIgnored = "initial value";

        @FaunaField
        String fieldNotIgnored;
    }

    @Test
    public void shouldIgnoreFieldAnnotated() {
        ObjectWithFieldsIgnored obj = decode(new ObjectV(of(
                "fieldIgnored", new StringV("should be ignored"),
                "fieldNotIgnored", new StringV("value")
        )), ObjectWithFieldsIgnored.class).get();

        assertEquals("value", obj.fieldNotIgnored);
        assertEquals("initial value", obj.fieldIgnored);
    }

    enum CpuType {
        @FaunaEnum("x86_32") X86,
        @FaunaEnum("x86_64") X86_64,
        ARM,
        MIPS
    }

    @Test
    public void shouldDecodeEnums() {
        assertEquals(CpuType.X86, decode(new StringV("x86_32"), CpuType.class).get());
        assertEquals(CpuType.X86_64, decode(new StringV("x86_64"), CpuType.class).get());
        assertEquals(CpuType.ARM, decode(new StringV("ARM"), CpuType.class).get());
        assertEquals(CpuType.MIPS, decode(new StringV("MIPS"), CpuType.class).get());
    }

    static class NotInstantiableArrayList<T> extends ArrayList<T> { }

    @Test
    public void shouldFailWhenIsNotPossibleToInstantiateACollectionType() {
        assertEquals(
          Result.fail("Could not instantiate collection of type com.faunadb.client.DecoderSpec$NotInstantiableArrayList<long>"),
          decode(EMPTY_ARRAY, Types.collectionOf(NotInstantiableArrayList.class, long.class))
        );
    }

    static class NotInstantiableHashMap<K, V> extends HashMap<K, V> { }

    @Test
    public void shouldFailWhenIsNotPossibleToInstantiateAMapType() {
        assertEquals(
          Result.fail("Could not instantiate map of type com.faunadb.client.DecoderSpec$NotInstantiableHashMap<java.lang.String,long>"),
          decode(EMPTY_OBJECT, Types.mapOf(NotInstantiableHashMap.class, long.class))
        );
    }

    static class NotInstantiableObject {
        private NotInstantiableObject() throws Exception {
            throw new Exception("some random error");
        }
    }

    @Test
    public void shouldFailWhenIsNotPossibleToInstantiateAnObject() {
        assertEquals(
          Result.fail("Could not instantiate object of class com.faunadb.client.DecoderSpec$NotInstantiableObject"),
          decode(EMPTY_OBJECT, NotInstantiableObject.class)
        );
    }

    @Test
    public void shouldDecodeUsingValueApi() {
        assertEquals((Long) 10L, new LongV(10).to(long.class).get());
        assertEquals((Integer) 10, new LongV(10).to(int.class).get());

        assertEquals(newArrayList(10, 20), new ArrayV(newArrayList(new LongV(10), new LongV(20))).asCollectionOf(int.class).get());
        assertEquals(newHashMap(of("key", "value")), new ObjectV(of("key", new StringV("value"))).asMapOf(String.class).get());

        assertArrayEquals(new int[]{10, 20}, new ArrayV(newArrayList(new LongV(10), new LongV(20))).to(int[].class).get());
    }

    static class ClassWithDefaults {
        @FaunaField
        Integer nullableField;

        @FaunaField
        int nonNullableField;
    }

    @Test
    public void shouldApplyDefaultValuesForFieldsWhenNull() throws IOException {
        ClassWithDefaults classWithDefaults = decode(new ObjectV(of("nullableField", NullV.NULL, "nonNullableField", NullV.NULL)), ClassWithDefaults.class).get();

        assertNull(classWithDefaults.nullableField);
        assertEquals(0, classWithDefaults.nonNullableField);
   }
}
