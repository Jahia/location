package org.jahia.modules.location;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.StaticOperand;

import org.apache.jackrabbit.commons.query.qom.OperandEvaluator;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.query.lucene.JackrabbitIndexSearcher;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.query.qom.ConstraintImpl;
import org.apache.jackrabbit.spi.commons.query.qom.QOMTreeVisitor;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.util.NumericUtils;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaRuntimeException;
import org.jahia.services.search.jcr.IndexExtender;
import org.jahia.services.search.jcr.QueryExtender;

public class Sql2ExtenderSpatialSearch implements IndexExtender, QueryExtender {

    private static final double MIN_SEARCH_RADIUS_KM = 1;
    private static final double MAX_SEARCH_RADIUS_KM = 500;
    private static final int MIN_TIER;
    private static final int MAX_TIER;
    static {
        CartesianTierPlotter tierPlotter = new CartesianTierPlotter(0, new SinusoidalProjector(), CartesianTierPlotter.DEFALT_FIELD_PREFIX);
        MIN_TIER = tierPlotter.bestFit(kilometerToMile(MAX_SEARCH_RADIUS_KM));
        MAX_TIER = tierPlotter.bestFit(kilometerToMile(MIN_SEARCH_RADIUS_KM));
    }

    private static final double KILOMETER_PER_MILE = 1.609344;

    private static final String LUCENE_FIELD_LATITUDE = "geo_latitude";
    private static final String LUCENE_FIELD_LONGITUDE = "geo_longitude";

    private static final String SQL2_FUNCTION_ISWITHINCIRCLE = "ISWITHINCIRCLE";

