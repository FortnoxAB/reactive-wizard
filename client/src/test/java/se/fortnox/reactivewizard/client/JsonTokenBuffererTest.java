package se.fortnox.reactivewizard.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.Test;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonTokenBuffererTest {
    @Test
    public void shallBeAbleToBufferWholeObjectsFromAChunkedStream() {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectReader reader       = objectMapper.reader();

        Flux<byte[]> stream = createChunkedJson();

        List<AnObject> result = JsonTokenBufferer.buffer(stream, objectMapper)
            .map(tokenBuffer -> {
                try {
                    return reader.readValue(tokenBuffer.asParser(), AnObject.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collectList().block();

        assertThat(result.size()).isEqualTo(1000);
        assertThat(result.get(0).getAn().getObject().get(2)).isEqualTo(3);
        assertThat(result.get(999).getAn().getObject().get(2)).isEqualTo(3);
    }

    private Flux<byte[]> createChunkedJson() {
        String json = "{\"an\":{\"object\":[1,2,3]}}";

        byte[] longJson = IntStream.range(0, 1000)
            .mapToObj(i -> json).collect(Collectors.joining())
            .getBytes(StandardCharsets.UTF_8);

        byte[][] partitioned = partition(longJson, 29);

        return Flux.fromArray(partitioned);
    }

    private static byte[][] partition(byte[] input, int partitionSize) {
        int partitionCount = (int)Math.ceil((double)input.length / (double)partitionSize);

        byte[][] result = new byte[partitionCount][];

        for (int i = 0; i < partitionCount; i++) {
            boolean isLast    = (i == partitionCount - 1);
            int     start     = i * partitionSize;
            int     len       = !isLast ? partitionSize : input.length - start;
            byte[]  partition = new byte[len];

            System.arraycopy(input, start, partition, 0, len);

            result[i] = partition;
        }

        return result;
    }

    private static class AnObject {
        private Inner an;

        public Inner getAn() {
            return an;
        }

        public void setAn(Inner an) {
            this.an = an;
        }

        private static class Inner {
            private List<Integer> object;

            public List<Integer> getObject() {
                return object;
            }

            public void setObject(List<Integer> object) {
                this.object = object;
            }
        }
    }
}
