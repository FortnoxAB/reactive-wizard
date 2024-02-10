package se.fortnox.reactivewizard.db.statement;

import se.fortnox.reactivewizard.db.query.ParamSetter;

import java.util.List;

public class UpdateReturningCountSqlStatement extends UpdateBatchableSqlStatement<Integer> {

    public UpdateReturningCountSqlStatement(String sql, List<ParamSetter> paramSetters,
        int minimumAffected) {
        this(sql, paramSetters, minimumAffected, sql);
    }

    public UpdateReturningCountSqlStatement(String sql, List<ParamSetter> paramSetters,
        int minimumAffected, String rawSql) {
        super(sql, paramSetters, minimumAffected, rawSql);
    }

    @Override
    protected void executed(int count) {
        if (fluxSink != null) {
            fluxSink.next(count);
        } else if (monoSink != null) {
            monoSink.success(count);
        }
    }
}
