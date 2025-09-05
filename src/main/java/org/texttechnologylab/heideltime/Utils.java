package org.texttechnologylab.heideltime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static List<MatchResult> findMatches(Pattern pattern, CharSequence s) {
        if (s == null || s.isEmpty()) {
            return Collections.emptyList();
        }

        List<MatchResult> results = new ArrayList<MatchResult>();
        for (Matcher m = pattern.matcher(s); m.find();)
            results.add(m.toMatchResult());
        return results;
    }

    public static String replaceSpaces(String text) {
        return text.replaceAll(" ", "[\\\\u2000-\\\\u200A \\\\u202F\\\\u205F\\\\u3000\\\\u00A0\\\\u1680\\\\u180E]+");
    }

    public static class MergeSets<T> implements BinaryOperator<Set<T>> {

        @Override
        public Set<T> apply(Set<T> a, Set<T> b) {
            a.addAll(b);
            return a;
        }
    }
}
