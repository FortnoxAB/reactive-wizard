package se.fortnox.reactivewizard.json;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Jdk8Types {
    private final LocalDate        localDate;
    private final LocalTime        localTime;
    private final LocalDateTime    localDateTime;
    private final Optional<String> optionalString;
    private final OptionalInt      optionalInt;

    public Jdk8Types(LocalDate localDate, LocalTime localTime, LocalDateTime localDateTime,
        Optional<String> optionalString, OptionalInt optionalInt
    ) {
        this.localDate = localDate;
        this.localTime = localTime;
        this.localDateTime = localDateTime;
        this.optionalString = optionalString;
        this.optionalInt = optionalInt;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public Optional<String> getOptionalString() {
        return optionalString;
    }

    public OptionalInt getOptionalInt() {
        return optionalInt;
    }

    @Override
    public String toString() {
        return String.format(
            "Jdk8Types{localDate=%s, localTime=%s, localDateTime=%s, optionalString=%s, optionalInt=%s}",
            localDate,
            localTime,
            localDateTime,
            optionalString,
            optionalInt);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Jdk8Types that = (Jdk8Types)object;
        return Objects.equals(localDate, that.localDate) && Objects.equals(localTime, that.localTime) && Objects.equals(
            localDateTime,
            that.localDateTime) && Objects.equals(optionalString, that.optionalString) && Objects.equals(optionalInt,
            that.optionalInt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localDate, localTime, localDateTime, optionalString, optionalInt);
    }
}
