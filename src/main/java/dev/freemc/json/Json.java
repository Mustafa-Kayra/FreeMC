package dev.freemc.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal dependency-free JSON reader. Numbers become Long when integral, Double otherwise. */
public final class Json {

    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        Json parser = new Json(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("unexpected trailing content");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(String text) {
        Object value = parse(text);
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("expected a JSON object");
        }
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object parent, String key) {
        if (!(parent instanceof Map)) {
            return null;
        }
        Object value = ((Map<String, Object>) parent).get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object parent, String key) {
        if (!(parent instanceof Map)) {
            return List.of();
        }
        Object value = ((Map<String, Object>) parent).get(key);
        return value instanceof List ? (List<Object>) value : List.of();
    }

    public static String str(Object parent, String key) {
        if (!(parent instanceof Map)) {
            return null;
        }
        Object value = ((Map<String, Object>) parent).get(key);
        return value instanceof String ? (String) value : null;
    }

    public static String str(Object parent, String key, String fallback) {
        String value = str(parent, key);
        return value == null ? fallback : value;
    }

    public static long num(Object parent, String key, long fallback) {
        if (!(parent instanceof Map)) {
            return fallback;
        }
        Object value = ((Map<String, Object>) parent).get(key);
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    public static boolean bool(Object parent, String key, boolean fallback) {
        if (!(parent instanceof Map)) {
            return fallback;
        }
        Object value = ((Map<String, Object>) parent).get(key);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private Object readValue() {
        if (pos >= text.length()) {
            throw error("unexpected end of input");
        }
        char c = text.charAt(pos);
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                expect("true");
                return Boolean.TRUE;
            case 'f':
                expect("false");
                return Boolean.FALSE;
            case 'n':
                expect("null");
                return null;
            default:
                return readNumber();
        }
    }

    private Map<String, Object> readObject() {
        Map<String, Object> result = new LinkedHashMap<>();
        pos++;
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("expected an object key");
            }
            String key = readString();
            skipWhitespace();
            if (peek() != ':') {
                throw error("expected ':'");
            }
            pos++;
            skipWhitespace();
            result.put(key, readValue());
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
                continue;
            }
            if (c == '}') {
                pos++;
                return result;
            }
            throw error("expected ',' or '}'");
        }
    }

    private List<Object> readArray() {
        List<Object> result = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return result;
        }
        while (true) {
            skipWhitespace();
            result.add(readValue());
            skipWhitespace();
            char c = peek();
            if (c == ',') {
                pos++;
                continue;
            }
            if (c == ']') {
                pos++;
                return result;
            }
            throw error("expected ',' or ']'");
        }
    }

    private String readString() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw error("unterminated string");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            if (pos >= text.length()) {
                throw error("unterminated escape");
            }
            char escape = text.charAt(pos++);
            switch (escape) {
                case '"':
                    sb.append('"');
                    break;
                case '\\':
                    sb.append('\\');
                    break;
                case '/':
                    sb.append('/');
                    break;
                case 'b':
                    sb.append('\b');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'u':
                    if (pos + 4 > text.length()) {
                        throw error("truncated unicode escape");
                    }
                    sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                    break;
                default:
                    throw error("invalid escape '\\" + escape + "'");
            }
        }
    }

    private Object readNumber() {
        int start = pos;
        if (peek() == '-' || peek() == '+') {
            pos++;
        }
        boolean floating = false;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                floating = true;
                pos++;
            } else {
                break;
            }
        }
        if (start == pos) {
            throw error("expected a value");
        }
        String raw = text.substring(start, pos);
        try {
            return floating ? (Object) Double.parseDouble(raw) : (Object) Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw error("invalid number '" + raw + "'");
        }
    }

    private void expect(String literal) {
        if (!text.startsWith(literal, pos)) {
            throw error("expected '" + literal + "'");
        }
        pos += literal.length();
    }

    private char peek() {
        if (pos >= text.length()) {
            throw error("unexpected end of input");
        }
        return text.charAt(pos);
    }

    private void skipWhitespace() {
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
            } else {
                break;
            }
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON: " + message + " at offset " + pos);
    }
}
