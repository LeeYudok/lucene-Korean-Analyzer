# AGENTS.md

에이전트/AI 도구가 이 레포에서 작업할 때 알아야 할 사실 모음.

## 프로젝트 개요

- 이수명님의 한글 Analyzer를 기반으로 형태소 분석 Filter, 동의어 Filter, 복합명사 Filter를 추가한 Lucene용 한글 분석기.
- 사전 기반 매칭 + 휴리스틱 방식. Lucene 공식 Nori(mecab-ko-dic + FST + Viterbi)와 달리 텍스트 사전을 직접 수정할 수 있어 사전/분해 로직 커스터마이징이 쉬운 것이 존재 이유.

## 빌드 환경

- **Java 11 필수** (`build.gradle`의 source/target compatibility가 11). Java 8은 지원하지 않으며, Lucene 9.x 자체가 Java 11+를 요구하므로 낮출 수 없음.
- 활성 모듈은 `korean-analyzer-9.x` 하나뿐. 구 Lucene 3.x 모듈은 제거됨 (`jdk-11-upgrade.md` 참고).
- 주요 의존성: Lucene 9.10.0, SLF4J 2.0.13, Logback 1.5.6, Guava 33.2.0-jre, JUnit 4.13.2. Gradle wrapper 8.7.
- 빌드/테스트: `./gradlew clean test`. `buildDir`는 관례와 달리 `target`으로 설정되어 있음.

## Git 리모트

- `origin` = leeyudok/lucene-Korean-Analyzer (포크)
- `upstream` = need4spd/lucene-Korean-Analyzer (원본, 2026-07-16 기준 origin과 동일 커밋)
- 동기화: `git fetch upstream && git merge upstream/master`

## 리소스

- 사전·프로퍼티는 `korean-analyzer-9.x/src/main/resources` 아래에 있고 Gradle 표준 리소스 처리로 포함됨.
- 분석 파이프라인: KoreanCharacterTokenizer → KoreanNounFilter(CompoundNoun/BaseNoun/LongestNoun/Synonym/Morph 엔진) → KoreanStopFilter.
