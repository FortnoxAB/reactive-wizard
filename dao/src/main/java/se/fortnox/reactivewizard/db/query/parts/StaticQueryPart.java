package se.fortnox.reactivewizard.db.query.parts;

import se.fortnox.reactivewizard.db.query.PreparedStatementParameters;

public class StaticQueryPart implements QueryPart {

    private final String part;

    public StaticQueryPart(String part) {
        this.part = part;
    }

    @Override
    public void visit(StringBuilder sql, Object[] args) {
        sql.append(part);
    }

    @Override
    public void addParams(PreparedStatementParameters preparedStatement, Object[] args) {

    }
}
