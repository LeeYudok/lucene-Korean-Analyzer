package com.tistory.devyongsik.analyzer;

import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.Test;
import static org.junit.Assert.*;

public class Lucene10SearchTest {
    private List<String> terms(KoreanAnalyzer analyzer, String text) throws Exception {
        List<String> values = new ArrayList<>();
        try (TokenStream stream = analyzer.tokenStream("text", text)) {
            CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) values.add(term.toString());
            stream.end();
        }
        return values;
    }

    @Test
    public void packagedDictionarySupportsKoreanSearchAndReuse() throws Exception {
        try (KoreanAnalyzer analyzer = new KoreanAnalyzer(); ByteBuffersDirectory dir = new ByteBuffersDirectory()) {
            assertTrue(terms(analyzer, "점심 식사 메뉴는 비빔밥과 된장국이다.").contains("된장"));
            assertTrue(terms(analyzer, "").isEmpty());
            try (IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(analyzer))) {
                for (String text : List.of("점심 식사 메뉴는 비빔밥과 된장국이다.", "대출 연체 위험을 평가한다.")) {
                    Document document = new Document();
                    document.add(new TextField("text", text, Field.Store.YES));
                    writer.addDocument(document);
                }
                writer.commit();
                try (DirectoryReader reader = DirectoryReader.open(writer)) {
                    IndexSearcher searcher = new IndexSearcher(reader);
                    var hits = searcher.search(new TermQuery(new Term("text", "된장")), 10);
                    assertEquals(1, hits.scoreDocs.length);
                    assertTrue(searcher.storedFields().document(hits.scoreDocs[0].doc).get("text").contains("된장국"));
                }
            }
        }
    }

    @Test
    public void resetDiscardsUnconsumedTokens() throws Exception {
        try (KoreanAnalyzer analyzer = new KoreanAnalyzer()) {
            try (TokenStream stream = analyzer.tokenStream("text", "된장국이다")) {
                stream.reset();
                assertTrue(stream.incrementToken());
                // A caller may close early (for example an analysis token limit).
            }
            assertTrue(terms(analyzer, "").isEmpty());
        }
    }
}
