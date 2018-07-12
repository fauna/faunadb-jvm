package com.faunadb.client;

import com.faunadb.client.types.*;
import com.faunadb.client.types.Value.*;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.faunadb.client.types.Encoder.encode;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class EncoderSpec {

    private Instant parseInstant(String str) {
        return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(str));
    }

    @Test
    public void shouldEncodePrimitives() {
        assertEquals(NullV.NULL, encode(null).get());

        assertEquals(new StringV("a string"), encode("a string").get());

        assertEquals(BooleanV.TRUE, encode(true).get());
        assertEquals(BooleanV.FALSE, encode(false).get());

        assertEquals(new LongV(10), encode((char) 10).get());
        assertEquals(new LongV(10), encode((byte) 10).get());
        assertEquals(new LongV(10), encode((short) 10).get());
        assertEquals(new LongV(10), encode((int) 10).get());
        assertEquals(new LongV(10), encode((long) 10).get());

        assertEquals(new DoubleV(10), encode((float) 10).get());
        assertEquals(new DoubleV(10), encode((double) 10).get());

        assertEquals(new DateV(LocalDate.parse("1970-01-01")), encode(LocalDate.parse("1970-01-01")).get());

        assertEquals(new TimeV(parseInstant("1970-01-01T00:05:02.010000000Z")), encode(parseInstant("1970-01-01T00:05:02.010000000Z")).get());

        assertEquals(new BytesV(new byte[] {1, 2, 3, 4}), encode(new byte[] {1, 2, 3, 4}).get());
    }

    @Test
    public void shouldEncodeUsingValueApi() {
        assertEquals(NullV.NULL, Value.from(null).get());
        assertEquals(new StringV("a string"), Value.from("a string").get());
        assertEquals(BooleanV.TRUE, Value.from(true).get());
        assertEquals(new LongV(10), Value.from(10).get());
        assertEquals(new DoubleV(10), encode(10.0).get());
    }

    @Test
    public void shouldEncodeArrays() {
        assertEquals(new ArrayV(asList(new LongV(1))), encode(new char[] { 1 }).get());
        assertEquals(new ArrayV(asList(new LongV(1))), encode(new short[] { 1 }).get());
        assertEquals(new ArrayV(asList(new LongV(1))), encode(new int[] { 1 }).get());
        assertEquals(new ArrayV(asList(new LongV(1))), encode(new long[] { 1 }).get());

        assertEquals(new ArrayV(Arrays.<Value>asList(new LongV(1), new StringV("a string"))), encode(new Object[] { 1, "a string" }).get());
    }

    @Test
    public void shouldEncodeValues() {
        assertEquals(new StringV("a string"), encode(new StringV("a string")).get());
        assertEquals(new LongV(10), encode(new LongV(10)).get());
        assertEquals(new DoubleV(10), encode(new DoubleV(10)).get());
        assertEquals(new DateV(LocalDate.parse("1970-01-01")), encode(new DateV(LocalDate.parse("1970-01-01"))).get());
        assertEquals(new TimeV(parseInstant("1970-01-01T00:05:02.010000000Z")), encode(new TimeV(parseInstant("1970-01-01T00:05:02.010000000Z"))).get());
        assertEquals(new BytesV(new byte[] {1, 2, 3, 4}), encode(new BytesV(new byte[] {1, 2, 3, 4})).get());
        assertEquals(new RefV("123", new RefV("widgets", Native.CLASSES)), encode(new RefV("123", new RefV("widgets", Native.CLASSES))).get());
        assertEquals(new SetRefV(Collections.<String, Value>emptyMap()), encode(new SetRefV(Collections.<String, Value>emptyMap())).get());
        assertEquals(new ArrayV(asList(new LongV(1))), encode(new ArrayV(asList(new LongV(1)))).get());

        Map<String, Value> obj = new LinkedHashMap<>();
        obj.put("key", new StringV("value"));

        assertEquals(new ObjectV(obj), encode(new ObjectV(obj)).get());
    }

    static class ObjectWithGetter {
        private final String name;

        public ObjectWithGetter(String name) {
            this.name = name;
        }

        public String getName() {
            return name + " changed in getter";
        }
    }

    static class ObjectWithField {
        @FaunaField
        private final String name;

        public ObjectWithField(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class ObjectWithIgnore {
        @FaunaField
        private final String a = "a";

        @FaunaField
        @FaunaIgnore
        private final String b = "b";

        public String getC() {
            return "c";
        }

        @FaunaIgnore
        public String getD() {
            return "d";
        }
    }

    @Test
    public void shouldEncodeObjects() {
        Map<String, Value> changed = new LinkedHashMap<>();

        changed.put("name", new StringV("name changed in getter"));
        
        assertEquals(new ObjectV(changed), encode(new ObjectWithGetter("name")).get());

        Map<String, Value> unchanged = new LinkedHashMap<>();

        unchanged.put("name", new StringV("name not changed in getter"));

        assertEquals(new ObjectV(unchanged), encode(new ObjectWithField("name not changed in getter")).get());

        Map<String, Value> ignored = new LinkedHashMap<>();
        ignored.put("a", new StringV("a"));
        ignored.put("c", new StringV("c"));

        assertEquals(new ObjectV(ignored), encode(new ObjectWithIgnore()).get());
    }

    static class User {
        @FaunaField
        private final String name = "john";
        @FaunaField
        private final long age = 30;
    }

    @Test
    public void shouldEncodeArrayOfObjects() {
        Map<String, Value> obj = new LinkedHashMap<>();
        
        obj.put("name", new StringV("john"));
        obj.put("age", new LongV(30));
        
        ObjectV user = new ObjectV(obj);

        assertEquals(new ArrayV(asList(user, user)), encode(new Object[] { new User(), new User() }).get());
    }

    @Test
    public void shouldEncodeCollections() {
        assertEquals(new ArrayV(asList(new LongV(1), new LongV(2))), encode(newArrayList(1, 2)).get());
        assertEquals(new ArrayV(asList(new LongV(1), new LongV(2))), encode(newHashSet(1, 2)).get());
    }

    @Test
    public void shouldEncodeMaps() {
        Map<String, Value> obj = new LinkedHashMap<>();
        Map<String, String> enc = new LinkedHashMap<>();

        obj.put("a", new StringV("b"));
        enc.put("a", "b");

        assertEquals(new ObjectV(obj), encode(enc).get());
    }

    static class Node {
        @FaunaField
        private long data;

        @FaunaField
        private Node next;

        public Node(long data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return format("Node(%d)", data);
        }

        @Override
        public boolean equals(Object obj) {
            return false; //never be equal, so it should detect by reference
        }
    }

    @Test
    public void shouldDetectReferenceLoops() {
        Node head = new Node(10);
        Node tail = new Node(20);

        head.next = tail;
        tail.next = head;

        assertEquals(
          Result.fail("Could not encode field \"next\". Reason: Could not encode field \"next\". Reason: Self reference loop detected for object \"Node(10)\""),
          encode(head)
        );
    }

    static class ObjectRenamed {
        private final String field1 = "value1";

        @FaunaField("field2_renamed")
        private final String field2 = "value2";

        @FaunaField("field1_renamed")
        public String getField1() {
            return field1;
        }
    }

    @Test
    public void shouldRenameFieldName() {
        Map<String, Value> obj = new LinkedHashMap<>();

        obj.put("field1_renamed", new StringV("value1"));
        obj.put("field2_renamed", new StringV("value2"));

        assertEquals(
                new ObjectV(obj),
                encode(new ObjectRenamed()).get()
        );
    }

    static class ObjectRenamed2 {
        @FaunaField("field1")
        private final String value = "value";

        @FaunaField("field2")
        public String getValue() {
            return value;
        }
    }

    @Test
    public void shouldCreateTwoPropertiesIfFieldAndGetterAreAnnotated() {
        Map<String, Value> obj = new LinkedHashMap<>();

        obj.put("field1", new StringV("value"));
        obj.put("field2", new StringV("value"));

        assertEquals(
                new ObjectV(obj),
                encode(new ObjectRenamed2()).get()
        );
    }

    enum CpuType {
        @FaunaEnum("x86_32") X86,
        @FaunaEnum("x86_64") X86_64,
        ARM,
        MIPS
    }

    @Test
    public void shouldEncodeEnums() {
        assertEquals(new StringV("x86_32"), encode(CpuType.X86).get());
        assertEquals(new StringV("x86_64"), encode(CpuType.X86_64).get());
        assertEquals(new StringV("ARM"), encode(CpuType.ARM).get());
        assertEquals(new StringV("MIPS"), encode(CpuType.MIPS).get());
    }
}
