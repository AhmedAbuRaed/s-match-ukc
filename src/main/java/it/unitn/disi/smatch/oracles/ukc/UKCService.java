package it.unitn.disi.smatch.oracles.ukc;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import it.unitn.disi.smatch.data.ling.ISense;
import it.unitn.disi.smatch.data.mappings.IMappingElement;
import it.unitn.disi.smatch.data.trees.IContext;
import it.unitn.disi.smatch.data.trees.INode;
import it.unitn.disi.sweb.core.kb.IConceptService;
import it.unitn.disi.sweb.core.kb.IKnowledgeBaseService;
import it.unitn.disi.sweb.core.kb.IVocabularyService;
import it.unitn.disi.sweb.core.kb.model.KnowledgeBase;
import it.unitn.disi.sweb.core.kb.model.concepts.Concept;
import it.unitn.disi.sweb.core.kb.model.concepts.ConceptRelation;
import it.unitn.disi.sweb.core.kb.model.concepts.ConceptRelationType;
import it.unitn.disi.sweb.core.kb.model.vocabularies.*;
import it.unitn.disi.sweb.core.nlp.components.lemmatizers.ILemmatizer;
import it.unitn.disi.sweb.core.nlp.parameters.NLPParameters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by Ahmed on 6/25/14.
 */
@Transactional(readOnly=true)
@Component
public class UKCService implements IUKCService {

    @Autowired
    private IKnowledgeBaseService knowledgeBaseService;

    @Autowired
    private IConceptService conceptservice;

    @Autowired
    private IVocabularyService vocabularyservice;

    // Autowires the tokenizer only
    @Autowired
    @Qualifier("Lemmatizer")
    private ILemmatizer<NLPParameters> lemmatizer;
    // Also autowires the NLP parameters

    @Autowired
    @Qualifier("NLPParameters")
    private NLPParameters parameters;
    
    public UKCService()
    {
        //ContextLoader cl = new ContextLoader("classpath:/META-INF/smatch-context.xml");
        //cl.getApplicationContext().getBean(IUKCService.class);
    }

    @Override
    public boolean isEqual(String str1, String str2, String language) {
        try
        {
            str1 = str1.toLowerCase(new Locale(language));
            str2 = str2.toLowerCase(new Locale(language));
        }
        catch (Exception e)
        {
            str1 = str1.toLowerCase();
            str2 = str2.toLowerCase();
        }
        ArrayList<String> str1res = new ArrayList<>() ;
        ArrayList<String> str2res = new ArrayList<>() ;

        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);

        if(vocabularyservice.readMultiWordLemmas(voc).contains(str1))
        {
            str1res.add(str1);
        }
        //if(lemmatizer.isLemmaExists(derivation, language))
        if(lemmatizer.isLemmaExists(str1, voc))
        {
            str1res.add(str1);
        }
        else
        {
/*          Map<String,Set<String>> alllemmas = lemmatizer.lemmatize(derivation, language);
            Collection s = alllemmas.values();
            for(String key : alllemmas.keySet())
            {
                for(String lemma : alllemmas.get(key))
                {
                    if (null != lemma && !result.contains(lemma)) {
                        result.add(lemma);
                    }
                }
            }
            alllemmas.clear();*/
            str1res.addAll(lemmatizer.lemmatize(str1, voc));
        }

        if(vocabularyservice.readMultiWordLemmas(voc).contains(str2))
        {
            str2res.add(str2);
        }
        //if(lemmatizer.isLemmaExists(derivation, language))
        if(lemmatizer.isLemmaExists(str2, voc))
        {
            str2res.add(str2);
        }
        else
        {
/*          Map<String,Set<String>> alllemmas = lemmatizer.lemmatize(derivation, language);
            Collection s = alllemmas.values();
            for(String key : alllemmas.keySet())
            {
                for(String lemma : alllemmas.get(key))
                {
                    if (null != lemma && !result.contains(lemma)) {
                        result.add(lemma);
                    }
                }
            }
            alllemmas.clear();*/
            str2res.addAll(lemmatizer.lemmatize(str2, voc));
        }

        for(String str1lemma : str1res)
        {
        for(String str2lemma : str2res)
            if(str1lemma.equals(str2lemma))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public ISense createSense(String id,String language) {
        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);
        Synset synset = vocabularyservice.readSynset(Long.valueOf(id).longValue());
        return new UKCSense(synset.getConcept().getId(), synset.getId(), language, this);
    }

