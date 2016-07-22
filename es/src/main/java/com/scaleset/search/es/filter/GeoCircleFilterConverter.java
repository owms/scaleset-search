package com.scaleset.search.es.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaleset.geo.geojson.GeoJsonModule;
import com.scaleset.search.Filter;
import com.scaleset.utils.Coerce;
import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.geo.builders.ShapeBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

public class GeoCircleFilterConverter implements FilterConverter {

    private Coerce coerce = new Coerce(new ObjectMapper().registerModule(new GeoJsonModule()));

    public static final JtsSpatialContext SPATIAL_CONTEXT = JtsSpatialContext.GEO;
    public static final GeometryFactory FACTORY = SPATIAL_CONTEXT.getGeometryFactory();
    protected final boolean multiPolygonMayOverlap = false;

    @Override
    public FilterBuilder convert(Filter filter) {
        String field = filter.getString("field");
        Coordinate geometry = filter.get(Coordinate.class, "geometry");
        String radius = filter.getString("radius");
        ShapeBuilder shapeBuilder = ShapeBuilder.newCircleBuilder().center(geometry).radius(radius);
        FilterBuilder result = FilterBuilders.geoWithinFilter(field, shapeBuilder);
        return result;
    }

}
