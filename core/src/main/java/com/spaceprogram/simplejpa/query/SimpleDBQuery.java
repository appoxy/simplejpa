package com.spaceprogram.simplejpa.query;

import com.amazonaws.services.s3.internal.S3QueryStringSigner;
import com.spaceprogram.simplejpa.EntityManagerSimpleJPA;

import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kerry Wright
 */
public class SimpleDBQuery extends AbstractQuery {
    private final String originalQuery;
    private static final Pattern COUNT_REGEX = Pattern.compile("select(\\s+.*\\s+)from\\s+.*", Pattern.CASE_INSENSITIVE);

    public SimpleDBQuery(EntityManagerSimpleJPA em, String originalQuery) {
        super(em);
        this.originalQuery = originalQuery;
    }

    @Override
    public int getCount() {
        AmazonQueryString aq = createAmazonQuery();
        if(aq.isCount()) return Integer.parseInt(getSingleResult().toString());

        String countQuery = convertToCountQuery(aq);
        return Integer.parseInt(new SimpleDBQuery(em, countQuery).getSingleResult().toString());
    }

    static String convertToCountQuery(AmazonQueryString aq) {
        Matcher m = COUNT_REGEX.matcher(aq.getValue());
        if (!m.find()) throw new IllegalArgumentException("Can not convert query to a count query: "+aq.getValue());
        String replaceGroup = m.group(1);
        return aq.getValue().replace(replaceGroup, " count(*) ");
    }

    @Override
    public AmazonQueryString createAmazonQuery(boolean appendLimit) {
        String replacedQuery = replaceQueryParameters(originalQuery, getParameters());
        boolean isCount = replacedQuery.trim().toLowerCase().matches("select\\s+count\\s*\\(\\s*\\*\\s*\\)\\s+.*");
        return new AmazonQueryString(replacedQuery, isCount);
    }

    private String replaceQueryParameters(String originalQuery, Map<String, Object> parameters) {
        originalQuery = originalQuery+" "; // pad a space on the end for easier matching
        for (Map.Entry<String, Object> entry : getParameters().entrySet()) {
            String stringVal = convertToSimpleDBValue(entry.getValue(), entry.getValue().getClass());
            originalQuery = originalQuery.replaceAll(":"+entry.getKey()+" ", "'"+stringVal+"' ");
        }
        return originalQuery.trim();
    }
}
