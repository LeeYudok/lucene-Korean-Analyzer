# OpenSearch 한국어 분석 플러그인

이 포크의 `KoreanAnalyzer`를 `doksam_korean`으로 등록한다. Nori와 독립적으로 공존한다.
원형·명사·복합명사·동의어·형태소 엔진과 패키지 내 사전을 사용한다. 사전 파일을 노드 전역에서
수정하는 설정은 제공하지 않는다. 모델 임베딩과 관계없는 Lucene 텍스트 분석 플러그인이다.

## 호환 버전과 빌드

- OpenSearch **3.7.0**, Lucene **10.4.0**, Java **21 이상**으로 고정.
- `korean-analyzer-10.x`는 9.x의 소스·사전을 공유하여 빌드하며, 8.x/9.x 산출물은 유지한다.
- OpenSearch/Lucene jar는 서버가 제공하므로 ZIP에 넣지 않는다. 분석기 jar와 SLF4J API만 포함한다.
- 다른 OpenSearch 버전에 그대로 설치하지 않는다. 의존성·descriptor·이미지를 함께 변경하고 검증한다.

```sh
./gradlew test :opensearch-analysis-korean:pluginZip
# 결과: opensearch-analysis-korean/target/distributions/analysis-korean-1.0.0-opensearch-3.7.0.zip
podman build -t localhost/opensearch-korean:3.7.0-1.0.0 \
  -f deploy/Containerfile opensearch-analysis-korean/target/distributions
```

이미지는 공식 OpenSearch 3.7.0에 Nori와 이 플러그인을 함께 설치한다. 컨테이너 내부에만 설치한
파일은 재생성 때 사라지므로, 운영 Quadlet의 Image 또는 영속 drop-in에서 이 이미지를 지정한다.
ZIP과 이미지 빌드에 사용한 소스 커밋·SHA256을 배포 기록에 남긴다.

## 인덱스와 검색

```http
PUT korean-documents
{
  "settings": {"number_of_shards": 1, "number_of_replicas": 0},
  "mappings": {
    "properties": {
      "text": {"type": "text", "analyzer": "doksam_korean"}
    }
  }
}

POST korean-documents/_analyze
{"analyzer": "doksam_korean", "text": "점심 식사 메뉴는 비빔밥과 된장국이다."}

PUT korean-documents/_doc/1?refresh=true
{"text": "점심 식사 메뉴는 비빔밥과 된장국이다."}

GET korean-documents/_search
{"query": {"match": {"text": "된장"}}}
```

실측 토큰: `점심, 식사, 메뉴는, 메뉴, 비빔밥과, 된장국이다, 된장, 된장국`.
명사만 반환하는 Nori와 결과가 같지는 않다. 형태소 분석은 임의 부분 문자열 검색도 아니므로
별도 n-gram 필드가 필요한 요구는 따로 검증한다. Nori와 비교할 때는 `decompound_mode=mixed`로
원형을 보존한다. Nori 기본 모드에서는 단독 `된장`이 `된`, `장`으로 분석되어 검색이 달라졌다.

필요하면 설정한 분석기에서 `type=doksam_korean`, `indexing_mode=false`를 사용할 수 있다.
기본값 true가 전체 엔진을 사용하며 false는 기존 query-mode처럼 base-noun 엔진을 생략한다.

## 검증

```sh
# 별도 테스트 컨테이너; 운영 데이터 볼륨을 연결하지 않는다.
podman run -d --name korean-analysis-test -p 127.0.0.1:19227:9200 \
  -e discovery.type=single-node -e DISABLE_SECURITY_PLUGIN=true \
  -e DISABLE_INSTALL_DEMO_CONFIG=true -e 'OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m' \
  localhost/opensearch-korean:3.7.0-1.0.0
# /_cluster/health가 응답한 뒤 실행
OPENSEARCH_URL=http://127.0.0.1:19227 python3 scripts/test-opensearch.py
podman rm -f korean-analysis-test
```

통합 검사는 고유 이름의 인덱스만 생성/삭제한다. 사전 로딩, 동시 분석, 빈 입력, 영문,
실제 된장 색인/검색, Nori 공존, 문서 필터, k-NN을 검사한다. 합성 데이터의 기능 검증이며
한국어 검색 품질 전반이나 대규모 처리 성능 벤치마크는 아니다.

## 기존 데이터 적용·롤백

플러그인 설치만으로 기존 필드 분석기는 바뀌지 않는다. 새 인덱스에 재색인하거나
`text.fork` 같은 새 multi-field를 추가한 뒤 `_update_by_query`로 기존 문서를 다시 색인한다.
운영 대량 데이터에는 작업 부하·실패 문서·색인 버전을 별도로 관리한다.

노드를 재시작하기 전에 설치 ZIP을 격리 컨테이너에서 검증한다. 분석기를 참조하는 인덱스가
생긴 뒤 플러그인 없는 이미지로 돌아가면 인덱스가 열리지 않을 수 있다. 롤백 이미지에도
플러그인을 유지하거나, 의존 인덱스를 먼저 호환 인덱스로 재색인한다. 기존 Nori 필드를
참조하는 데이터가 있으므로 배포 이미지에서 Nori도 제거하지 않는다.
