package com.faunadb.client.streaming;

public class EventFields {

    public static EventField DocumentField = new EventField() {
        @Override
        public String value() {
            return "document";
        }
    };

    public static EventField PrevField = new EventField() {
        @Override
        public String value() {
            return "prev";
        }
    };

    public static EventField DiffField = new EventField() {
        @Override
        public String value() {
            return "diff";
        }
    };

    public static EventField ActionField = new EventField() {
        @Override
        public String value() {
            return "action";
        }
    };

    public static EventField IndexField = new EventField() {
        @Override
        public String value() {
            return "index";
        }
    };
}
