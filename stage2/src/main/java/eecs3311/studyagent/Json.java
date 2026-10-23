package eecs3311.studyagent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small JSON codec for structured LLM requests and responses; no mutable global state. */
public final class Json {
    private Json() { }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value();
        parser.space();
        if (parser.position != text.length()) throw new IllegalArgumentException("Trailing JSON content");
        return value;
    }

    public static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            List<String> parts = new ArrayList<>();
            for (var entry : map.entrySet()) parts.add(write(entry.getKey().toString()) + ":" + write(entry.getValue()));
            return "{" + String.join(",", parts) + "}";
        }
        if (value instanceof Iterable<?> items) {
            List<String> parts = new ArrayList<>();
            for (Object item : items) parts.add(write(item));
            return "[" + String.join(",", parts) + "]";
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    public static Map<String, Object> object(Object value) {
        if (!(value instanceof Map<?, ?> source)) throw new IllegalArgumentException("Expected JSON object");
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(key.toString(), item));
        return result;
    }

    public static List<?> array(Object value) {
        if (!(value instanceof List<?> list)) throw new IllegalArgumentException("Expected JSON array");
        return list;
    }

    public static String string(Map<String, Object> value, String key) {
        Object item = value.get(key);
        if (!(item instanceof String s)) throw new IllegalArgumentException("Expected string: " + key);
        return s;
    }

    private static final class Parser {
        private final String input;
        private int position;
        Parser(String input) { this.input = input; }
        void space() { while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++; }
        char next() {
            if (position >= input.length()) throw new IllegalArgumentException("Unexpected end of JSON");
            return input.charAt(position++);
        }
        Object value() {
            space();
            if (position >= input.length()) throw new IllegalArgumentException("Empty JSON");
            char c = input.charAt(position);
            if (c == '"') return string();
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == 't' || c == 'f' || c == 'n') return literal();
            return number();
        }
        String string() {
            if (next() != '"') throw new IllegalArgumentException("Expected quote");
            StringBuilder result = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') return result.toString();
                if (c != '\\') { result.append(c); continue; }
                char escape = next();
                switch (escape) {
                    case '"', '\\', '/' -> result.append(escape);
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'u' -> {
                        if (position + 4 > input.length()) throw new IllegalArgumentException("Bad Unicode escape");
                        result.append((char) Integer.parseInt(input.substring(position, position + 4), 16));
                        position += 4;
                    }
                    default -> throw new IllegalArgumentException("Bad JSON escape");
                }
            }
        }
        Map<String, Object> object() {
            next();
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (input.charAt(position) == '}') { position++; return result; }
            do {
                space();
                String key = string();
                space();
                if (next() != ':') throw new IllegalArgumentException("Expected colon");
                result.put(key, value());
                space();
                char separator = next();
                if (separator == '}') return result;
                if (separator != ',') throw new IllegalArgumentException("Expected comma");
            } while (true);
        }
        List<Object> array() {
            next();
            List<Object> result = new ArrayList<>();
            space();
            if (input.charAt(position) == ']') { position++; return result; }
            do {
                result.add(value());
                space();
                char separator = next();
                if (separator == ']') return result;
                if (separator != ',') throw new IllegalArgumentException("Expected comma");
            } while (true);
        }
        Object literal() {
            for (String token : List.of("true", "false", "null")) {
                if (input.startsWith(token, position)) {
                    position += token.length();
                    return token.equals("null") ? null : Boolean.valueOf(token);
                }
            }
            throw new IllegalArgumentException("Invalid JSON literal");
        }
        Number number() {
            int start = position;
            while (position < input.length() && "-+0123456789.eE".indexOf(input.charAt(position)) >= 0) position++;
            if (start == position) throw new IllegalArgumentException("Invalid JSON value");
            String text = input.substring(start, position);
            return text.contains(".") || text.contains("e") || text.contains("E")
                    ? Double.valueOf(text) : Long.valueOf(text);
        }
    }
}