    @Override
    public void indexNode(NodeState node, ItemStateManager stateProvider, Document doc) {
        if (!node.getMixinTypeNames().contains(NameFactoryImpl.getInstance().create(Constants.JAHIAMIX_NS, "geotagged"))) {
            return;
        }
        Double latitude = null;
        Double longitude = null;
        for (Name propertyName : node.getPropertyNames()) {
            if (!Constants.JAHIA_NS.equals(propertyName.getNamespaceURI())) {
                continue;
            }
            String fieldName = propertyName.getLocalName();
            if (!(fieldName.equals("latitude") || fieldName.equals("longitude"))) {
                continue;
            }
            PropertyId propertyId = new PropertyId(node.getNodeId(), propertyName);
            PropertyState propertyState;
            try {
                propertyState = (PropertyState) stateProvider.getItemState(propertyId);
            } catch (ItemStateException e) {
                throw new JahiaRuntimeException(e);
            }
            try {
                if (fieldName.equals("latitude")) {
                    latitude = Double.parseDouble(propertyState.getValues()[0].getString());
                }
                if (fieldName.equals("longitude")) {
                    longitude = Double.parseDouble(propertyState.getValues()[0].getString());
                }
            } catch (RepositoryException e) {
                throw new JahiaRuntimeException(e);
            }
        }
        doc.add(new Field(LUCENE_FIELD_LATITUDE, NumericUtils.doubleToPrefixCoded(latitude), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(LUCENE_FIELD_LONGITUDE, NumericUtils.doubleToPrefixCoded(longitude), Field.Store.YES, Field.Index.NOT_ANALYZED));
        SinusoidalProjector projector = new SinusoidalProjector();
        for (int tier = MIN_TIER; tier <= MAX_TIER; tier++) {
            CartesianTierPlotter tierPlotter = new CartesianTierPlotter(tier, projector, CartesianTierPlotter.DEFALT_FIELD_PREFIX);
            double boxId = tierPlotter.getTierBoxId(latitude, longitude);
            doc.add(new Field(tierPlotter.getTierFieldName(), NumericUtils.doubleToPrefixCoded(boxId), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }

    @Override
    public Constraint parseConditionFunction(String functionName, QueryReader queryReader, NamePathResolver namePathResolver) {
        if (!SQL2_FUNCTION_ISWITHINCIRCLE.equalsIgnoreCase(functionName)) {
            return null;
        }
        StaticOperand latitude = queryReader.parseStaticOperand();
        queryReader.read(",");
        StaticOperand longitude = queryReader.parseStaticOperand();
        queryReader.read(",");
        StaticOperand radius = queryReader.parseStaticOperand();
        String selector;
        if (queryReader.readIf(",")) {
            selector = queryReader.readName();
        } else {
            selector = queryReader.getOnlySelectorName();
        }
        queryReader.read(")");
        return new WithinCircle(namePathResolver, selector, latitude, longitude, radius);
    }

    @Override
    public boolean append(Constraint constraint, QomFormatter qomFormatter) {
        if (!(constraint instanceof WithinCircle)) {
            return false;
        }
        WithinCircle withinCircle = (WithinCircle) constraint;
        qomFormatter.append(SQL2_FUNCTION_ISWITHINCIRCLE);
        qomFormatter.append("(");
        qomFormatter.append(withinCircle.getLatitude());
        qomFormatter.append(", ");
        qomFormatter.append(withinCircle.getLongitude());
        qomFormatter.append(", ");
        qomFormatter.append(withinCircle.getRadius());
        qomFormatter.append(", ");
        qomFormatter.appendName(withinCircle.getSelectorName());
        qomFormatter.append(")");
        return true;
    }

    @Override
    public Query getQuery(Constraint constraint, Map<String, NodeType> selectorMap, JackrabbitIndexSearcher searcher, OperandEvaluator operandEvaluator) {
        if (!(constraint instanceof WithinCircle)) {
            return null;
        }
        WithinCircle withinCircle = (WithinCircle) constraint;
        double latitude;
        double longitude;
        double radius;
        try {
            latitude = operandEvaluator.getValue(withinCircle.getLatitude()).getDouble();
            longitude = operandEvaluator.getValue(withinCircle.getLongitude()).getDouble();
            radius = operandEvaluator.getValue(withinCircle.getRadius()).getDouble();
        } catch (RepositoryException e) {
            throw new JahiaRuntimeException(e);
        }
        if (radius < MIN_SEARCH_RADIUS_KM || radius > MAX_SEARCH_RADIUS_KM) {
            throw new IllegalArgumentException("Search radius must be within the range of (" + MIN_SEARCH_RADIUS_KM + ", " + MAX_SEARCH_RADIUS_KM + ") km");
        }
        DistanceQueryBuilder queryBuilder = new DistanceQueryBuilder(latitude, longitude, kilometerToMile(radius), LUCENE_FIELD_LATITUDE, LUCENE_FIELD_LONGITUDE, CartesianTierPlotter.DEFALT_FIELD_PREFIX, true);
        return queryBuilder.getQuery(new MatchAllDocsQuery());
    }

    public static double kilometerToMile(double kilometer) {
        return kilometer / KILOMETER_PER_MILE;
    }

    public static double mileToKilometer(double mile) {
        return mile * KILOMETER_PER_MILE;
    }

    private static class WithinCircle extends ConstraintImpl {

        private Name selectorName;
        private StaticOperand latitude;
        private StaticOperand longitude;
        private StaticOperand radius;

        public WithinCircle(NamePathResolver namePathResolver, String selectorName, StaticOperand latitude, StaticOperand longitude, StaticOperand radius) {
            super(namePathResolver);
            try {
                this.selectorName = namePathResolver.getQName(selectorName);
            } catch (RepositoryException e) {
                throw new JahiaRuntimeException(e);
            }
            this.latitude = latitude;
            this.longitude = longitude;
            this.radius = radius;
        }

        public String getSelectorName() {
            return getJCRName(selectorName);
        }

        public StaticOperand getLatitude() {
            return latitude;
        }

        public StaticOperand getLongitude() {
            return longitude;
        }

        public StaticOperand getRadius() {
            return radius;
        }

        @Override
        public Object accept(QOMTreeVisitor visitor, Object data) throws Exception {
            return null;
        }
    }
}
