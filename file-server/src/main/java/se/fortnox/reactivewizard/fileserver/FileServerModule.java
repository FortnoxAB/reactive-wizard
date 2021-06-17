package se.fortnox.reactivewizard.fileserver;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import se.fortnox.reactivewizard.binding.AutoBindModule;

public class FileServerModule implements AutoBindModule {

    @Override
    public void configure(Binder binder) {
        TypeLiteral<RequestHandler<ByteBuf, ByteBuf>> type = new TypeLiteral<RequestHandler<ByteBuf, ByteBuf>>() {
        };
        Multibinder<RequestHandler<ByteBuf, ByteBuf>> requestHandlerMultibinder = Multibinder.newSetBinder(binder, type);
        requestHandlerMultibinder.addBinding().to(FileServerRequestHandler.class);
    }
}
