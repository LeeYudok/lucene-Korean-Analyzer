package com.tistory.devyongsik.opensearch;

import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.opensearch.index.analysis.AnalyzerProvider;
import org.opensearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.opensearch.plugins.AnalysisPlugin;
import org.opensearch.plugins.Plugin;

/** Registers the fork's analyzer under a distinct name so Nori can coexist. */
public final class KoreanAnalysisPlugin extends Plugin implements AnalysisPlugin {
    @Override
    public Map<String, AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        return Map.of("doksam_korean", KoreanAnalyzerProvider::new);
    }
}
