package org.everit.osgi.servicecollector.internal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.utils.manifest.Attribute;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

public class ReferenceParameter {

    private final String referenceName;

    private final Class<?> clazz;

    private final boolean optional;

    private final Filter filter;

    private final Map<String, String> attributes;

    private final String clause;

    public ReferenceParameter(String referenceName, Class<?> clazz, String clause) {
        this.referenceName = referenceName;
        this.clazz = clazz;
        this.clause = clause;

        Clause[] clauses = Parser.parseClauses(new String[] { clause });
        if (clauses.length != 1) {
            throw new IllegalArgumentException("Clause parameter should contain exactly one clause, not "
                    + clauses.length + ": " + clause);
        }

        Clause tmpClause = clauses[0];
        String resolution = tmpClause.getDirective(Constants.RESOLUTION_DIRECTIVE);
        if (resolution != null) {
            if (Constants.RESOLUTION_OPTIONAL.equals(resolution)) {
                optional = true;
            } else if (Constants.RESOLUTION_MANDATORY.equals(resolution)) {
                optional = false;
            } else {
                throw new IllegalArgumentException("Unknown resolution type in clause: " + clause);
            }
        } else {
            optional = false;
        }

        String filterDirective = tmpClause.getDirective(Constants.FILTER_DIRECTIVE);
        try {
            this.filter = FrameworkUtil.createFilter(filterDirective);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Cannot parse filter directive in clause: " + clause, e);
        }

        Map<String, String> attributeMap = new LinkedHashMap<String, String>();

        Attribute[] attributeArray = tmpClause.getAttributes();
        for (Attribute attribute : attributeArray) {
            String oldValue = attributeMap.put(attribute.getName(), attribute.getValue());
            if (oldValue != null) {
                throw new IllegalArgumentException("Attribute name '" + attribute.getName()
                        + "' exists more than once in clause: " + clause);
            }
        }
        this.attributes = Collections.unmodifiableMap(attributeMap);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getClause() {
        return clause;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Filter getFilter() {
        return filter;
    }

    public String getReferenceName() {
        return referenceName;
    }

    public boolean isOptional() {
        return optional;
    }
}
