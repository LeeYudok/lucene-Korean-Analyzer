#!/usr/bin/env python3
"""Integration test against an opt-in OpenSearch URL; only its own index is removed."""
import concurrent.futures
import json
import os
import urllib.request
import uuid

BASE = os.environ.get('OPENSEARCH_URL', 'http://127.0.0.1:19227').rstrip('/')
INDEX = 'korean-plugin-test-' + uuid.uuid4().hex


def request(method, path, body=None):
    headers = {'Content-Type': 'application/json'}
    if os.environ.get('OPENSEARCH_AUTHORIZATION'):
        headers['Authorization'] = os.environ['OPENSEARCH_AUTHORIZATION']
    req = urllib.request.Request(BASE + path, method=method, headers=headers,
                                 data=None if body is None else json.dumps(body).encode())
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)


def analyze(text):
    return [token['token'] for token in request('POST', '/' + INDEX + '/_analyze',
            {'analyzer': 'doksam_korean', 'text': text})['tokens']]


def main():
    created = False
    try:
        request('PUT', '/' + INDEX, {
            'settings': {'number_of_shards': 1, 'number_of_replicas': 0, 'index.knn': True,
                         'analysis': {'analyzer': {'nori_compare': {'type': 'nori', 'decompound_mode': 'mixed'}}}},
            'mappings': {'properties': {
                'text': {'type': 'text', 'analyzer': 'doksam_korean',
                         'fields': {'nori': {'type': 'text', 'analyzer': 'nori_compare'}}},
                'doc_id': {'type': 'keyword'},
                'vector': {'type': 'knn_vector', 'dimension': 2,
                           'method': {'name': 'hnsw', 'engine': 'lucene', 'space_type': 'cosinesimil'}}}}})
        created = True
        sentence = '점심 식사 메뉴는 비빔밥과 된장국이다.'
        tokens = analyze(sentence)
        assert '된장' in tokens, tokens
        assert analyze('') == []
        assert 'opensearch' in analyze('OpenSearch')
        # Each request can reuse a different thread's analyzer components.
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            outputs = list(pool.map(analyze, [sentence, ''] * 8))
        assert all('된장' in v if i % 2 == 0 else v == [] for i, v in enumerate(outputs))
        for i, text in enumerate([sentence, '대출 연체 위험을 평가한다.', sentence]):
            request('PUT', '/' + INDEX + '/_doc/' + str(i), {
                'text': text, 'doc_id': 'other' if i == 2 else 'target',
                'vector': [1.0, 0.0] if i != 1 else [0.0, 1.0]})
        request('POST', '/' + INDEX + '/_refresh')
        for field in ['text', 'text.nori']:
            result = request('POST', '/' + INDEX + '/_search', {
                'query': {'bool': {'must': [{'match': {field: '된장'}}],
                                   'filter': [{'term': {'doc_id': 'target'}}]}}})
            assert result['_shards']['failed'] == 0 and not result['timed_out']
            assert [h['_id'] for h in result['hits']['hits']] == ['0'], result
        result = request('POST', '/' + INDEX + '/_search', {
            'size': 1, 'query': {'knn': {'vector': {'vector': [1.0, 0.0], 'k': 1,
                                  'filter': {'term': {'doc_id': 'target'}}}}}})
        assert result['hits']['hits'][0]['_id'] == '0', result
        print(json.dumps({'pass': True, 'analyzer': 'doksam_korean', 'tokens': tokens,
                          'checks': ['packaged_dictionary', 'concurrent_reuse', 'empty', 'latin',
                                     'index_search', 'nori_coexistence', 'document_filter', 'knn']}, ensure_ascii=False))
    finally:
        if created:
            request('DELETE', '/' + INDEX)


if __name__ == '__main__':
    main()
