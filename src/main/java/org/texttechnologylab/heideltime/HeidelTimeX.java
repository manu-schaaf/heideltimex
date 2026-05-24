/*
 * HeidelTime.java
 *
 * Copyright (c) 2011, Database Research Group, Institute of Computer Science, Heidelberg University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU General Public License.
 *
 * author: Jannik Strötgen
 * email:  stroetgen@uni-hd.de
 *
 * HeidelTime is a multilingual, cross-domain temporal tagger.
 * For details, see http://dbs.ifi.uni-heidelberg.de/heideltime
 */

package org.texttechnologylab.heideltime;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unihd.dbs.uima.annotator.heideltime.HeidelTime;
import de.unihd.dbs.uima.annotator.heideltime.ProcessorManager;
import de.unihd.dbs.uima.annotator.heideltime.ProcessorManager.Priority;
import de.unihd.dbs.uima.annotator.heideltime.processors.TemponymPostprocessing;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.annotator.heideltime.resources.NormalizationManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.RePatternManager;
import de.unihd.dbs.uima.annotator.heideltime.resources.RegexHashMap;
import de.unihd.dbs.uima.annotator.heideltime.utilities.DateCalculator;
import de.unihd.dbs.uima.annotator.heideltime.utilities.LocaleException;
import de.unihd.dbs.uima.types.heideltime.Dct;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author Manuel Schaaf, based on HeidelTime by jannik stroetgen
 */
public class HeidelTimeX extends HeidelTime {
    private int timexID = 0;

    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    /**
     * @see org.apache.uima.fit.component.JCasAnnotator_ImplBase#process(JCas)
     */
    public void process(JCas jcas) {
        String documentUri = "";
        try {
            DocumentMetaData documentMetaData = DocumentMetaData.get(jcas);
            documentUri = "documentUri=" + documentMetaData.getDocumentUri();
        } catch (Exception ignored) {
        }

        // check whether a given DCT (if any) is of the correct format and if not, skip this call
        if (!isValidDCT(jcas)) {

            getLogger().error(
                    """
                            Detected an incorrect DCT in current CAS. 
                            HeidleTimeX expects either \"YYYYMMDD\" or \"YYYY-MM-DD...\".
                            Skipping current document {}""",
                    documentUri
            );
            return;
        }

        // run preprocessing processors
        procMan.executeProcessors(jcas, Priority.PREPROCESSING);

        try {
            switch (jcas.getDocumentLanguage()) {
                case String lang when lang.toLowerCase().startsWith("en") -> {
                    Locale locale = DateCalculator.getLocaleFromString("en");
                    Locale.setDefault(locale);
                    language = Language.ENGLISH;
                }
                case String lang when lang.toLowerCase().startsWith("de") -> {
                    Locale locale = DateCalculator.getLocaleFromString("de");
                    Locale.setDefault(locale);
                    language = Language.GERMAN;
                }
                default -> {
                    language = Language.GERMAN;
                }
            }
        } catch (LocaleException e) {
            getLogger().error(e.getMessage());
            language = Language.GERMAN;
        }

        RuleManager.RuleSet ruleSet = RuleManager.getRuleSet(language, find_temponyms);

        timexID = 1; // reset counter once per document processing

        flagHistoricDates = false;

        ////////////////////////////////////////////
        // CHECK SENTENCE BY SENTENCE FOR TIMEXES //
        ////////////////////////////////////////////
        Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);
        if (sentences.isEmpty()) {
            getLogger().error(
                    "HeidleTimeX has not found any sentence tokens in this document. " +
                            "HeidleTimeX needs sentence tokens tagged by a preprocessing UIMA analysis engine to " +
                            "do its work. Please check your UIMA workflow and add an analysis engine that creates " +
                            "these sentence tokens."
            );
        }

