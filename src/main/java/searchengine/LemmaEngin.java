package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.services.morphology.LemmaFinder;
import searchengine.services.morphology.LemmaIndexer;

import java.io.IOException;
import java.util.List;

public class LemmaEngin {
    public static void main(String[] args) throws IOException {
        String text = "";
        LemmaIndexer lemmaIndexer = new LemmaIndexer();

        System.out.println(lemmaIndexer.getLemmaMap(text));


    }
}
