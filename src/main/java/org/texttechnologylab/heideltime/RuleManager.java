package org.texttechnologylab.heideltime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.unihd.dbs.uima.annotator.heideltime.resources.*;
import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class fills the role of a manager of all the rule resources. It reads
 * the data from a file system and fills up a bunch of HashMaps with their
 * information.
 *
 * @author jannik stroetgen
 */
public class RuleManager {
    protected static HashMap<String, RuleSet> instances = new HashMap<String, RuleSet>();

    /**
     * singleton producer.
     *
     * @return singleton instance of RuleManager
     */
    public static RuleSet getRuleSet(Language language, Boolean load_temponym_resources) {
        if (!instances.containsKey(language.getName())) {
            RuleSet rules = new RuleReader(language.getResourceFolder(), load_temponym_resources).getRuleSet();
            instances.put(language.getName(), rules);
        }

        return instances.get(language.getName());
    }

    public record PosConstraint(int group, Pattern pattern) {
        public static final Pattern PATTERN_CONSTRAINT = Pattern.compile("group\\(([0-9]+)\\):(.*?):");

        public static List<PosConstraint> fromRule(String constraint) {
            return Utils.findMatches(PATTERN_CONSTRAINT, constraint).stream()
                    .map(matchResult ->
                            new PosConstraint(Integer.parseInt(matchResult.group(1)), Pattern.compile(matchResult.group(2)))
                    ).toList();
        }

        public boolean matches(String pos) {
            return Objects.nonNull(pos) && pattern.matcher(pos).matches();
        }
    }

    public record Offset(int start, int end) {
        public static final Pattern PATTERN_OFFSET = Pattern.compile("group\\(([0-9]+)\\)-group\\(([0-9]+)\\)");

        public static Offset fromRule(String rule) {
            if (rule != null && !rule.isEmpty()) {
                Matcher matcher = PATTERN_OFFSET.matcher(rule);
                if (matcher.find()) {
                    MatchResult mr = matcher.toMatchResult();
                    int startOffset = Integer.parseInt(mr.group(1));
                    int endOffset = Integer.parseInt(mr.group(2));
                    return new Offset(startOffset, endOffset);
                }
            }

            return new Offset(0, 0);
        }
    }

    public record RuleInstance(
            String type,
            String name,
            Pattern pattern,
            Pattern patternFast,
            String normalization,
            Offset offset,
            String quant,
            String freq,
            String mod,
            List<PosConstraint> constraints,
            String empty
    ) {
        public boolean fastCheck(String string) {
            return patternFast == null || !patternFast.matcher(string).find();
        }


        /**
         * Check whether the part of speech constraint defined in a rule is satisfied.
         */
        public boolean checkPosConstraint(ContextAnalyzer.SentenceContainer sentence, MatchResult matchResult) {
            for (PosConstraint constraint : constraints) {
                int tokenBegin = sentence.begin() + matchResult.start(constraint.group);
                int tokenEnd = sentence.begin() + matchResult.end(constraint.group);
                String pos_as_is = getPosFromMatchResult(sentence, tokenBegin);
                if (constraint.matches(pos_as_is)) {
                    Logger.printDetail("POS CONSTRAINT IS VALID: pattern should be " + constraint.pattern.pattern() + " and is " + pos_as_is);
                } else {
                    Logger.printDetail("POS CONSTRAINT INVALID: pattern should be " + constraint.pattern.pattern() + " and is " + pos_as_is);
                    return false;
                }
            }
            return true;
        }

        /**
         * Identify the part of speech (POS) of a MarchResult.
         */
        public static String getPosFromMatchResult(ContextAnalyzer.SentenceContainer sentence, int tokBegin) {
            for (Token token : sentence.tokens()) {
                if (token.getBegin() == tokBegin) {
                    POS pos = token.getPos();
                    return pos == null ? "" : pos.getPosValue();
                }
            }
            return "";
        }


    }

    public record RuleSet(
            TreeMap<String, RuleInstance> dates,
            TreeMap<String, RuleInstance> durations,
            TreeMap<String, RuleInstance> times,
            TreeMap<String, RuleInstance> sets,
            TreeMap<String, RuleInstance> temponyms
    ) {
    }

