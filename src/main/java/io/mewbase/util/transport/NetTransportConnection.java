package io.mewbase.util.transport;

import io.mewbase.util.TransportConnection;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

import java.util.function.Consumer;

/**
 * Created by tim on 21/11/16.
 */
public class NetTransportConnection implements TransportConnection {

    private final NetSocket netSocket;

    public NetTransportConnection(NetSocket netSocket) {
        this.netSocket = netSocket;
    }

    @Override
    public void write(Buffer buffer) {
        netSocket.write(buffer);
    }

    @Override
    public void handler(Consumer<Buffer> handler) {
        netSocket.handler(handler::accept);
    }

    @Override
    public void close() {
        netSocket.close();
    }

    @Override
    public void closeHandler(Runnable handler) {
        netSocket.closeHandler(v -> handler.run());
    }
}
