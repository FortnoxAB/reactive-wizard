package se.fortnox.reactivewizard.db;

import rx.Observable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface DbProxyTestDao {
    @Update("update table set val=:val where key=:key")
    Observable<Integer> update(String key, String val);

    @Update(value = "update table set val=:val where key=:key", minimumAffected = 0)
    Observable<Integer> updateAllowingZeroAffectedRows(String key, String val);

    @Query("select * from table where key=:key")
    Observable<DbTestObj> select(String key);

    @Update("insert into table")
    Observable<GeneratedKey<Long>> insert();

    @Update("insert into table (date) values (:date)")
    Observable<GeneratedKey<Long>> insertWithGeneratedKey(LocalDate date);

    @Update("insert into table (date) values (:date)")
    Observable<Integer> insertLocalDate(LocalDate date);

    @Update("insert into table (time) values (:time)")
    Observable<Integer> insertLocalTime(LocalTime time);

    @Update("insert into table (datetime) values (:datetime)")
    Observable<Integer> insertLocalDateTime(LocalDateTime datetime);

    @Update("insert into table")
    Observable<String> insertBadreturnType();

    @Update("insert into table")
    Observable<Void> insertVoidReturnType();

    Observable<String> methodMissingAnnotation();

    @Query("badsql")
    Observable<String> failingSql();

}