    protected static class RuleReader extends GenericResourceManager {
        // PATTERNS TO READ RESOURCES "RULES" AND "NORMALIZATION"
        Pattern paReadRules = Pattern.compile("RULENAME=\"(.*?)\",EXTRACTION=\"(.*?)\",NORM_VALUE=\"(.*?)\"(.*)");

        // EXTRACTION PARTS OF RULES (patterns loaded from files)
        TreeMap<String, Pattern> hmDatePattern = new TreeMap<String, Pattern>();
        TreeMap<String, Pattern> hmDurationPattern = new TreeMap<String, Pattern>();
        TreeMap<String, Pattern> hmTimePattern = new TreeMap<String, Pattern>();
        TreeMap<String, Pattern> hmSetPattern = new TreeMap<String, Pattern>();
        TreeMap<String, Pattern> hmTemponymPattern = new TreeMap<String, Pattern>();


        // NORMALIZATION PARTS OF RULES (patterns loaded from files)
        HashMap<String, String> hmDateNormalization = new HashMap<String, String>();
        HashMap<String, String> hmTimeNormalization = new HashMap<String, String>();
        HashMap<String, String> hmDurationNormalization = new HashMap<String, String>();
        HashMap<String, String> hmSetNormalization = new HashMap<String, String>();
        HashMap<String, String> hmTemponymNormalization = new HashMap<String, String>();

        // OFFSET PARTS OF RULES (patterns loaded from files)
        HashMap<String, String> hmDateOffset = new HashMap<String, String>();
        HashMap<String, String> hmTimeOffset = new HashMap<String, String>();
        HashMap<String, String> hmDurationOffset = new HashMap<String, String>();
        HashMap<String, String> hmSetOffset = new HashMap<String, String>();
        HashMap<String, String> hmTemponymOffset = new HashMap<String, String>();

        // QUANT PARTS OF RULES (patterns loaded from files)
        HashMap<String, String> hmDateQuant = new HashMap<String, String>();
        HashMap<String, String> hmTimeQuant = new HashMap<String, String>();
        HashMap<String, String> hmDurationQuant = new HashMap<String, String>();
        HashMap<String, String> hmSetQuant = new HashMap<String, String>();
        HashMap<String, String> hmTemponymQuant = new HashMap<String, String>();

        // FREQ PARTS OF RULES (patterns loaded from files)
        HashMap<String, String> hmDateFreq = new HashMap<String, String>();
        HashMap<String, String> hmTimeFreq = new HashMap<String, String>();
        HashMap<String, String> hmDurationFreq = new HashMap<String, String>();
        HashMap<String, String> hmSetFreq = new HashMap<String, String>();
        HashMap<String, String> hmTemponymFreq = new HashMap<String, String>();

        // MOD PARTS OF RULES (patterns loaded from files)
        HashMap<String, String> hmDateMod = new HashMap<String, String>();
        HashMap<String, String> hmTimeMod = new HashMap<String, String>();
        HashMap<String, String> hmDurationMod = new HashMap<String, String>();
        HashMap<String, String> hmSetMod = new HashMap<String, String>();
        HashMap<String, String> hmTemponymMod = new HashMap<String, String>();

        // POS PARTS OF RULES (patterns loaded from files)
        HashMap<String, String> hmDatePosConstraint = new HashMap<String, String>();
        HashMap<String, String> hmTimePosConstraint = new HashMap<String, String>();
        HashMap<String, String> hmDurationPosConstraint = new HashMap<String, String>();
        HashMap<String, String> hmSetPosConstraint = new HashMap<String, String>();
        HashMap<String, String> hmTemponymPosConstraint = new HashMap<String, String>();

        // EMPTYVALUE part of rules
        HashMap<String, String> hmDateEmptyValue = new HashMap<String, String>();
        HashMap<String, String> hmTimeEmptyValue = new HashMap<String, String>();
        HashMap<String, String> hmDurationEmptyValue = new HashMap<String, String>();
        HashMap<String, String> hmSetEmptyValue = new HashMap<String, String>();
        HashMap<String, String> hmTemponymEmptyValue = new HashMap<String, String>();

