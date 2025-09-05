package org.texttechnologylab.heideltime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.unihd.dbs.uima.annotator.heideltime.utilities.DateCalculator;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Toolbox;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.RePatternManager;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * This class contains methods that work with the dependence of a subject with its
 * surrounding data; namely via the jcas element or a subset list.
 *
 * @author jannik stroetgen
 */
public class ContextAnalyzer {

    public static final Pattern PATTERN_EXCLUDE_SUFFIX = Pattern.compile("^[°\\w]|^[.,]\\d");
    public static final Pattern PATTERN_EXCLUDE_PREFIX = Pattern.compile("\\d\\.$|\\w$|[$+]\\s*$");

    /**
     * The value of the x of the last mentioned Timex is calculated.
     *
     * @param linearDates list of previous linear dates
     * @param i           index for the previous dates entry
     * @param x           type to search for
     * @return last mentioned entry
     */
    public static String getLastMentionedX(List<Timex3> linearDates, int i, String x, Language language) {
        NormalizationManager nm = NormalizationManager.getInstance(language, true);

        // Timex for which to get the last mentioned x (i.e., Timex i)
        Timex3 t_i = linearDates.get(i);

        String xValue = "";
        int j = i - 1;
        while (j >= 0) {
            Timex3 timex = linearDates.get(j);
            // check that the two timexes to compare do not have the same offset:
            if (!(t_i.getBegin() == timex.getBegin())) {

                String value = timex.getTimexValue();
                if (!(value.contains("funcDate"))) {
                    if (x.equals("century")) {
                        if (value.matches("^[0-9][0-9].*")) {
                            xValue = value.substring(0, 2);
                            break;
                        } else if (value.matches("^BC[0-9][0-9].*")) {
                            xValue = value.substring(0, 4);
                            break;
                        } else {
                            j--;
                        }
                    } else if (x.equals("decade")) {
                        if (value.matches("^[0-9][0-9][0-9].*")) {
                            xValue = value.substring(0, 3);
                            break;
                        } else if (value.matches("^BC[0-9][0-9][0-9].*")) {
                            xValue = value.substring(0, 5);
                            break;
                        } else {
                            j--;
                        }
                    } else if (x.equals("year")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9].*")) {
                            xValue = value.substring(0, 4);
                            break;
                        } else if (value.matches("^BC[0-9][0-9][0-9][0-9].*")) {
                            xValue = value.substring(0, 6);
                            break;
                        } else {
                            j--;
                        }
                    } else if (x.equals("dateYear")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9].*")) {
                            xValue = value;
                            break;
                        } else if (value.matches("^BC[0-9][0-9][0-9][0-9].*")) {
                            xValue = value;
                            break;
                        } else {
                            j--;
                        }
                    } else if (x.equals("month")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9].*")) {
                            xValue = value.substring(0, 7);
                            break;
                        } else if (value.matches("^BC[0-9][0-9][0-9][0-9]-[0-9][0-9].*")) {
                            xValue = value.substring(0, 9);
                            break;
                        } else {
                            j--;
                        }
                    } else if (x.equals("month-with-details")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9].*")) {
                            xValue = value;
                            break;
                        }
//							else if (value.matches("^BC[0-9][0-9][0-9][0-9]-[0-9][0-9].*")) {
//								xValue = value;
//								break;
//							}
                        else {
                            j--;
                        }
                    } else if (x.equals("day")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].*")) {
                            xValue = value.substring(0, 10);
                            break;
                        }
