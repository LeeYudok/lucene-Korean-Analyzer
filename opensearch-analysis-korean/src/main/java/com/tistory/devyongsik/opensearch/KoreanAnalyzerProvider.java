package com.tistory.devyongsik.opensearch;

import com.tistory.devyongsik.analyzer.KoreanAnalyzer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractIndexAnalyzerProvider;

/** Uses packaged dictionaries; no node-global dictionary mutation is exposed. */
public final class KoreanAnalyzerProvider extends AbstractIndexAnalyzerProvider<KoreanAnalyzer> {
    private final KoreanAnalyzer analyzer;

    public KoreanAnalyzerProvider(IndexSettings index, Environment environment, String name, Settings settings) {
        super(index, name, settings);
        analyzer = new KoreanAnalyzer(settings.getAsBoolean("indexing_mode", true));
    }

    @Override
    public KoreanAnalyzer get() {
        return analyzer;
    }
}
