package com.scaleset.search.es;

import com.scaleset.search.*;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.types.TypesExistsResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.client.Requests.createIndexRequest;
import static org.elasticsearch.client.Requests.deleteIndexRequest;

public class ElasticSearchDao<T, K> extends AbstractSearchDao<T, K> implements GenericSearchDao<T, K> {

    protected Client client;
    protected SearchMapping<T, K> mapping;
    private Logger log = LoggerFactory.getLogger(getClass());

    public ElasticSearchDao(Client client, SearchMapping<T, K> mapping) {
        this.client = client;
        this.mapping = mapping;
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    @Override
    public void delete(T entity) throws Exception {
        String index = mapping.index(entity);
        String type = mapping.type(entity);
        String id = mapping.id(entity);
        client.prepareDelete(index, type, id).execute().actionGet();
    }

    @Override
    public void deleteByKey(K key) throws Exception {
        String index = mapping.indexForKey(key);
        String type = mapping.typeForKey(key);
        String id = mapping.idForKey(key);
        client.prepareDelete(index, type, id).execute().actionGet();
    }

    @Override
    public void deleteByQuery(Query query) throws Exception {
        DeleteByQueryRequestBuilder builder = createConverter(query).deleteRequest();
        DeleteByQueryResponse response = builder.execute().actionGet();
    }


    @Override
    public T findById(K key) throws Exception {
        String index = mapping.indexForKey(key);
        String type = mapping.typeForKey(key);
        String id = mapping.idForKey(key);
        try {
            GetResponse response = client.prepareGet(index, type, id).execute().actionGet();
            String source = response.getSourceAsString();
            if (source == null) {
                return null;
            } else {
                T result = mapping.fromDocument(id, source);
                return result;
            }
        } catch (Exception e) {
            log.error("error retrieving dao", e);
            throw e;
        }
    }

    @Override
    public T save(T entity) throws Exception {
        String id = mapping.id(entity);
        String index = mapping.index(entity);
        String type = mapping.type(entity);
        String source = mapping.toDocument(entity);
        client.prepareIndex(index, type, id).setSource(source).execute().actionGet();
        T result = entity;
        return result;
    }

    public List<T> saveBatch(List<T> entities) throws Exception {
        List<T> result = new ArrayList<>();
        BulkRequestBuilder bulkRequest = client.prepareBulk();

        for (T entity : entities) {
            String id = mapping.id(entity);
            String index = mapping.index(entity);
            String type = mapping.type(entity);
            String source = mapping.toDocument(entity);
            bulkRequest.add(client.prepareIndex(index, type, id).setSource(source));
            result.add(entity);
        }

        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            log.error("Bulk ingest has errors");
        }
        return result;
    }

    public Results<T> search(Query query) throws Exception {
        Query updated = new QueryBuilder(query).build();
        SearchRequestBuilder srb = convert(updated);
        SearchResponse response = srb.execute().actionGet();
        Results<T> results = createResultsConverter(updated, response, mapping).convert();
        return results;
    }

    public SearchRequestBuilder convert(Query query) throws Exception {
        return createConverter(query).searchRequest();
    }

    /**
     * Intended only for testing
     */
    public void flush() {
        client.admin().indices().prepareRefresh().execute().actionGet();
    }

    public void createIndex(String indexName) {
        CreateIndexResponse createResponse = client.admin().indices().create(createIndexRequest(indexName)).actionGet();
    }

    public void deleteIndex(String indexName) {
        DeleteIndexResponse deleteIndexResponse =
                client.admin().indices().delete(deleteIndexRequest(indexName)).actionGet();
    }

    public void recreateMapping(String index, String type, String schema) {

        // drop mapping incase it exists
        try {
            client.admin().indices().prepareDeleteMapping(index).setType(type).execute().actionGet();
        } catch (Exception e) {
        }
        // recreate mapping
        client.admin().indices().preparePutMapping(index).setType(type).setSource(schema).execute().actionGet();
    }

    public boolean indexExists(String name) {
        boolean result = false;
        try {
            IndicesExistsResponse exists = client.admin().indices().exists(new IndicesExistsRequest(name)).actionGet();
            result = exists.isExists();
        } catch (Exception e) {
            log.error("Error checking index exists");
        }
        return result;
    }

    public boolean mappingExists(String indexName, String typeName) {
        boolean result = false;
        try {
            String[] indices = new String[]{indexName};
            TypesExistsResponse exists = client.admin().indices().typesExists(new TypesExistsRequest(indices, typeName)).actionGet();
            result = exists.isExists();
        } catch (Exception e) {
            log.error("Error checking type exists");
        }
        return result;
    }

    protected SearchMapping<T, K> getMapping() {
        return mapping;
    }

    protected DefaultQueryConverter createConverter(Query query) throws Exception {
        String index = mapping.indexForQuery(query);
        String type = mapping.typeForQuery(query);
        return new DefaultQueryConverter(client, query, index, type);
    }

    protected ResultsConverter<T, K> createResultsConverter(Query query, SearchResponse response, SearchMapping<T, K> mapping) throws Exception {
        return new ResultsConverter<T, K>(query, response, mapping);
    }

}