        try (ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor()) {
            ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Sentence sentence : sentences) {
                CompletableFuture<ContextAnalyzer.SentenceContainer> containerFuture = CompletableFuture.supplyAsync(
                        () -> ContextAnalyzer.SentenceContainer.fromSentence(jcas, sentence),
                        threadPool
                );

                if (find_dates) {
                    for (RuleManager.RuleInstance rule : ruleSet.dates().values()) {
                        futures.add(findTimexInVirtualThread(jcas, rule, containerFuture, threadPool));
                    }
                }
                if (find_times) {
                    for (RuleManager.RuleInstance rule : ruleSet.times().values()) {
                        futures.add(findTimexInVirtualThread(jcas, rule, containerFuture, threadPool));
                    }
                }
                if (find_sets) {
                    for (RuleManager.RuleInstance rule : ruleSet.sets().values()) {
                        futures.add(findTimexInVirtualThread(jcas, rule, containerFuture, threadPool));
                    }
                }
                if (find_durations) {
                    for (RuleManager.RuleInstance rule : ruleSet.durations().values()) {
                        futures.add(findTimexInVirtualThread(jcas, rule, containerFuture, threadPool));
                    }
                }
                if (find_temponyms) {
                    for (RuleManager.RuleInstance rule : ruleSet.temponyms().values()) {
                        futures.add(findTimexInVirtualThread(jcas, rule, containerFuture, threadPool));
                    }
                }
            }
            for (CompletableFuture<Void> future : futures) {
                future.get();
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        /*
         * kick out some overlapping expressions
         */
        if (deleteOverlapped) deleteOverlappedTimexesPreprocessing(jcas);

        /*
         * specify ambiguous values, e.g.: specific year for dates values of
         * format UNDEF-year-01-01; specific month for values of format UNDEF-last-month
         */
        specifyAmbiguousValues(jcas);

        // disambiguate historic dates
        // check dates without explicit hints to AD or BC if they might refer to BC dates
        if (flagHistoricDates) try {
            disambiguateHistoricDates(jcas);
        } catch (Exception e) {
            getLogger().error("Something went wrong disambiguating historic dates:\n" + e.fillInStackTrace().getMessage());
        }

        if (find_temponyms) {
            TemponymPostprocessing.handleIntervals(jcas);
        }

        /*
         * kick out the rest of the overlapping expressions
         */
        if (deleteOverlapped) deleteOverlappedTimexesPostprocessing(jcas);

        // run arbitrary processors
        procMan.executeProcessors(jcas, Priority.ARBITRARY);

        // remove invalid timexes
        removeInvalids(jcas);

        // run postprocessing processors
        procMan.executeProcessors(jcas, Priority.POSTPROCESSING);
    }

    private CompletableFuture<Void> findTimexInVirtualThread(JCas jcas, RuleManager.RuleInstance rule, CompletableFuture<ContextAnalyzer.SentenceContainer> containerFuture, ExecutorService threadPool) {
        return containerFuture
                .thenApplyAsync(container -> findTimexes(rule, container), threadPool)
                .thenAcceptBoth(
                        containerFuture,
                        (maybeRuleMatch, container) -> {
                            try {
                                maybeRuleMatch.ifPresent(ruleMatch -> addTimexAnnotationsToJCas(jcas, container, ruleMatch));
                            } catch (NullPointerException npe) {
                                getLogger().error(
                                        """
                                                HeidleTimeX's execution has been interrupted by an exception that \
                                                is likely rooted in faulty normalization resource files. Please consider opening an issue \
                                                report containing the following information at our GitHub project issue tracker: \
                                                https://github.com/texttechnologylab/heideltime/issues - Thanks!
                                                Sentence [{}-{}]: {}
                                                Language: {}
                                                Stack Trace: {}""",
                                        container.begin(),
                                        container.end(),
                                        container.text(),
                                        language,
                                        npe.fillInStackTrace().getMessage()
                                );
                            }
                        }
                );
    }

    /**
     * Add timex annotation to CAS object.
     */
    public void addTimexAnnotation(
            String timexType,
            int begin,
            int end,
            ContextAnalyzer.SentenceContainer sentence,
            HeidelTimeX.TimexAttributes attributes,
            String timexId,
            String foundByRule,
            JCas jcas
    ) {
        Timex3 timex3 = new Timex3(jcas);
        timex3.setBegin(begin);
        timex3.setEnd(end);

//        timex3.setFilename(sentence.getFilename());
        timex3.setSentId(sentence.id());

        timex3.setEmptyValue(attributes.emptyValue());

        StringBuilder allTokIds = new StringBuilder();
        for (Token tok : sentence.tokens()) {
            if (tok.getBegin() <= begin && tok.getEnd() > begin) {
                timex3.setFirstTokId(tok._id());
                allTokIds = new StringBuilder("BEGIN<-->" + tok._id());
            } else if ((tok.getBegin() > begin) && (tok.getEnd() <= end)) {
                allTokIds.append("<-->").append(tok._id());
            } else if (tok.getBegin() > end) {
                break;
            }
        }
        timex3.setAllTokIds(allTokIds.toString());

        timex3.setTimexType(timexType);
        timex3.setTimexValue(attributes.value());
        timex3.setTimexId(timexId);
        timex3.setFoundByRule(foundByRule);
        if ((timexType.equals("DATE")) || (timexType.equals("TIME"))) {
            if ((attributes.value().startsWith("X")) || (attributes.value().startsWith("UNDEF"))) {
                timex3.setFoundByRule(foundByRule + "-relative");
            } else {
                timex3.setFoundByRule(foundByRule + "-explicit");
            }

            /*
             *  check for historic dates/times starting with BC
             *  to check if post-processing step is required
             */
            if (!flagHistoricDates && (typeToProcess.equals("narrative") || typeToProcess.equals("narratives"))) {
                if (timex3.getTimexValue().startsWith("BC")) {
                    flagHistoricDates = true;
                }
            }
        }
        if (attributes.quant() != null) {
            timex3.setTimexQuant(attributes.quant());
        }
        if (attributes.freq() != null) {
            timex3.setTimexFreq(attributes.freq());
        }
        if (attributes.mod() != null) {
            timex3.setTimexMod(attributes.mod());
        }

        timex3.addToIndexes();

        if (doDebug) {
            getLogger().debug(
                    "EXTRACTION PHASE:   " +
                            timex3.getTimexId() +
                            " found by:" +
                            timex3.getFoundByRule() +
                            " text:" +
                            timex3.getCoveredText()
            );
            getLogger().debug(
                    "NORMALIZATION PHASE:" +
                            timex3.getTimexId() +
                            " found by:" +
                            timex3.getFoundByRule() +
                            " text:" +
                            timex3.getCoveredText() +
                            " value:" +
                            timex3.getTimexValue()
            );
        }
    }

