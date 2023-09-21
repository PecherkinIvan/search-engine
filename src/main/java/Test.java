import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        LuceneMorphology lm = new EnglishLuceneMorphology();
        System.out.println(lm.getNormalForms("esping-et"));
        String word = "лмвnак";
        System.out.println(word.matches("[а-я-]+"));
    }
}
