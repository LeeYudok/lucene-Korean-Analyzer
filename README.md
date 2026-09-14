lucene-Korean-Analyzer
======================

Lucene Analyzer For Korean

이수명님의 Analyzer(http://cafe.naver.com/korlucene)를 형태소 분석 Filter 형태로 변형하고, 동의어 Filter와 복합명사 Filter 등을 추가한 Lucene용 한글 분석기입니다.

Lucene 메이저 버전 라인별로 모듈을 나누어 관리합니다.

## Modules

```text
korean-analyzer-9.x  — Java 11 / Lucene 9.10.0
korean-analyzer-10.x — Java 21 / Lucene 10.4.0 (9.x 소스·사전 공유)
opensearch-analysis-korean — OpenSearch 3.7.0 설치 플러그인
korean-analyzer-8.x  — Java 8  / Lucene 8.11.4 (Java 8 제약 환경용)
```

8.x 모듈의 빌드 방법과 배포 시 필요한 런타임 의존성 목록은 [`korean-analyzer-8.x/README.md`](korean-analyzer-8.x/README.md)를 참고하세요.

## Build

```bash
./gradlew clean test
```

## Resources

Analyzer dictionaries and properties live under `korean-analyzer-9.x/src/main/resources` and are included by Gradle's standard resource processing.

## Analyzer Pipeline

```text
입력 텍스트
→ KoreanCharacterTokenizer
→ KoreanNounFilter
   → KoreanCompoundNounEngine
   → KoreanBaseNounEngine
   → KoreanLongestNounEngine
   → KoreanSynonymEngine
   → KoreanMorphEngine
→ KoreanStopFilter
→ Lucene TokenStream
```

## OpenSearch 배포

[doksam_korean 플러그인 빌드·설치·검색·롤백](opensearch-analysis-korean/README.md).
