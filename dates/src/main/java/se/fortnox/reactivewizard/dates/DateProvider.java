package se.fortnox.reactivewizard.dates;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;

public interface DateProvider {
    LocalDate getLocalDate();

    LocalDateTime getLocalDateTime();

    OffsetDateTime getOffsetDateTime();

    /**
     * @deprecated Use Java 8 LocalDate and LocalDateTime instead.
     */
    @Deprecated
    Date getDate();
}