//							else if (value.matches("^BC[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].*")) {
//								xValue = value.substring(0,12);
//								break;
//							}
                        else {
                            j--;
                        }
                    } else if (x.equals("week")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9].*")) {
                            for (MatchResult r : Toolbox.findMatches(Pattern.compile("^(([0-9][0-9][0-9][0-9])-[0-9][0-9]-[0-9][0-9]).*"), value)) {
                                xValue = r.group(2) + "-W" + DateCalculator.getWeekOfDate(r.group(1));
                                break;
                            }
                            break;
                        } else if (value.matches("^[0-9][0-9][0-9][0-9]-W[0-9][0-9].*")) {
                            for (MatchResult r : Toolbox.findMatches(Pattern.compile("^([0-9][0-9][0-9][0-9]-W[0-9][0-9]).*"), value)) {
                                xValue = r.group(1);
                                break;
                            }
                            break;
                        }
                        // TODO check what to do for BC times
                        else {
                            j--;
                        }
                    } else if (x.equals("quarter")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9].*")) {
                            String month = value.substring(5, 7);
                            String quarter = nm.getFromNormMonthInQuarter(month);
                            if (quarter == null) {
                                quarter = "1";
                            }
                            xValue = value.substring(0, 4) + "-Q" + quarter;
                            break;
                        } else if (value.matches("^[0-9][0-9][0-9][0-9]-Q[1234].*")) {
                            xValue = value.substring(0, 7);
                            break;
                        }
                        // TODO check what to do for BC times
                        else {
                            j--;
                        }
                    } else if (x.equals("dateQuarter")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9]-Q[1234].*")) {
                            xValue = value.substring(0, 7);
                            break;
                        }
                        // TODO check what to do for BC times
                        else {
                            j--;
                        }
                    } else if (x.equals("season")) {
                        if (value.matches("^[0-9][0-9][0-9][0-9]-[0-9][0-9].*")) {
                            String month = value.substring(5, 7);
                            String season = nm.getFromNormMonthInSeason(month);
                            xValue = value.substring(0, 4) + "-" + season;
                            break;
                        }
//							else if (value.matches("^BC[0-9][0-9][0-9][0-9]-[0-9][0-9].*")) {
//								String month   = value.substring(7,9);
//								String season = nm.getFromNormMonthInSeason(month);
//								xValue = value.substring(0,6)+"-"+season;
//								break;
//							}
                        else if (value.matches("^[0-9][0-9][0-9][0-9]-(SP|SU|FA|WI).*")) {
                            xValue = value.substring(0, 7);
                            break;
                        }
//							else if (value.matches("^BC[0-9][0-9][0-9][0-9]-(SP|SU|FA|WI).*")) {
//								xValue = value.substring(0,9);
//								break;
//							}
                        else {
                            j--;
                        }
                    }
                } else {
                    j--;
                }
            } else {
                j--;
            }
        }
        return xValue;
    }

    /**
     * Get the last tense used in the sentence
     *
     * @param timex timex construct to discover tense data for
     * @return string that contains the tense
     */
    public static String getLastTense(Timex3 timex, JCas jcas, Language language) {
        RePatternManager rpm = RePatternManager.getInstance(language, false);

        String lastTense = "";

        // Get the sentence
        FSIterator iterSentence = jcas.getAnnotationIndex(Sentence.type).iterator();
        Sentence s = new Sentence(jcas);
        while (iterSentence.hasNext()) {
            s = (Sentence) iterSentence.next();
            if ((s.getBegin() <= timex.getBegin())
                    && (s.getEnd() >= timex.getEnd())) {
                break;
            }
        }

        // Get the tokens
        TreeMap<Integer, Token> tmToken = new TreeMap<Integer, Token>();
        FSIterator iterToken = jcas.getAnnotationIndex(Token.type).subiterator(s);
        while (iterToken.hasNext()) {
            Token token = (Token) iterToken.next();
            tmToken.put(token.getEnd(), token);
        }

        // Get the last VERB token
        for (Integer tokEnd : tmToken.keySet()) {
            if (tokEnd < timex.getBegin()) {
                Token token = tmToken.get(tokEnd);

                Logger.printDetail("GET LAST TENSE: string:" + token.getCoveredText() + " pattern:" + token.getPos());
                Logger.printDetail("hmAllRePattern.containsKey(tensePos4PresentFuture):" + rpm.get("tensePos4PresentFuture"));
                Logger.printDetail("hmAllRePattern.containsKey(tensePos4Future):" + rpm.get("tensePos4Future"));
                Logger.printDetail("hmAllRePattern.containsKey(tensePos4Past):" + rpm.get("tensePos4Past"));
                Logger.printDetail("CHECK TOKEN:" + token.getPos());

                if (token.getPos() == null) {

                } else if ((rpm.containsKey("tensePos4PresentFuture")) && (token.getPos().getPosValue().matches(rpm.get("tensePos4PresentFuture")))) {
                    lastTense = "PRESENTFUTURE";
                    Logger.printDetail("this tense:" + lastTense);
                } else if ((rpm.containsKey("tensePos4Past")) && (token.getPos().getPosValue().matches(rpm.get("tensePos4Past")))) {
                    lastTense = "PAST";
                    Logger.printDetail("this tense:" + lastTense);
                } else if ((rpm.containsKey("tensePos4Future")) && (token.getPos().getPosValue().matches(rpm.get("tensePos4Future")))) {
                    if (token.getCoveredText().matches(rpm.get("tenseWord4Future"))) {
                        lastTense = "FUTURE";
                        Logger.printDetail("this tense:" + lastTense);
                    }
                }
                if (token.getCoveredText().equals("since")) {
                    lastTense = "PAST";
                    Logger.printDetail("this tense:" + lastTense);
                }
                if (token.getCoveredText().equals("depuis")) { // French
                    lastTense = "PAST";
                    Logger.printDetail("this tense:" + lastTense);
                }
            }
            if (lastTense.equals("")) {
                if (tokEnd > timex.getEnd()) {
                    Token token = tmToken.get(tokEnd);

                    Logger.printDetail("GET NEXT TENSE: string:" + token.getCoveredText() + " pattern:" + token.getPos());
                    Logger.printDetail("hmAllRePattern.containsKey(tensePos4PresentFuture):" + rpm.get("tensePos4PresentFuture"));
                    Logger.printDetail("hmAllRePattern.containsKey(tensePos4Future):" + rpm.get("tensePos4Future"));
                    Logger.printDetail("hmAllRePattern.containsKey(tensePos4Past):" + rpm.get("tensePos4Past"));
                    Logger.printDetail("CHECK TOKEN:" + token.getPos());

                    if (token.getPos() == null) {

                    } else if ((rpm.containsKey("tensePos4PresentFuture")) && (token.getPos().getPosValue().matches(rpm.get("tensePos4PresentFuture")))) {
                        lastTense = "PRESENTFUTURE";
                        Logger.printDetail("this tense:" + lastTense);
                    } else if ((rpm.containsKey("tensePos4Past")) && (token.getPos().getPosValue().matches(rpm.get("tensePos4Past")))) {
                        lastTense = "PAST";
                        Logger.printDetail("this tense:" + lastTense);
                    } else if ((rpm.containsKey("tensePos4Future")) && (token.getPos().getPosValue().matches(rpm.get("tensePos4Future")))) {
                        if (token.getCoveredText().matches(rpm.get("tenseWord4Future"))) {
                            lastTense = "FUTURE";
                            Logger.printDetail("this tense:" + lastTense);
                        }
                    }
                }
            }
        }
        // check for double POS Constraints (not included in the rule language, yet) TODO
        // VHZ VNN and VHZ VNN and VHP VNN and VBP VVN
        String prevPos = "";
        String longTense = "";
        if (lastTense.equals("PRESENTFUTURE")) {
            for (Integer tokEnd : tmToken.keySet()) {
                Token token = tmToken.get(tokEnd);
                POS thisPos = token.getPos();
                String thisPosValue = thisPos == null ? "" : thisPos.getPosValue();

                if (tokEnd < timex.getBegin()) {
                    if (("VHZ".equals(prevPos)) || ("VBZ".equals(prevPos)) || ("VHP".equals(prevPos)) || ("VBP".equals(prevPos))
                            || (prevPos.equals("VER:pres"))) {
                        if ("VVN".equals(thisPosValue) || "VER:pper".equals(thisPosValue)) {
                            if ((!(token.getCoveredText().equals("expected"))) && (!(token.getCoveredText().equals("scheduled")))) {
                                lastTense = "PAST";
                                longTense = "PAST";
                                Logger.printDetail("this tense:" + lastTense);
                            }
                        }
                    }
                    prevPos = thisPosValue;
                }
                if (longTense.equals("")) {
                    if (tokEnd > timex.getEnd()) {
                        if (("VHZ".equals(prevPos)) || ("VBZ".equals(prevPos)) || ("VHP".equals(prevPos)) || ("VBP".equals(prevPos))
                                || ("VER:pres".equals(prevPos))) {
                            if ("VVN".equals(thisPosValue) || "VER:pper".equals(thisPosValue)) {
                                if ((!(token.getCoveredText().equals("expected"))) && (!(token.getCoveredText().equals("scheduled")))) {
                                    lastTense = "PAST";
                                    longTense = "PAST";
                                    Logger.printDetail("this tense:" + lastTense);
                                }
                            }
                        }
                        prevPos = thisPosValue;
                    }
                }
            }
        }
        // French: VER:pres VER:pper
        if (lastTense.equals("PAST")) {
            for (Integer tokEnd : tmToken.keySet()) {
                Token token = tmToken.get(tokEnd);
                POS thisPos = token.getPos();
                String thisPosValue = thisPos == null ? "" : thisPos.getPosValue();

                if (tokEnd < timex.getBegin() || (longTense.isEmpty() && tokEnd > timex.getEnd())) {
                    if (("VER:pres".equals(prevPos)) && ("VER:pper".equals(thisPosValue))) {
                        if (((token.getCoveredText().matches("^prévue?s?$"))) || ((token.getCoveredText().equals("^envisagée?s?$")))) {
                            lastTense = "FUTURE";
                            longTense = "FUTURE";
                            Logger.printDetail("this tense:" + lastTense);
                        }
                    }
                    prevPos = thisPosValue;
                }
            }
        }
        Logger.printDetail("TENSE: " + lastTense);

        return lastTense;
    }

    public record SentenceContainer(String text, int begin, int end, List<Token> tokens) {
        public static SentenceContainer fromSentence(JCas jCas, Sentence sentence) {
            FSIterator<Annotation> subiterator = jCas.getAnnotationIndex(Token.type).subiterator(sentence);
            ArrayList<Token> tokens = new ArrayList<>();
            while (subiterator.hasNext()) {
                tokens.add((Token) subiterator.next());
            }
            return new ContextAnalyzer.SentenceContainer(
                    sentence.getCoveredText(),
                    sentence.getBegin(),
                    sentence.getEnd(),
                    Collections.unmodifiableList(tokens)
            );
        }
    }

    public static boolean checkSentenceMatch(SentenceContainer sentence, int matchStart, int matchEnd) {
        return ContextAnalyzer.checkPrefixSuffix(sentence.text, matchStart, matchEnd)
                && ContextAnalyzer.checkTokenBoundaries(sentence, matchStart, matchEnd);
    }

    /**
     * Check preceding and succeeding strings to exclude common false positives.
     */
    public static Boolean checkPrefixSuffix(String coveredText, int matchStart, int matchEnd) {
        if (matchStart > 0) {
            // Check the prefix for expressions such as "1999" in 53453.1999
            String prefix = coveredText.substring(0, matchStart);
            if (PATTERN_EXCLUDE_PREFIX.matcher(prefix).find()) {
                return false;
            }
        }

        if (matchEnd < coveredText.length()) {
            String suffix = coveredText.substring(matchEnd);
            if (PATTERN_EXCLUDE_SUFFIX.matcher(suffix).find()) {
                return false;
            }
        }

        return true;
    }


    public final static Pattern PATTERN_PUNCT = Pattern.compile("\\p{P}", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Check token boundaries using token information
     *
     * @return Whether the MatchResult is clean
     */
    public static Boolean checkTokenBoundaries(SentenceContainer sentence, int start, int end) {
        boolean beginOK = false;
        boolean endOK = false;

        // whole expression is marked as a sentence
        if ((end - start) == (sentence.end - sentence.begin)) {
            return true;
        }

        // Only check Token boundaries if no white-spaces in front of and behind the match-result
        if (
                ((start == 0) || (sentence.text.charAt(start - 1) == ' '))
                && ((end == sentence.text.length()) || (sentence.text.charAt(end) == ' '))
        ) {
            return true;
        } else {
            // other token boundaries than white-spaces
            for (Annotation token : sentence.tokens) {
                // Check begin
                if ((start + sentence.begin) == token.getBegin() || (
                        // Tokenizer does not split number from some symbols (".", "/", "-", "–"),
                        // e.g., "...12 August-24 Augsut..."
                        start > 0 && PATTERN_PUNCT.matcher(sentence.text.substring(start - 1, start)).matches()
                )) {
                    beginOK = true;
                }

                // Check end
                if ((end + sentence.begin) == token.getEnd() || (
                        // Tokenizer does not split number from some symbols (".", "/", "-", "–"),
                        // e.g., "... in 1990. New Sentence ..."
                        end < sentence.text.length() && PATTERN_PUNCT.matcher(sentence.text.substring(end, end + 1)).matches()
                )) {
                    endOK = true;
                }

                if (beginOK && endOK)
                    return true;
            }
        }
        return false;
    }

}
