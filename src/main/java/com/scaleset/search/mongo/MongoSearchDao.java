package com.scaleset.search.mongo;

import com.fasterxml.jackson.databind.Module;
import com.mongodb.DB;
import com.scaleset.geo.geojson.GeoJsonModule;
import com.scaleset.search.AbstractSearchDao;
import com.scaleset.search.GenericSearchDao;
import com.scaleset.search.Query;
import com.scaleset.search.Results;
import org.jongo.Find;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;

import java.util.ArrayList;
import java.util.List;

public class MongoSearchDao<T, K> extends AbstractSearchDao<T, K> {

    private MongoCollection collection;
    private Class<T> typeClass;

    public MongoSearchDao(DB db, String collectionName, Class<T> typeClass, Module... modules) {
        this.typeClass = typeClass;
        JacksonMapper.Builder mapperBuilder = new JacksonMapper.Builder();
        mapperBuilder.registerModule(new GeoJsonModule());
        for (Module module : modules) {
            mapperBuilder.registerModule(module);
        }
        mapperBuilder.withQueryFactory(new LuceneJongoQueryFactory());
        Jongo jongo = new Jongo(db, mapperBuilder.build());
        collection = jongo.getCollection(collectionName);
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public Results<T> search(Query query) throws Exception {
        Find find = collection
                .find(query.getQ())
                .limit(query.getLimit())
                .skip(query.getOffset());
        Results<T> results = new ResultsConverter<T, K>(query, find, typeClass).convert();
        return results;
    }

    @Override
    public T findById(K id) throws Exception {
        return collection.findOne("_id:#", id).as(typeClass);
    }

    @Override
    public List<T> saveBatch(List<T> entities) throws Exception {
        List<T> results = new ArrayList<>();
        for (T entity : entities) {
            save(entity);
            results.add(entity);
        }
        return results;
    }

    @Override
    public void delete(T entity) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByKey(K id) throws Exception {
        collection.remove("_id: #", id);
    }

    @Override
    public void deleteByQuery(Query query) throws Exception {
        collection.remove(query.getQ());
    }

    @Override
    public T save(T entity) throws Exception {
        collection.save(entity);
        return entity;
    }

}
