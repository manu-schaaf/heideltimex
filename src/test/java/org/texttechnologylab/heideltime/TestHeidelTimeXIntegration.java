package org.texttechnologylab.heideltime;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestHeidelTimeXIntegration {
    AnalysisEngine engine;
    JCas jCas;

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

    @ParameterizedTest
    @ValueSource(strings = {
            "13068230.txt.gz",
    })
    public void testBIOfid(String fileName) throws URISyntaxException, IOException {
        URI fileUri = TestHeidelTimeXIntegration.class.getClassLoader().getResource(fileName).toURI();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GzipCompressorInputStream(new FileInputStream(new File(fileUri))))
        )) {
            List<String> lines = reader.lines().filter(line -> !line.startsWith("#")).map(String::trim).toList();

            ArrayList<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            for (String line : lines) {
                offsets.add(offsets.getLast() + line.length() + 1);
            }
            String text = String.join(" ", lines);

            jCas.reset();
            jCas.setDocumentLanguage("de");
            jCas.setDocumentText(text);

            int offset = 0;
            for (int i = 1; i < text.length(); i++) {
                if (text.charAt(i) == ' ' || i == text.length() - 1) {
                    new Token(jCas, offset, i).addToIndexes();
                    offset = i + 1;
                }
            }
            for (int i = 1; i < offsets.size(); i++) {
                new Sentence(jCas, offsets.get(i - 1), offsets.get(i) - 1).addToIndexes();
            }

            SimplePipeline.runPipeline(jCas, engine);

            Collection<Timex3> timex3s = JCasUtil.select(jCas, Timex3.class);
            System.out.printf("Found %d Timex3 annotations in %d sentences.%n", timex3s.size(), lines.size());
            printAnnotations(timex3s);
        } catch (AnalysisEngineProcessException e) {
            throw new RuntimeException(e);
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
