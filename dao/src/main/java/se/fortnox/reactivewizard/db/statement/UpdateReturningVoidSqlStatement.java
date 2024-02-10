package se.fortnox.reactivewizard.db.statement;

import se.fortnox.reactivewizard.db.query.ParamSetter;

import java.util.List;

public class UpdateReturningVoidSqlStatement extends UpdateBatchableSqlStatement<Void> {

    public UpdateReturningVoidSqlStatement(String sql, List<ParamSetter> paramSetters,
        int minimumAffected) {
        this(sql, paramSetters, minimumAffected, sql);
    }

    public UpdateReturningVoidSqlStatement(String sql, List<ParamSetter> paramSetters,
        int minimumAffected, String rawSql) {
        super(sql, paramSetters, minimumAffected, rawSql);
    }

    @Override
    protected void executed(int count) {
        if (monoSink != null) {
            monoSink.success();
        }
    }
}