        // FASTCHECK part of rules
        HashMap<String, Pattern> hmDateFastCheck = new HashMap<String, Pattern>();
        HashMap<String, Pattern> hmTimeFastCheck = new HashMap<String, Pattern>();
        HashMap<String, Pattern> hmDurationFastCheck = new HashMap<String, Pattern>();
        HashMap<String, Pattern> hmSetFastCheck = new HashMap<String, Pattern>();
        HashMap<String, Pattern> hmTemponymFastCheck = new HashMap<String, Pattern>();

        /**
         * Constructor calls the parent constructor that sets language/resource
         * parameters and collects rules resources.
         *
         * @param language                language of resources to be used
         * @param load_temponym_resources whether temponyms resources are loaded
         */
        protected RuleReader(String language, Boolean load_temponym_resources) {
            super("rules", language);

            // /////////////////////////////////////////////////
            // READ RULE RESOURCES FROM FILES AND STORE THEM //
            // /////////////////////////////////////////////////
            ResourceScanner rs = ResourceScanner.getInstance();
            ResourceMap hmResourcesRules = rs.getRules(language);
            readRules(hmResourcesRules, language, load_temponym_resources);
        }

        /**
         * READ THE RULES FROM THE FILES. The files have to be defined in the
         * HashMap hmResourcesRules.
         *
         * @param hmResourcesRules        rules to be interpreted
         * @param load_temponym_resources whether temponyms resources are loaded
         */
        public void readRules(ResourceMap hmResourcesRules, String language, Boolean load_temponym_resources) {
            InputStream is = null;
            InputStreamReader isr = null;
            BufferedReader br = null;

            LinkedList<String> resourceKeys = new LinkedList<String>(hmResourcesRules.keySet());

            // sort DATE > TIME > DURATION > SET > rest
            resourceKeys.sort((arg0, arg1) -> {
                if ("daterules".equals(arg0)) {
                    return -1;
                } else if ("timerules".equals(arg0) && !"daterules".equals(arg1)) {
                    return -1;
                } else if ("durationrules".equals(arg0) && !"daterules".equals(arg1) && !"timerules".equals(arg1)) {
                    return -1;
                } else if ("setrules".equals(arg0) && !"daterules".equals(arg1) && !"timerules".equals(arg1) && !"durationrules".equals(arg1)) {
                    return -1;
                }
                return 1;
            });

            try {
                for (String resource : resourceKeys) {
                    is = hmResourcesRules.getInputStream(resource);
                    isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                    br = new BufferedReader(isr);

                    Logger.printDetail(component, "Adding rule resource: " + resource);
                    for (String line; (line = br.readLine()) != null; ) {
                        // skip comments or empty lines in resource files
                        if (line.startsWith("//") || line.isEmpty())
                            continue;

                        boolean correctLine = false;
                        Logger.printDetail("DEBUGGING: reading rules..." + line);
                        // check each line for the name, extraction, and
                        // normalization part, others are optional
                        for (MatchResult r : Utils.findMatches(paReadRules, line)) {
                            correctLine = true;
                            String rule_name = r.group(1);
                            String rule_extraction = replaceSpaces(r.group(2));
                            String rule_normalization = r.group(3);
                            String rule_offset = "";
                            String rule_quant = "";
                            String rule_freq = "";
                            String rule_mod = "";
                            String pos_constraint = "";
                            String rule_empty_value = "";
                            String rule_fast_check = "";

                            // throw an error if the rule's name already exists
                            if (hmDatePattern.containsKey(rule_name) ||
                                    hmDurationPattern.containsKey(rule_name) ||
                                    hmSetPattern.containsKey(rule_name) ||
                                    hmTimePattern.containsKey(rule_name)) {
                                Logger.printError("WARNING: Duplicate rule name detected. This rule is being ignored:");
                                Logger.printError(line);
                            }

                            // //////////////////////////////////////////////////////////////////
                            // RULE EXTRACTION PARTS ARE TRANSLATED INTO REGULAR
                            // EXPRESSSIONS //
                            // //////////////////////////////////////////////////////////////////
                            // create pattern for rule extraction part
                            Pattern paVariable = Pattern.compile("%(re[a-zA-Z0-9]*)");
                            RePatternManager rpm = RePatternManager.getInstance(Language.getLanguageFromString(language), load_temponym_resources);
                            for (MatchResult mr : Utils.findMatches(paVariable, rule_extraction)) {
                                Logger.printDetail("DEBUGGING: replacing patterns..." + mr.group());
                                if (!(rpm.containsKey(mr.group(1)))) {
                                    Logger.printError("Error creating rule:" + rule_name);
                                    Logger.printError("The following pattern used in this rule does not exist, does it? %" + mr.group(1));
                                    System.exit(-1);
                                }
                                rule_extraction = rule_extraction.replaceAll("%" + mr.group(1), rpm.get(mr.group(1)));
                            }
                            rule_extraction = rule_extraction.replaceAll(" ", "[\\\\s]+");
                            Pattern pattern = null;
                            try {
                                pattern = Pattern.compile(rule_extraction);
                            } catch (java.util.regex.PatternSyntaxException e) {
                                Logger.printError("Compiling rules resulted in errors.");
                                Logger.printError("Problematic rule is " + rule_name);
                                Logger.printError("Cannot compile pattern: " + rule_extraction);
                                e.printStackTrace();
                                System.exit(-1);
                            }
                            // Pattern pattern = Pattern.compile(rule_extraction);

                            // ///////////////////////////////////
                            // CHECK FOR ADDITIONAL CONSTRAINS //
                            // ///////////////////////////////////
                            Pattern patternFast = null;
                            if (!(r.group(4) == null)) {
                                if (r.group(4).contains("OFFSET")) {
                                    Pattern paOffset = Pattern
                                            .compile("OFFSET=\"(.*?)\"");
                                    for (MatchResult ro : Utils.findMatches(
                                            paOffset, line)) {
                                        rule_offset = ro.group(1);
                                    }
                                }
                                if (r.group(4).contains("NORM_QUANT")) {
                                    Pattern paQuant = Pattern
                                            .compile("NORM_QUANT=\"(.*?)\"");
                                    for (MatchResult rq : Utils.findMatches(
                                            paQuant, line)) {
                                        rule_quant = rq.group(1);
                                    }
                                }
                                if (r.group(4).contains("NORM_FREQ")) {
                                    Pattern paFreq = Pattern
                                            .compile("NORM_FREQ=\"(.*?)\"");
                                    for (MatchResult rf : Utils.findMatches(
                                            paFreq, line)) {
                                        rule_freq = rf.group(1);
                                    }
                                }
                                if (r.group(4).contains("NORM_MOD")) {
                                    Pattern paMod = Pattern
                                            .compile("NORM_MOD=\"(.*?)\"");
                                    for (MatchResult rf : Utils.findMatches(
                                            paMod, line)) {
                                        rule_mod = rf.group(1);
                                    }
                                }
                                if (r.group(4).contains("POS_CONSTRAINT")) {
                                    Pattern paPos = Pattern
                                            .compile("POS_CONSTRAINT=\"(.*?)\"");
                                    for (MatchResult rp : Utils.findMatches(
                                            paPos, line)) {
                                        pos_constraint = rp.group(1);
                                    }
                                }
                                if (r.group(4).contains("EMPTY_VALUE")) {
                                    Pattern paEmpty = Pattern
                                            .compile("EMPTY_VALUE=\"(.*?)\"");
                                    for (MatchResult rp : Utils.findMatches(
                                            paEmpty, line)) {
                                        rule_empty_value = rp.group(1);
                                    }
                                }
                                if (r.group(4).contains("FAST_CHECK")) {
                                    Pattern paFast = Pattern
                                            .compile("FAST_CHECK=\"(.*?)\"");
                                    for (MatchResult rp : Utils.findMatches(
                                            paFast, line)) {
                                        rule_fast_check = rp.group(1);
                                        // create pattern for rule fast check part -- similar to extraction part
                                        // thus using paVariable and rpm
                                        for (MatchResult mr : Utils.findMatches(paVariable, rule_fast_check)) {
                                            Logger.printDetail("DEBUGGING: replacing patterns..." + mr.group());
                                            if (!(rpm.containsKey(mr.group(1)))) {
                                                Logger.printError("Error creating rule:" + rule_name);
                                                Logger.printError("The following pattern used in this rule does not exist, does it? %" + mr.group(1));
                                                System.exit(-1);
                                            }
                                            rule_fast_check = rule_fast_check.replaceAll("%" + mr.group(1), rpm.get(mr.group(1)));
                                        }
                                        rule_fast_check = rule_fast_check.replaceAll(" ", "[\\\\s]+");
                                        patternFast = null;
                                        try {
                                            patternFast = Pattern.compile(rule_fast_check);
                                        } catch (java.util.regex.PatternSyntaxException e) {
                                            Logger.printError("Compiling rules resulted in errors.");
                                            Logger.printError("Problematic rule is " + rule_name);
                                            Logger.printError("Cannot compile pattern: " + rule_fast_check);
                                            e.printStackTrace();
                                            System.exit(-1);
                                        }
                                    }
                                }
                            }

                            // ///////////////////////////////////////////
                            // READ DATE RULES AND MAKE THEM AVAILABLE //
                            // ///////////////////////////////////////////
                            if (resource.equals("daterules")) {
                                // get extraction part
                                hmDatePattern.put(rule_name, pattern);
                                // get normalization part
                                hmDateNormalization.put(rule_name,
                                        rule_normalization);
                                // get offset part
                                if (!(rule_offset.isEmpty())) {
                                    hmDateOffset.put(rule_name, rule_offset);
                                }
                                // get quant part
                                if (!(rule_quant.isEmpty())) {
                                    hmDateQuant.put(rule_name, rule_quant);
                                }
                                // get freq part
                                if (!(rule_freq.isEmpty())) {
                                    hmDateFreq.put(rule_name, rule_freq);
                                }
                                // get mod part
                                if (!(rule_mod.isEmpty())) {
                                    hmDateMod.put(rule_name, rule_mod);
                                }
                                // get pattern constraint part
                                if (!(pos_constraint.isEmpty())) {
                                    hmDatePosConstraint.put(rule_name,
                                            pos_constraint);
                                }
                                // get empty value part
                                if (!(rule_empty_value.isEmpty())) {
                                    hmDateEmptyValue.put(rule_name,
                                            rule_empty_value);
                                }
                                // get fast check part
                                if (!(rule_fast_check.isEmpty())) {
                                    hmDateFastCheck.put(rule_name,
                                            patternFast);
                                }
                            }

                            // ///////////////////////////////////////////////
                            // READ DURATION RULES AND MAKE THEM AVAILABLE //
                            // ///////////////////////////////////////////////
                            else if (resource.equals("durationrules")) {
                                // get extraction part
                                hmDurationPattern.put(rule_name, pattern);
                                // get normalization part
                                hmDurationNormalization.put(rule_name,
                                        rule_normalization);
                                // get offset part
                                if (!(rule_offset.isEmpty())) {
                                    hmDurationOffset.put(rule_name, rule_offset);
                                }
                                // get quant part
                                if (!(rule_quant.isEmpty())) {
                                    hmDurationQuant.put(rule_name, rule_quant);
                                }
                                // get freq part
                                if (!(rule_freq.isEmpty())) {
                                    hmDurationFreq.put(rule_name, rule_freq);
                                }
                                // get mod part
                                if (!(rule_mod.isEmpty())) {
                                    hmDurationMod.put(rule_name, rule_mod);
                                }
                                // get pattern constraint part
                                if (!(pos_constraint.isEmpty())) {
                                    hmDurationPosConstraint.put(rule_name,
                                            pos_constraint);
                                }
                                // get empty value part
                                if (!(rule_empty_value.isEmpty())) {
                                    hmDurationEmptyValue.put(rule_name,
                                            rule_empty_value);
                                }
                                // get fast check part
                                if (!(rule_fast_check.isEmpty())) {
                                    hmDurationFastCheck.put(rule_name,
                                            patternFast);
                                }
                            }

                            // //////////////////////////////////////////
                            // READ SET RULES AND MAKE THEM AVAILABLE //
                            // //////////////////////////////////////////
                            else if (resource.equals("setrules")) {
                                // get extraction part
                                hmSetPattern.put(rule_name, pattern);
                                // get normalization part
                                hmSetNormalization.put(rule_name,
                                        rule_normalization);
                                // get offset part
                                if (!rule_offset.isEmpty()) {
                                    hmSetOffset.put(rule_name, rule_offset);
                                }
                                // get quant part
                                if (!rule_quant.isEmpty()) {
                                    hmSetQuant.put(rule_name, rule_quant);
                                }
                                // get freq part
                                if (!rule_freq.isEmpty()) {
                                    hmSetFreq.put(rule_name, rule_freq);
                                }
                                // get mod part
                                if (!rule_mod.isEmpty()) {
                                    hmSetMod.put(rule_name, rule_mod);
                                }
                                // get pattern constraint part
                                if (!pos_constraint.isEmpty()) {
                                    hmSetPosConstraint.put(rule_name,
                                            pos_constraint);
                                }
                                // get empty value part
                                if (!(rule_empty_value.isEmpty())) {
                                    hmSetEmptyValue.put(rule_name,
                                            rule_empty_value);
                                }
                                // get fast check part
                                if (!(rule_fast_check.isEmpty())) {
                                    hmSetFastCheck.put(rule_name,
                                            patternFast);
                                }
                            }

                            // ///////////////////////////////////////////
                            // READ TIME RULES AND MAKE THEM AVAILABLE //
                            // ///////////////////////////////////////////
                            else if (resource.equals("timerules")) {
                                // get extraction part
                                hmTimePattern.put(rule_name, pattern);
                                // get normalization part
                                hmTimeNormalization.put(rule_name,
                                        rule_normalization);
                                // get offset part
                                if (!rule_offset.isEmpty()) {
                                    hmTimeOffset.put(rule_name, rule_offset);
                                }
                                // get quant part
                                if (!rule_quant.isEmpty()) {
                                    hmTimeQuant.put(rule_name, rule_quant);
                                }
                                // get freq part
                                if (!rule_freq.isEmpty()) {
                                    hmTimeFreq.put(rule_name, rule_freq);
                                }
                                // get mod part
                                if (!rule_mod.isEmpty()) {
                                    hmTimeMod.put(rule_name, rule_mod);
                                }
                                // get pattern constraint part
                                if (!pos_constraint.isEmpty()) {
                                    hmTimePosConstraint.put(rule_name,
                                            pos_constraint);
                                }
                                // get empty value part
                                if (!(rule_empty_value.isEmpty())) {
                                    hmTimeEmptyValue.put(rule_name,
                                            rule_empty_value);
                                }
                                // get fast check part
                                if (!(rule_fast_check.isEmpty())) {
                                    hmTimeFastCheck.put(rule_name,
                                            patternFast);
                                }
                            }
                            // //////////////////////////////////////////////
                            // READ TEMPONYM RULES AND MAKE THEM AVAILABLE //
                            // //////////////////////////////////////////////
                            else if (resource.equals("temponymrules")) {
                                // get extraction part
                                hmTemponymPattern.put(rule_name, pattern);
                                // get normalization part
                                hmTemponymNormalization.put(rule_name,
                                        rule_normalization);
                                // get offset part
                                if (!(rule_offset.isEmpty())) {
                                    hmTemponymOffset.put(rule_name, rule_offset);
                                }
                                // get quant part
                                if (!(rule_quant.isEmpty())) {
                                    hmTemponymQuant.put(rule_name, rule_quant);
                                }
                                // get freq part
                                if (!(rule_freq.isEmpty())) {
                                    hmTemponymFreq.put(rule_name, rule_freq);
                                }
                                // get mod part
                                if (!(rule_mod.isEmpty())) {
                                    hmTemponymMod.put(rule_name, rule_mod);
                                }
                                // get pattern constraint part
                                if (!(pos_constraint.isEmpty())) {
                                    hmTemponymPosConstraint.put(rule_name,
                                            pos_constraint);
                                }
                                // get empty value part
                                if (!(rule_empty_value.isEmpty())) {
                                    hmTemponymEmptyValue.put(rule_name,
                                            rule_empty_value);
                                }
                                // get fast check part
                                if (!(rule_fast_check.isEmpty())) {
                                    hmTemponymFastCheck.put(rule_name,
                                            patternFast);
                                }
                            } else {
                                Logger.printDetail(component, "Resource not recognized by HeidelTime: " + resource);
                            }
                        }

                        // /////////////////////////////////////////
                        // CHECK FOR PROBLEMS WHEN READING RULES //
                        // /////////////////////////////////////////
                        if (!correctLine) {
                            Logger.printError(component, "Cannot read the following line of rule resource " + resource);
                            Logger.printError(component, "Line: " + line);
                        }

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                    if (isr != null) {
                        isr.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private TreeMap<String, RuleInstance> getRulesFor(final String type, TreeMap<String, Pattern> patterns, HashMap<String, Pattern> fastPatterns, HashMap<String, String> normalization, HashMap<String, String> offset, HashMap<String, String> quant, HashMap<String, String> freq, HashMap<String, String> mod, HashMap<String, String> constraint, HashMap<String, String> emptyValue) {
            TreeMap<String, RuleInstance> rules = new TreeMap<>();
            patterns.forEach(
                    (name, pattern) -> rules.put(
                            name, new RuleInstance(
                                    type,
                                    name,
                                    pattern,
                                    fastPatterns.get(name),
                                    normalization.get(name),
                                    Offset.fromRule(offset.get(name)),
                                    quant.get(name),
                                    freq.get(name),
                                    mod.get(name),
                                    PosConstraint.fromRule(constraint.get(name)),
                                    emptyValue.get(name)
                            )
                    )
            );
            return rules;
        }

        public final TreeMap<String, RuleInstance> getDateRules() {
            return getRulesFor("DATE", hmDatePattern, hmDateFastCheck, hmDateNormalization, hmDateOffset, hmDateQuant, hmDateFreq, hmDateMod, hmDatePosConstraint, hmDateEmptyValue);
        }

        public final TreeMap<String, RuleInstance> getDurationRules() {
            return getRulesFor("DURATION", hmDurationPattern, hmDurationFastCheck, hmDurationNormalization, hmDurationOffset, hmDurationQuant, hmDurationFreq, hmDurationMod, hmDurationPosConstraint, hmDurationEmptyValue);
        }

        public final TreeMap<String, RuleInstance> getTimeRules() {
            return getRulesFor("TIME", hmTimePattern, hmTimeFastCheck, hmTimeNormalization, hmTimeOffset, hmTimeQuant, hmTimeFreq, hmTimeMod, hmTimePosConstraint, hmTimeEmptyValue);
        }

        public final TreeMap<String, RuleInstance> getSetRules() {
            return getRulesFor("SET", hmSetPattern, hmSetFastCheck, hmSetNormalization, hmSetOffset, hmSetQuant, hmSetFreq, hmSetMod, hmSetPosConstraint, hmSetEmptyValue);
        }

        public final TreeMap<String, RuleInstance> getTemponymRules() {
            return getRulesFor("TEMPONYM", hmTemponymPattern, hmTemponymFastCheck, hmTemponymNormalization, hmTemponymOffset, hmTemponymQuant, hmTemponymFreq, hmTemponymMod, hmTemponymPosConstraint, hmTemponymEmptyValue);
        }

        public final RuleSet getRuleSet() {
            return new RuleSet(
                    getDateRules(),
                    getDurationRules(),
                    getTimeRules(),
                    getSetRules(),
                    getTemponymRules()
            );
        }
    }
}
