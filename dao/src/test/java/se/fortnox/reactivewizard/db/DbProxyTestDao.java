package se.fortnox.reactivewizard.db;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

public interface DbProxyTestDao {
    @Update("update table set val=:val where key=:key")
    Mono<Integer> update(String key, String val);

    @Update(value = "update table set val=:val where key=:key", minimumAffected = 0)
    Mono<Integer> updateAllowingZeroAffectedRows(String key, String val);

    @Query("select sql_val from table where key=:key")
    Flux<String> selectSpecificColumn(String key);

    @Query("select * from table where key=:key")
    Mono<DbTestObj> select(String key);

    @Query("select * from table")
    Mono<DbTestObj> selectMultipleWithMono();

    @Query("select * from table where key=:key")
    Flux<DbTestObj> selectFlux(String key);

    @Update("insert into table")
    Flux<GeneratedKey<Long>> insert();

    @Update("insert into table")
    Mono<GeneratedKey<Long>> insertMono();

    @Update("insert into table (date) values (:date)")
    Mono<GeneratedKey<Long>> insertWithGeneratedKey(LocalDate date);

    @Update("insert into table (date) values (:date)")
    Flux<Integer> insertLocalDate(LocalDate date);

    @Update("insert into table (time) values (:time)")
    Flux<Integer> insertLocalTime(LocalTime time);

    @Update("insert into table (datetime) values (:datetime)")
    Flux<Integer> insertLocalDateTime(LocalDateTime datetime);

    @Update("insert into table (yearMonth) values (:yearMonth)")
    Flux<Integer> insertYearMonth(YearMonth yearMonth);

    @Update("insert into table")
    Mono<String> insertBadreturnType();

    @Update("insert into table")
    Mono<Void> insertVoidReturnType();

    Mono<String> methodMissingAnnotation();

    @Query("badsql")
    Mono<String> failingSql();

    @Query("select sql_val from table")
    Observable<String> selectObservable();
}
