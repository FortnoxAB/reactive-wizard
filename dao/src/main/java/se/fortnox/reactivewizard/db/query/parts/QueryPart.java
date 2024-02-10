package se.fortnox.reactivewizard.db.query.parts;

import se.fortnox.reactivewizard.db.query.ParamSetter;

import java.util.List;

public interface QueryPart {

    void visit(StringBuilder sql, Object[] args);

    void addParamSetter(List<ParamSetter> paramSetters, Object[] args);
}
