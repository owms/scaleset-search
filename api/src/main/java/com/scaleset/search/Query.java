package com.scaleset.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.vividsolutions.jts.geom.Envelope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_EMPTY)
public class Query {

    private HashMap<String, Aggregation> aggs = new HashMap<>();

    private Envelope bbox;

    private boolean echo = false;

    private Map<String, Filter> filters = new HashMap<>();

    private String[] fields;

    private String fieldSet;

    private String geoField;

    private int limit;

    private int offset = 0;

    private Map<String, Object> headers = new HashMap<>();

    private String q;

    private Sort[] sorts;

    /* For Jackson */
    public Query() {
    }

    public Query(String q, Envelope bbox, boolean echo, String geoField, String fieldSet, List<String> fields, int offset, int limit, List<Sort> sorts, Map<String, Aggregation> aggs, Map<String, Filter> filters, Map<String, Object> headers) {
        this.q = q;
        this.offset = offset;
        this.limit = limit;
        this.bbox = bbox;
        this.echo = echo;
        this.geoField = geoField;
        this.fields = fields.toArray(new String[fields.size()]);
        this.fieldSet = fieldSet;
        this.sorts = sorts.toArray(new Sort[sorts.size()]);
        if (aggs != null) {
            for (String name : aggs.keySet()) {
                Aggregation agg = aggs.get(name);
                this.aggs.put(name, new Aggregation(name, agg.getType(), agg.anyGetter(), agg.getAggs()));
            }
        }
        if (filters != null) {
            for (String name : filters.keySet()) {
                Filter filter = filters.get(name);
                this.filters.put(name, new Filter(name, filter.getType(), filter.anyGetter()));
            }
            this.filters.putAll(filters);
        }
        if (headers != null) {
            this.headers.putAll(headers);
        }
    }

    public Map<String, Aggregation> getAggs() {
        return aggs;
    }

    public Envelope getBbox() {
        return bbox;
    }

    public String getFieldSet() {
        return fieldSet;
    }

    public boolean getEcho() {
        return echo;
    }

    public String[] getFields() {
        return fields;
    }

    public Map<String, Filter> getFilters() {
        return filters;
    }

    public String getGeoField() {
        return geoField;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public String getQ() {
        return q;
    }

    public Sort[] getSorts() {
        return sorts;
    }

    public boolean isEcho() {
        return echo;
    }

    public void setEcho(boolean echo) {
        this.echo = echo;
    }
}
