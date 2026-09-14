package com.tistory.devyongsik.analyzer.dictionaryindex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.tistory.devyongsik.analyzer.dictionary.DictionaryFactory;

public class SynonymDictionaryIndexTest {

	@Test
	public void indexingDictionaryMakesSynonymGroupSearchable() throws Exception {
		SynonymDictionaryIndex index = SynonymDictionaryIndex.getIndexingModule();
		List<String> originalSynonyms = DictionaryFactory.getFactory().getSynonymList();
		SearcherManager searcherManager = index.getSearcherManager();
		IndexSearcher searcher = null;

		try {
			index.indexingDictionary(Lists.newArrayList("커버리지동의어,coverage-synonym"));
			searcherManager.maybeRefresh();
			searcher = searcherManager.acquire();

			assertEquals(1, searcher.search(new TermQuery(new Term("syn", "커버리지동의어")), 10).scoreDocs.length);
			assertEquals(1, searcher.search(new TermQuery(new Term("syn", "coverage-synonym")), 10).scoreDocs.length);
		} finally {
			if(searcher != null) {
				searcherManager.release(searcher);
			}
			index.indexingDictionary(originalSynonyms);
		}
	}

	@Test
	public void indexingDictionaryWrapsIndexingFailures() {
		SynonymDictionaryIndex index = SynonymDictionaryIndex.getIndexingModule();
		List<String> originalSynonyms = DictionaryFactory.getFactory().getSynonymList();

		try {
			index.indexingDictionary(null);
		} catch (IllegalStateException e) {
			assertEquals("Failed to index synonym dictionary", e.getMessage());
			assertTrue(e.getCause() instanceof NullPointerException);
			return;
		} finally {
			index.indexingDictionary(originalSynonyms);
		}

		throw new AssertionError("Expected IllegalStateException");
	}
}
