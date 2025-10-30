package org.texttechnologylab.heideltime;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestHeidelTimeXIntegration {
    AnalysisEngine engine;
    JCas jCas;

    final Pattern WORD_PATTERN = Pattern.compile("\\p{L}+(-\\p{L}+)?|[^\\p{L}\\s]+", Pattern.UNICODE_CHARACTER_CLASS);

    @BeforeAll
    public void setUp() throws ResourceInitializationException, CASException {
        jCas = JCasFactory.createJCas();
        engine = AnalysisEngineFactory.createEngine(
                HeidelTimeX.class,
//                HeidelTimeX.PARAM_PARALLEL_SEARCH, true,
                HeidelTimeX.PARAM_LANGUAGE, Language.GERMAN,
                HeidelTimeX.PARAM_DEBUG, false,
                HeidelTimeX.PARAM_TYPE_TO_PROCESS, "narrative",
                HeidelTimeX.PARAM_FIND_DATES, true,
                HeidelTimeX.PARAM_FIND_TIMES, true,
                HeidelTimeX.PARAM_FIND_DURATIONS, true,
                HeidelTimeX.PARAM_FIND_SETS, true,
                HeidelTimeX.PARAM_FIND_TEMPONYMS, true,
                HeidelTimeX.PARAM_GROUP_GRAN, true
        );
    }

    /**
     * Get a resource stream by name and open a buffered reader for it.
     * If the input is compressed, the corresponding decompressor is used through {@link CompressorStreamFactory#createCompressorInputStream(InputStream)}.
     * If the input is not compressed, the corresponding input stream is used directly.
     * In either case, the input stream is wrapped in a {@link BufferedReader} for convenience.
     *
     * @param resourceName The name of the resource to open. Must be a valid resource name to be loaded with {@link ClassLoader#getResourceAsStream(String)}.}
     * @return A {@link BufferedReader} for the resource with the given name, possibly decompressed.
     * @throws NullPointerException If the resource with the given name could not be found.
     */
    private static BufferedReader getBufferedReader(String resourceName) throws NullPointerException {
        final BufferedInputStream fileInputStream = new BufferedInputStream(
                Objects.requireNonNull(
                        TestHeidelTimeXIntegration.class.getClassLoader().getResourceAsStream(resourceName),
                        "Resource not found: " + resourceName
                )
        );
        try {
            final CompressorInputStream compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(fileInputStream);
            return new BufferedReader(new InputStreamReader(compressorInputStream));
        } catch (CompressorException e) {
            return new BufferedReader(new InputStreamReader(fileInputStream));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "13068230.txt.gz",
    })
    public void testFile(String resourceName) throws IOException {
        try (BufferedReader reader = getBufferedReader(resourceName)) {
            List<String> lines = reader.lines().filter(line -> !line.startsWith("#")).map(String::trim).toList();
            String text = String.join(" ", lines);

            jCas.reset();
            jCas.setDocumentLanguage("de");
            jCas.setDocumentText(text);

            // Create Sentence annotations, one for each line
            ArrayList<Integer> offsets = new ArrayList<>(lines.size() + 1);
            offsets.add(0);
            for (String line : lines) {
                offsets.add(offsets.getLast() + line.length() + 1);
            }
            for (int i = 1; i < offsets.size(); i++) {
                new Sentence(jCas, offsets.get(i - 1), offsets.get(i) - 1).addToIndexes();
            }
            // Add Token annotations for each "word" as given by a regular expression
            List<MatchResult> tokenMatches = WORD_PATTERN.matcher(text).results().toList();
            for (MatchResult matchResult : tokenMatches) {
                new Token(jCas, matchResult.start(), matchResult.end()).addToIndexes();
            }

            SimplePipeline.runPipeline(jCas, engine);

            Collection<Timex3> timex3s = JCasUtil.select(jCas, Timex3.class);
            System.out.printf("Found %d Timex3 annotations in %d sentences.%n", timex3s.size(), lines.size());
            printAnnotations(timex3s);
        } catch (AnalysisEngineProcessException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // testcase for seasons
            "Lebendig wirkt der Garten zur Spätsommerzeit, wenn Artischoken, rotblühende Lobelien, farbige Lupinen auf der Blumenrabatte vorgesehen werden.",
            // testcase for weird OCR errors
            "Arnica montana anwesend abwesend Arrhenatherum elatius anwesend A 4 C 161 A+C 165 abwesend D 169 B 939 D+B 1108 A+D 173 C+B 1100 n 1273",
    })
    public void testSentence(String sentence) throws IOException, AnalysisEngineProcessException {
        jCas.reset();
        jCas.setDocumentLanguage("de");
        jCas.setDocumentText(sentence);

        new Sentence(jCas, 0, sentence.length()).addToIndexes();
        // Add Token annotations for each "word" as given by a regular expression
        List<MatchResult> tokenMatches = WORD_PATTERN.matcher(sentence).results().toList();
        for (MatchResult matchResult : tokenMatches) {
            new Token(jCas, matchResult.start(), matchResult.end()).addToIndexes();
        }

        SimplePipeline.runPipeline(jCas, engine);

        Collection<Timex3> timex3s = JCasUtil.select(jCas, Timex3.class);
        printAnnotations(timex3s);
        if (timex3s.isEmpty()) {
            Assertions.fail("Expected at least one Timex3 for '%s' but got %d".formatted(sentence, timex3s.size()));
        }
    }

    public static void printAnnotations(Collection<? extends Annotation> annotations) {
        for (Annotation annotation : annotations) {
            StringBuffer stringBuffer = new StringBuffer();
            annotation.prettyPrint(0, 2, stringBuffer, true);
            System.out.print(stringBuffer);
            System.out.println("\n  text: \"" + annotation.getCoveredText() + "\"\n");
        }
    }

}
