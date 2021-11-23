package se.fortnox.reactivewizard.server;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RSocketMessage {
    private String origin;
    private String interaction;
    private long index;
    private long created = Instant.now().getEpochSecond();

    public RSocketMessage(String origin, String interaction) {
        this.origin = origin;
        this.interaction = interaction;
        this.index = 0;
    }

    public RSocketMessage(String origin, String interaction, long index) {
        this.origin = origin;
        this.interaction = interaction;
        this.index = index;
    }
}
