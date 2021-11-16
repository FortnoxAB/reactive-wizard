package se.fortnox.reactivewizard.server;

import org.apache.log4j.Appender;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.TcpServer;
import se.fortnox.reactivewizard.test.LoggingMockUtil;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;
import static se.fortnox.reactivewizard.test.TestUtil.matches;

@RunWith(MockitoJUnitRunner.class)
public class RwServer2Test {



    @Test
    public void shouldSetServerToNullIfConfigSaysDisabled() throws InterruptedException {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setEnabled(false);
        serverConfig.setPort(1337);

       var rwServer = new RwServer3();




    }


}