    @Override
    public List<String> getMultiwords(String language) {
        List<String> result = new ArrayList<String>();
        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);
        List<Word> multiwords =  vocabularyservice.readMultiWords(voc);
        for(int i=0;i<multiwords.size();i++)
        {
            result.add(multiwords.get(i).toString()) ;
        }
        return result;
    }

    @Override
    public char getRelation(List<ISense> sourceSenses, List<ISense> targetSenses) {
        for (ISense sourceSense : sourceSenses) {
            for (ISense targetSense : targetSenses) {
                UKCSense sourceSyn = (UKCSense) sourceSense;
                UKCSense targetSyn = (UKCSense) targetSense;
                if (isSourceSynonymTarget(sourceSyn,targetSyn)) {
                    return IMappingElement.EQUIVALENCE;
                }
            }
        }
        for (ISense sourceSense : sourceSenses) {
            for (ISense targetSense : targetSenses) {
                UKCSense sourceSyn = (UKCSense) sourceSense;
                UKCSense targetSyn = (UKCSense) targetSense;
                if (isSourceLessGeneralThanTarget(sourceSyn,targetSyn)) {
                    return IMappingElement.LESS_GENERAL;
                }
            }
        }
        for (ISense sourceSense : sourceSenses) {
            for (ISense targetSense : targetSenses) {
                UKCSense sourceSyn = (UKCSense) sourceSense;
                UKCSense targetSyn = (UKCSense) targetSense;
                if (isSourceMoreGeneralThanTarget(sourceSyn,targetSyn)) {
                    return IMappingElement.MORE_GENERAL;
                }
            }
        }
        for (ISense sourceSense : sourceSenses) {
            for (ISense targetSense : targetSenses) {
                UKCSense sourceSyn = (UKCSense) sourceSense;
                UKCSense targetSyn = (UKCSense) targetSense;
                if (isSourceOppositeToTarget(sourceSyn,targetSyn)) {
                    return IMappingElement.DISJOINT;
                }
            }
        }
        return IMappingElement.IDK;
    }

    @Override
    public boolean isSourceLessGeneralThanTarget(ISense source, ISense target) {
        return isSourceMoreGeneralThanTarget(target, source);
    }

    @Override
    public boolean isSourceSynonymTarget(ISense source, ISense target) {
        if (!(source instanceof UKCSense) || !(target instanceof UKCSense)) {
            return false;
        }
        UKCSense sourceSyn = (UKCSense) source;
        UKCSense targetSyn = (UKCSense) target;
        Synset s = vocabularyservice.readSynset(sourceSyn.getSynsetID());
        Synset t = vocabularyservice.readSynset(targetSyn.getSynsetID());
        if(sourceSyn.getlanguage().equals(targetSyn.getlanguage()))
        {
            if (source.equals(target)) {
                return true;
            }
            if(vocabularyservice.isRelationExists(vocabularyservice.readSynset(sourceSyn.getSynsetID()),vocabularyservice.readSynset(targetSyn.getSynsetID()),SynsetRelationType.SIMILAR_TO))
            {
                return !((PartOfSpeech.ADJECTIVE == sourceSyn.getPOS()) || (PartOfSpeech.ADJECTIVE == targetSyn.getPOS()));
            }
        }
        else {
            if(sourceSyn.getConceptID() == targetSyn.getConceptID())
            {
               return true;
            }
            if (source.equals(target)) {
                return true;
            }
            else
            {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean isSourceOppositeToTarget(ISense source, ISense target) {
        if ((source instanceof UKCSense) && (target instanceof UKCSense)) {
            UKCSense sourceSyn = (UKCSense) source;
            UKCSense targetSyn = (UKCSense) target;
            if(sourceSyn.getlanguage().equals(targetSyn.getlanguage()))
            {
                if (PartOfSpeech.NOUN != sourceSyn.getPOS() || PartOfSpeech.NOUN != targetSyn.getPOS()) {
                    List<Sense> sourcesenses = vocabularyservice.readSynset(sourceSyn.getSynsetID()).getSenses();
                    List<Sense> targetsenses = vocabularyservice.readSynset(targetSyn.getSynsetID()).getSenses();
                    for(int i = 0 ; i < sourcesenses.size(); i++)
                    {
                        for(int j = 0 ; j < targetsenses.size(); j++)
                        {
                            if(vocabularyservice.isRelationExists(sourcesenses.get(i),targetsenses.get(j),SenseRelationType.ANTONYM))
                            {
                                return true;
                            }
                        }
                    }
                    //Relationship list = RelationshipFinder.findRelationships(sourceSyn.getSynset(), targetSyn.getSynset(), PointerType.ANTONYM);
                    //if (list.size() > 0) {
                    //    return true;
                    //}
                }
            }
        }
        return false;
    }

    @Override
    public List<String> getlemmas(long synsetID) {
        List<String> result = vocabularyservice.readLemmas(vocabularyservice.readSynset(synsetID),true);
        return result;
    }

    @Override
    public List<ISense> getParents(long conceptID, String language, int depth) {
        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);

        List<Concept> concepts = conceptservice.readAncestors(conceptservice.readConcept(conceptID));
        List<ISense> result = new ArrayList<ISense>();
        for(int i = 0; i< depth ; i++)
        {
            result.add(new UKCSense(concepts.get(i).getId(),vocabularyservice.readSynset(voc, concepts.get(i)).getId(),language,this));
        }
        return result;
    }

    @Override
    public List<ISense> getChildren(long conceptID, String language, int depth) {
        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);

        List<Concept> concepts = conceptservice.readDescendants(conceptservice.readConcept(conceptID));
        List<ISense> result = new ArrayList<ISense>();
        for(int i = 0; i< depth ; i++)
        {
            result.add(new UKCSense(concepts.get(i).getId(),vocabularyservice.readSynset(voc, concepts.get(i)).getId(),language,this));
        }
        return result;
    }

    @Override
    public PartOfSpeech getPOS(long synsetID) {
        return vocabularyservice.readSynset(synsetID).getPartOfSpeech();
    }

    @Override
    public String detectLanguage(IContext context) {
        String lang = new String();
        try {

                Detector detector = DetectorFactory.create();
                HashMap priorMap = new HashMap();
                priorMap.put("en", new Double(0.1));
                priorMap.put("it", new Double(0.1));
                priorMap.put("es", new Double(0.1));
                detector.setPriorMap(priorMap);

                Iterator<INode> nodes = context.nodeIterator();

                String content = nodes.next().nodeData().getName();
                while(nodes.hasNext())
                {
                    content += " " + nodes.next().nodeData().getName();
                }

                detector.append(content);
                lang = detector.detect();

            }
            catch (LangDetectException e) {
                e.printStackTrace();
            }

        return lang;
    }

    @Override
    public HashMap<String, ArrayList<ArrayList<String>>> readMultiwords(String language) {
        HashMap<String, ArrayList<ArrayList<String>>> languageMultiwords = new HashMap<String, ArrayList<ArrayList<String>>>();
        ArrayList<String> multiwordEnd = new ArrayList<String>();
        ArrayList<String> multiwordEndBelow = new ArrayList<String>();
        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);
        List<Word> multiwordsList = vocabularyservice.readMultiWords(voc);
        HashMap<String, ArrayList<ArrayList<String>>> tempMultiwords = new HashMap<String, ArrayList<ArrayList<String>>>();

        for(int i = 0; i < multiwordsList.size(); i++)
        {
            ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();
            String [] tokens = multiwordsList.get(i).getLemma().split(" ");

            temp = tempMultiwords.get(tokens[0]);
            if(temp == null)
            {
                temp = new ArrayList<ArrayList<String>>();
            }
            multiwordEnd = new ArrayList<String>(Arrays.asList(tokens));
            multiwordEnd.remove(0);
            temp.add(multiwordEnd);
            tempMultiwords.put(tokens[0],temp);
        }
        return  tempMultiwords;
    }


    @Override
    public boolean isSourceMoreGeneralThanTarget(ISense source, ISense target) {
        if (!(source instanceof UKCSense) || !(target instanceof UKCSense)) {
            return false;
        }
        UKCSense sourceSyn = (UKCSense) source;
        UKCSense targetSyn = (UKCSense) target;

        //use distance or path to check for indirect connections
        //get path and check part of

        if ((PartOfSpeech.NOUN == sourceSyn.getPOS() && PartOfSpeech.NOUN == targetSyn.getPOS()) || (PartOfSpeech.VERB == sourceSyn.getPOS() && PartOfSpeech.VERB == targetSyn.getPOS())) {
            if (source.equals(target)) {
                return false;
            }
            // find all more general relationships from UKC
//                    if(!(conceptservice.isPathExists(conceptservice.readConcept(sourceSyn.getConceptID()),
//                            conceptservice.readConcept(targetSyn.getConceptID()))))
            Concept sourceConcept = conceptservice.readConcept(sourceSyn.getConceptID());
            Concept targetConcept = conceptservice.readConcept(targetSyn.getConceptID());
//                    if(!(conceptservice.isPathExists(sourceConcept, targetConcept)))
            List<Concept> ancestorsOfTarget = conceptservice.readAncestors(targetConcept);
            if (ancestorsOfTarget.contains(sourceConcept)) {
                return true;
            } else {
                // commented for faster processing
                //return traverseTree(ancestorsOfTarget, sourceSyn);
            }
        }
        return false;
    }

    private boolean traverseTree(List<Concept> targetAncestors, UKCSense sourceSyn/*, relationType relation*/) {
        // List<Concept> targetChildren = new ArrayList<Concept>();
        Concept sourceConcept = conceptservice.readConcept(sourceSyn.getConceptID());
        for(Concept targetAncestor : targetAncestors)
        {
/*            if(conceptservice.isRelationExists(sourceConcept,
                    conceptservice.readConcept(targetAncestor.getId()) ,ConceptRelationType.MEMBER_OF))
            {
                return true;
            }*/
            if(spreadSearch(sourceConcept,targetAncestor))
            {
                return true;
            }
        }

    /*    switch (relation) {
            case MemberOf:
                for(Concept targetAncestor : targetAncestors)
                {
                    if(conceptservice.isRelationExists(sourceConcept,
                            conceptservice.readConcept(targetAncestor.getId()) ,ConceptRelationType.MEMBER_OF))
                    {
                        return true;
                    }
                    spreadSearch(sourceConcept,targetAncestor);
                }
                break;
            case PartOf:
                for(Concept targetAncestor : targetAncestors)
                {
                    if(conceptservice.isRelationExists(sourceConcept,
                            conceptservice.readConcept(targetAncestor.getId()) ,ConceptRelationType.PART_OF))
                    {
                        return true;
                    }
                }
                break;
            case SubstanceOf:
                for(Concept targetAncestor : targetAncestors)
                {
                    if(conceptservice.isRelationExists(sourceConcept,
                            conceptservice.readConcept(targetAncestor.getId()) ,ConceptRelationType.SUBSTANCE_OF))
                    {
                        return true;
                    }
                }
                break;
        }*/


        return false;
    }

    private boolean spreadSearch(Concept source, Concept target)
    {
        List<ConceptRelation> relations = target.getConceptRelations(true);
        for(ConceptRelation relation : relations)
        {
            ConceptRelationType rt = relation.getRelationType();
            if((rt.equals(ConceptRelationType.PART_OF)
                    || rt.equals(ConceptRelationType.MEMBER_OF)
                    || rt.equals(ConceptRelationType.SUBSTANCE_OF))
                    && source.equals(relation.getTarget()))
            {
                return true;
            }
        }
        for(ConceptRelation relation : relations)
        {
            ConceptRelationType rt = relation.getRelationType();
            if(rt.equals(ConceptRelationType.PART_OF)
                    || rt.equals(ConceptRelationType.MEMBER_OF)
                    || rt.equals(ConceptRelationType.SUBSTANCE_OF))
            {
                spreadSearch(source, relation.getTarget());
            }
        }

        return false;
    }

    @Override
    public String getGloss(long conceptID, String language) {
        Concept concept = conceptservice.readConcept(conceptID);
        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);
        Synset synset = concept.getSynset(voc);
        return synset.getGloss();
    }

    @Transactional
    @Override
    public List<ISense> getSenses(String derivation, String language) {
        try
        {
            derivation = derivation.toLowerCase(new Locale(language));
        }
        catch (Exception e)
        {
            derivation = derivation.toLowerCase();
        }

        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);
        List<String> lemmas = new ArrayList<String>();
        List<ISense> senseList = new ArrayList<ISense>();

        if (vocabularyservice.readMultiWordLemmas(voc).contains(derivation)) {
            lemmas.add(derivation);
        }
        if(lemmatizer.isLemmaExists(derivation, language)) {
            lemmas.add(derivation);
        } else {
            lemmas.addAll(mapToSet(lemmatizer.lemmatize(derivation, language)));
        }

        for(String lemma: lemmas) {
            Word word = vocabularyservice.readWord(voc,lemma);
            if(word == null) {
                continue;
            }
            List<Synset> synsets = word.getSynsets();
            for(int i=0;i<synsets.size();i++) {
                Concept concept = synsets.get(i).getConcept();
                ISense s = new UKCSense(concept.getId(),synsets.get(i).getId(),language,this);
                senseList.add(s);
            }
        }
        return senseList;
    }

    @Transactional
    @Override
    public List<String> getBaseForms(String derivation, String language) {
        try
        {
            derivation = derivation.toLowerCase(new Locale(language));
        }
        catch (Exception e)
        {
            derivation = derivation.toLowerCase();
        }

        Set<String> result = new HashSet<String>();
        KnowledgeBase kb = knowledgeBaseService.readKnowledgeBase("uk");
        Vocabulary voc = vocabularyservice.readVocabulary(kb,language);
        if (vocabularyservice.readMultiWordLemmas(voc).contains(derivation)) {
            result.add(derivation);
        }
        if (lemmatizer.isLemmaExists(derivation, language))
        {
            result.add(derivation);
        }
        Map<String,Set<String>> map = lemmatizer.lemmatize(derivation, language);
        result.addAll(mapToSet(map));
        if (0 == result.size()) {
            result.add(derivation);
        }

        List<String> ret = new ArrayList<String>();
        ret.addAll(result);
        return ret;
    }
    
    private Set<String> mapToSet(Map<String,Set<String>> map) {
        Set<String> set = new HashSet<String>();
        for (Set s : map.values()) {
            set.addAll(s);
        }
        return set;
    }
}
