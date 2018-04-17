package se.fortnox.reactivewizard.dates;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;

public class DateProviderImpl implements DateProvider {

    @Inject
    public DateProviderImpl() {
    }

    @Override
    public LocalDate getLocalDate() {
        return LocalDate.now();
    }

    @Override
    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.now();
    }

    @Override
    public Date getDate() {
        return new Date();
    }

    @Override
    public OffsetDateTime getOffsetDateTime() {
        return OffsetDateTime.now();
    }
}
