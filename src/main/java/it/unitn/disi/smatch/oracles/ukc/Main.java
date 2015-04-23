package it.unitn.disi.smatch.oracles.ukc;

import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.oracles.ILinguisticOracle;
import it.unitn.disi.smatch.oracles.LinguisticOracleException;
import it.unitn.disi.smatch.preprocessors.ContextPreprocessorException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;

/**
 * Created by Ahmed on 6/12/14.
 */
@Component
public class Main implements IMain{
    private static UKC ukcEntity = new UKC();
    protected static ILinguisticOracle linguisticOracle;
    /**
     * the words which are cut off from the area of discourse
     */
    public static final String meaninglessWords = " these other others through of on to their than from for by in at is are have has the a as with your etc our into its his her which him among that those against di dell della de del los sobre hacia su que desde para por en es son tener tiene el la un como con tu nuestro dentro cual le entre aquellos contra otros otras otro otra ";
    /**
     * the words which are treated as logical and (&)
     */
    public static final String andWords = " + & ^ ";
    /**
     * the words which are treated as logical or (|)
     */
    public static final String orWords = " and or | , e o y ";
    /**
     * the words which are treated as logical not (~)
     */
    public static final String notWords = " except non without excepto no sin ";
    /**
     * number characters
     */
    public static final String numberCharacters = "1234567890";

    public static void main(String[] args){
        HashMap <String, Integer> words = new HashMap<String, Integer>();
        List<ISense> wnSense = new ArrayList<>();
        ukcEntity.loadContextLoader();
        linguisticOracle = new UKC();
        linguisticOracle.readMultiwords("es");
        try {
            File fileDir = new File("Spanish.txt");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(fileDir), "UTF8"));

            String str;

