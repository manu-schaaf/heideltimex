package de.unihd.dbs.uima.annotator.heideltime;

import de.unihd.dbs.uima.annotator.heideltime.resources.Language;
import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Timex3;
import de.unihd.dbs.uima.types.heideltime.Token;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.texttechnologylab.heideltime.TestHeidelTimeX;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.texttechnologylab.heideltime.TestHeidelTimeX.printAnnotations;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestHeidelTime {

    AnalysisEngine engine;
    JCas jCas;

    @BeforeAll
    public void setUp() throws ResourceInitializationException, CASException {
        jCas = JCasFactory.createJCas();
        engine = AnalysisEngineFactory.createEngine(
                HeidelTime.class,
                HeidelTime.PARAM_LANGUAGE, Language.GERMAN,
                HeidelTime.PARAM_LOCALE, "de_DE",
                HeidelTime.PARAM_DEBUG, false,
                HeidelTime.PARAM_TYPE_TO_PROCESS, "narrative",
                HeidelTime.PARAM_DATE, true,
                HeidelTime.PARAM_TIME, true,
                HeidelTime.PARAM_DURATION, true,
                HeidelTime.PARAM_SET, true,
                HeidelTime.PARAM_TEMPONYMS, true,
                HeidelTime.PARAM_GROUP, true
        );
    }

    @Test
    public void testLeipzigWikipedia() throws URISyntaxException, IOException {
        URI fileUri = TestHeidelTimeX.class.getClassLoader().getResource("leizipg_wortschatz/deu_wikipedia_2021_10K-sentences.txt").toURI();
        try (BufferedReader fr = new BufferedReader(new FileReader(new File(fileUri)))) {
            List<String> lines = fr.lines().map(line -> line.substring(line.indexOf('\t') + 1).trim()).toList();
            lines = lines.subList(0, 1000);

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
                    Token token = new Token(jCas, offset, i);
                    token.setPos("NN");
                    token.addToIndexes();
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
}
