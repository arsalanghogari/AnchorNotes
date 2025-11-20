package android.text;

import java.util.Iterator;

/**
 * Minimal stub of Android's TextUtils for local unit tests.
 * Only the methods invoked by the Template model / related helpers are implemented.
 */
public final class TextUtils {

    private TextUtils() {
        // Prevent instantiation.
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }

    public static String join(CharSequence delimiter, Iterable<?> tokens) {
        if (tokens == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Iterator<?> iterator = tokens.iterator();
        while (iterator.hasNext()) {
            Object token = iterator.next();
            if (token != null) {
                builder.append(token);
            }
            if (iterator.hasNext()) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }
}