            while ((str = in.readLine()) != null) {
                str = str.trim();
                str = replacePunctuation(str);
                str = str.toLowerCase();
                StringTokenizer lemmaTokenizer = new StringTokenizer(str, " _()[]/'\\#1234567890");
                ArrayList<String> tokens = new ArrayList<>();
                while (lemmaTokenizer.hasMoreElements()) {
                    tokens.add(lemmaTokenizer.nextToken());
                }

                tokens = multiwordRecognition(tokens, "es");

                // for all tokens in label
                for (int i = 0; i < tokens.size(); i++) {
                    String token = tokens.get(i).trim();
                    // if the token is not meaningless
                    if ((!meaninglessWords.contains(" " + token + " ")) && (isTokenMeaningful(token))) {
                        // add to list of processed tokens

                        // if not logical connective
                        if (!andWords.contains(token) && !orWords.contains(token)
                                && !notWords.contains(token) && !hasNumber(token)) {
                            // get WN senses for token
                            if (!(("top".equals(token) || "thing".equals(token)))) {
                                wnSense = linguisticOracle.getSenses(token, "es");
                            } else {
                                wnSense = Collections.emptyList();
                            }
                            if (0 == wnSense.size()) {
                                List<String> newTokens = complexWordsRecognition(token, "es");
                                if (0 < newTokens.size()) {
                                    wnSense = linguisticOracle.getSenses(newTokens.get(0), "es");
                                    tokens.remove(i);
                                    tokens.add(i, newTokens.get(0));
                                    for (int j = 1; j < newTokens.size(); j++) {
                                        String s = newTokens.get(j);
                                        tokens.add(i + j, s);
                                    }
                                }
                            }

                            // if there no WN senses
                            if (0 == wnSense.size()) {
                                if(words.containsKey(token))
                                {
                                    words.put(token,words.get(token) + 1);
                                }
                                else
                                {
                                    words.put(token, 1);
                                }
                            } else {

                            }
                        }
                    }
                }
        }
            //write file
            BufferedWriter out = null;
            try {
                //write the language trigram trained data file
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Spanish11www" + ".txt"), "UTF-8"));
                for(String key : words.keySet())
                {
                    out.write(key + "\t" + words.get(key));
                    out.write("\n");
                }
                out.close();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ContextPreprocessorException e) {
            e.printStackTrace();
        } catch (LinguisticOracleException e) {
            e.printStackTrace();
        }
    }

    private static String replacePunctuation(String lemma) {
        lemma = lemma.replace(",", " , ");
        lemma = lemma.replace('.', ' ');
//        lemma = lemma.replace('-', ' ');
        lemma = lemma.replace('\'', ' ');
        lemma = lemma.replace('(', ' ');
        lemma = lemma.replace(')', ' ');
        lemma = lemma.replace(':', ' ');
        lemma = lemma.replace(";", " ; ");
        return lemma;
    }


    private static  boolean hasNumber(String input) {
        for (int i = 0; i < numberCharacters.length(); i++) {
            if (-1 < input.indexOf(numberCharacters.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTokenMeaningful(String token) {
        token = token.trim();
        return andWords.contains(token) || orWords.contains(token) || token.length() >= 3;
    }

    private static List<String> complexWordsRecognition(String token, String language) throws ContextPreprocessorException {
        List<String> result = new ArrayList<>();
        try {
            List<ISense> senses = new ArrayList<>();
            int i = 0;
            String start = null;
            String end = null;
            String toCheck = null;
            boolean flag = false;
            boolean multiword = false;
            while ((i < token.length() - 1) && (0 == senses.size())) {
                i++;
                start = token.substring(0, i);
                end = token.substring(i, token.length());
                toCheck = start + ' ' + end;
                senses = linguisticOracle.getSenses(toCheck, language);
                if (0 == senses.size()) {
                    toCheck = start + '-' + end;
                    senses = linguisticOracle.getSenses(toCheck, language);
                }

                if (0 < senses.size()) {
                    multiword = true;
                    break;
                } else {
                    if ((start.length() > 3) && (end.length() > 3)) {
                        senses = linguisticOracle.getSenses(start, language);
                        if (0 < senses.size()) {
                            senses = linguisticOracle.getSenses(end, language);
                            if (0 < senses.size()) {
                                flag = true;
                                break;
                            }
                        }
                    }
                }
            }
            if (multiword) {
                result.add(toCheck);
                return result;
            }
            if (flag) {
                result.add(start);
                result.add(end);
                return result;
            }
            return result;
        } catch (LinguisticOracleException e) {
            throw new ContextPreprocessorException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private static ArrayList<String> multiwordRecognition(ArrayList<String> tokens, String language) throws ContextPreprocessorException {
        String subLemma;
        Map<String, List<Integer>> is_token_in_multiword = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            subLemma = tokens.get(i);
            if ((!andWords.contains(subLemma)) || (!orWords.contains(subLemma))) {
                // if there a multiword starting with a sublemma
                ArrayList<ArrayList<String>> entries;
                try {
                    entries = linguisticOracle.getMultiwords(subLemma, language);
                } catch (LinguisticOracleException e) {
                    throw new ContextPreprocessorException(e.getMessage(), e);
                }
                if (null != entries) {
                    for (List<String> mweTail : entries) {
                        boolean flag = false;
                        int co = 0;
                        // at the end co is needed to move pointer for the cases like
                        // Clupea harengus with mw Clupea harengus harengus
                        while ((co < mweTail.size()) && (extendedIndexOf(tokens, mweTail.get(co), co, language) > i + co)) {
                            flag = true;
                            co++;
                        }
                        if ((co > mweTail.size() - 1) && (flag)) {
                            ArrayList<Integer> positions = new ArrayList<>();
                            int word_pos = tokens.indexOf(subLemma);
                            if (word_pos == -1) {
                                break;
                            }
                            int multiword_pos = word_pos;
                            positions.add(word_pos);
                            boolean cont = true;
                            boolean connectives_precedence = false;
                            int and_pos = -1;
                            for (String tok : mweTail) {
                                int old_pos = word_pos;
                                word_pos = tokens.subList(old_pos + 1, tokens.size()).indexOf(tok) + old_pos + 1;
                                if (word_pos == -1) {
                                    word_pos = extendedIndexOf(tokens, tok, old_pos, language);
                                    if (word_pos == -1) {
                                        break;
                                    }
                                }
                                if (word_pos - old_pos > 1) {
                                    cont = false;
                                    for (int r = old_pos + 1; r < word_pos; r++) {
                                        if (andWords.contains(tokens.get(r)) || orWords.contains(tokens.get(r))) {
                                            and_pos = r;
                                            connectives_precedence = true;
                                        } else {
                                            //connectives_precedence = false;
                                        }
                                    }
                                }
                                positions.add(word_pos);
                            }
                            int removed_tokens_index_correction = 0;
                            if (cont) {
                                String multiword = "";
                                for (Integer integer : positions) {
                                    int pos = integer - removed_tokens_index_correction;
                                    multiword = multiword + tokens.get(pos) + " ";
                                    tokens.remove(pos);
                                    removed_tokens_index_correction++;
                                }
                                multiword = multiword.substring(0, multiword.length() - 1);
                                tokens.add(multiword_pos, multiword);
                            } else {
                                if (connectives_precedence) {
                                    if (and_pos > multiword_pos) {
                                        String multiword = "";
                                        int word_distance = positions.get(positions.size() - 1) - positions.get(0);
                                        for (Integer integer : positions) {
                                            int pos = integer - removed_tokens_index_correction;
                                            if (is_token_in_multiword.get(tokens.get(pos)) == null) {
                                                ArrayList<Integer> toAdd = new ArrayList<>();
                                                toAdd.add(1);
                                                toAdd.add(word_distance - 1);
                                                is_token_in_multiword.put(tokens.get(pos), toAdd);
                                            } else {
                                                List<Integer> toAdd = is_token_in_multiword.get(tokens.get(pos));
                                                int tmp = toAdd.get(0) + 1;
                                                toAdd.remove(0);
                                                toAdd.add(0, tmp);
                                                is_token_in_multiword.put(tokens.get(pos), toAdd);
                                            }
                                            multiword = multiword + tokens.get(pos) + " ";
                                        }
                                        multiword = multiword.substring(0, multiword.length() - 1);
                                        tokens.remove(multiword_pos);
                                        tokens.add(multiword_pos, multiword);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ArrayList<String> tmp = new ArrayList<>();
        for (String s : tokens) {
            if (is_token_in_multiword.get(s) == null) {
                tmp.add(s);
            } else {
                List<Integer> toAdd = is_token_in_multiword.get(s);
                int dist_wo_ands_ors = toAdd.get(0);
                int multiword_participation = toAdd.get(1);
                if (dist_wo_ands_ors != multiword_participation) {
                    tmp.add(s);
                }
            }
        }
        return tmp;
    }

    private static int extendedIndexOf(List<String> vec, String str, int init_pos, String language) throws ContextPreprocessorException {
        try {
            // for all words in the input list starting from init_pos
            for (int i = init_pos; i < vec.size(); i++) {
                String vel = vec.get(i);
                // try syntactic
                if (vel.equals(str)) {
                    return i;
                } else if (vel.indexOf(str) == 0) {
                    // and semantic comparison
                    if (linguisticOracle.isEqual(vel, str, language)) {
                        vec.add(i, str);
                        vec.remove(i + 1);
                        return i;
                    }
                }
            }
            return -1;
        } catch (LinguisticOracleException e) {
            throw new ContextPreprocessorException(e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    @Transactional
    public void doSomething(){





    }
}