    public record RuleMatches<T>(RuleManager.RuleInstance rule, List<T> results) {
        public boolean isEmpty() {
            return results.isEmpty();
        }

        public boolean any() {
            return !results.isEmpty();
        }
    }

    /**
     * Apply the extraction rules, normalization rules
     */
    protected Optional<HeidelTimeX.RuleMatches<HeidelTimeX.TimexAttributes>> findTimexes(
            RuleManager.RuleInstance rule,
            ContextAnalyzer.SentenceContainer sentence
    ) {
        if (!rule.fastCheck(sentence.text())) {
            return Optional.empty();
        } else {
            return
                    Optional.of(new HeidelTimeX.RuleMatches<>(
                            rule,
                            Utils.findMatches(rule.pattern(), sentence.text()).stream()
                                    .filter(matchResult -> ContextAnalyzer.checkSentenceMatch(
                                            sentence, matchResult.start(), matchResult.end()
                                    ))
                                    .filter(matchResult -> rule.checkPosConstraint(sentence, matchResult))
                                    .map(matchResult -> getTimexAttributes(rule, matchResult))
                                    .filter(Objects::nonNull)
                                    .toList()
                    ));
        }
    }

    private synchronized void addTimexAnnotationsToJCas(JCas jCas, ContextAnalyzer.SentenceContainer sentence, HeidelTimeX.RuleMatches<HeidelTimeX.TimexAttributes> ruleMatch) {
        RuleManager.RuleInstance rule = ruleMatch.rule();
        for (HeidelTimeX.TimexAttributes attributes : ruleMatch.results()) {
            addTimexAnnotation(
                    rule.type(),
                    attributes.start() + sentence.begin(),
                    attributes.end() + sentence.begin(),
                    sentence,
                    attributes,
                    "t" + timexID++,
                    rule.name(),
                    jCas
            );
        }
    }

    public record TimexAttributes(
            int start,
            int end,
            String value,
            String quant,
            String freq,
            String mod,
            String emptyValue
    ) {
    }

    public HeidelTimeX.TimexAttributes getTimexAttributes(RuleManager.RuleInstance rule, MatchResult matchResult) {
        try {
            // Normalize Value
            String value = applyRuleFunctions(rule.normalization(), matchResult);
            if (value == null) return null;

            // For example "PT24H" -> "P1D"
            if (group_gran) value = correctDurationValue(value);

            // get quant
            String quant = "";
            if (rule.quant() != null) {
                quant = applyRuleFunctions(rule.quant(), matchResult);
            }

            // get freq
            String freq = "";
            if (rule.freq() != null) {
                freq = applyRuleFunctions(rule.freq(), matchResult);
            }

            // get mod
            String mod = "";
            if (rule.mod() != null) {
                mod = applyRuleFunctions(rule.mod(), matchResult);
            }

            // get emptyValue
            String emptyValue = "";
            if (rule.empty() != null) {
                emptyValue = applyRuleFunctions(rule.empty(), matchResult);
                emptyValue = correctDurationValue(emptyValue);
            }

            RuleManager.Offset offset = rule.offset();

            return new HeidelTimeX.TimexAttributes(
                    matchResult.start(offset.start()),
                    matchResult.end(offset.end()),
                    value,
                    quant,
                    freq,
                    mod,
                    emptyValue
            );
        } catch (NormalizationException e) {
            getLogger().error("Caught exception while applying rule functions for rule %s".formatted(rule.name()), e);
            return null;
        }
    }
}
