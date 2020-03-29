package be.mapariensis.kanjiryoku.providers;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.mapariensis.kanjiryoku.dict.DictionaryAccessException;
import be.mapariensis.kanjiryoku.dict.KanjidicInterface;
import be.mapariensis.kanjiryoku.model.Problem;
import be.mapariensis.kanjiryoku.model.Word;
import be.mapariensis.kanjiryoku.model.YojiProblem;
import be.mapariensis.kanjiryoku.net.exceptions.BadConfigurationException;
import be.mapariensis.kanjiryoku.net.server.ConnectionMonitor;
import be.mapariensis.kanjiryoku.util.IProperties;

public class KanjiryokuYojiWrapper implements ProblemParser {
    private static final Logger log = LoggerFactory
            .getLogger(KanjiryokuYojiWrapper.class);
    public static final String PARAM_KANJIDIC = "kanjidicConfig";
    public static final String PARAM_DICTFACTORY = "interfaceClassName";
    public static final String PARAM_SEED = "seed";
    public static final String PARAM_OPTIONTOTAL = "optionCount";
    public static final int PARAM_OPTIONTOTAL_DEFAULT = 4;
    private final KanjiryokuShindanParser kparser = new KanjiryokuShindanParser();
    private final KanjidicInterface dict;
    private final Random rng;
    private final int optiontotal;
    // Fallback kanji list
    private final Set<Character> fallback;

    public KanjiryokuYojiWrapper(KanjidicInterface dict, Random rng,
            int optiontotal) throws BadConfigurationException {
        this.dict = dict;
        this.rng = rng;
        this.optiontotal = optiontotal;
        try {
            this.fallback = dict.randomKanji();
        } catch (DictionaryAccessException e1) {
            log.error("Failed to access dictionary", e1);
            throw new BadConfigurationException(e1);
        }
    }

    @Override
    public Problem parseProblem(String input) throws ParseException {
        Problem p = kparser.parseProblem(input);
        // sanity checks, extract the first (and only) word
        Word w;
        if (p.words.size() != 1 || (w = p.words.get(0)).main.length() != 4)
            throw new ParseException("Not a Yoji problem: " + p.words, 0);
        // TODO : pick options that actually make sense for the "wrong"
        // suggestions

        List<List<String>> options = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            List<String> myoptions = new ArrayList<>(optiontotal);
            int correctPosition = rng.nextInt(optiontotal);
            char correctChar = w.main.charAt(i);
            // query similar characters from the dictionary
            Iterator<Character> kanjilist;
            try {
                Set<Character> dictres = dict.getSimilar(correctChar);
                dictres.remove(correctChar);
                if (dictres.size() < 4) {
                    // list of similar kanji too small
                    fallback.addAll(dictres);
                    kanjilist = fallback.iterator();
                } else {
                    kanjilist = dictres.iterator();
                }
            } catch (DictionaryAccessException e) {
                log.error("Failed to access dictionary", e);
                throw new ParseException("Backend error while parsing problem",
                        -1);
            }
            for (int j = 0; j < optiontotal; j++) {
                if (j == correctPosition) {
                    myoptions.add(new String(new char[] { correctChar }));
                } else {
                    myoptions.add(new String(new char[] { kanjilist.next() }));
                }
            }
            options.add(myoptions);
        }
        return new YojiProblem(w, options);
    }

    public static class Factory implements ProblemParserFactory {
        private static final Logger log = LoggerFactory
                .getLogger(Factory.class);

        @Override
        public KanjiryokuYojiWrapper getParser(IProperties params)
                throws BadConfigurationException {
            if (params == null)
                throw new BadConfigurationException(
                        "KanjiryokuYojiWrapper requires parameters, but none were supplied.");
            int seed = params.getSafely(PARAM_SEED, Integer.class,
                    (int) (System.currentTimeMillis() % 10000));
            Random rng = new Random(seed);
            int optiontotal = params.getSafely(PARAM_OPTIONTOTAL,
                    Integer.class, PARAM_OPTIONTOTAL_DEFAULT);
            IProperties dictionaryOptions = params.getRequired(PARAM_KANJIDIC,
                    IProperties.class);
            String factory = dictionaryOptions.getRequired(PARAM_DICTFACTORY,
                    String.class);

            // build dictionary interface
            KanjidicInterface.Factory kif;
            try {
                kif = (KanjidicInterface.Factory) ConnectionMonitor.class
                        .getClassLoader().loadClass(factory).getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                log.error(
                        "Failed to instantiate dictionary interface factory.",
                        e);
                throw new BadConfigurationException(e);
            }
            KanjidicInterface iface; // FIXME: resource leak
            try {
                iface = kif.setUp(dictionaryOptions);
            } catch (DictionaryAccessException e) {
                throw new BadConfigurationException(e);
            }
            log.info(
                    "Building yojijukugo parser with seed {} and kanjidic interface {}",
                    seed, iface);

            return new KanjiryokuYojiWrapper(iface, rng, optiontotal);
        }

    }
}