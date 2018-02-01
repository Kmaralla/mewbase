package io.mewbase.eventsource.impl.nats;


import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.mewbase.bson.BsonObject;
import io.mewbase.eventsource.EventSink;

import io.nats.stan.Connection;
import io.nats.stan.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


/**
 * These tests assume that there is an instance of Nats Streaming Server running on localhost:4222
 */

public class NatsEventSink implements EventSink {

    private final static Logger logger = LoggerFactory.getLogger(NatsEventSink.class);

    private final Connection nats;

    public NatsEventSink() {
        this( ConfigFactory.load() );
    }


    public NatsEventSink(Config cfg) {
        final String userName = cfg.getString("mewbase.event.sink.nats.username");
        final String clusterName = cfg.getString("mewbase.event.sink.nats.clustername");
        final String url = cfg.getString("mewbase.event.sink.nats.url");

        final ConnectionFactory cf = new ConnectionFactory(clusterName,userName);
        cf.setNatsUrl(url);

        try {
            String clientUUID = UUID.randomUUID().toString();
            cf.setClientId(clientUUID);
            nats = cf.createConnection();
            logger.info("Created Nats EventSink connection with client UUID " + clientUUID);
        } catch (Exception exp) {
            logger.error("Error connecting to Nats Streaming Server", exp);
            throw new RuntimeException(exp);
        }
    }


    @Override
    public void publishSync(String channelName, BsonObject event) {
        try {
            nats.publish(channelName, event.encode().getBytes());
        } catch (Exception exp) {
            logger.error("Error attempting publishSync event to Nats Event Sink", exp);
        }
    }

    @Override
    public CompletableFuture<BsonObject> publishAsync(final String channelName, final BsonObject event) {
        CompletableFuture<BsonObject> fut = new CompletableFuture<>();
        try {
            nats.publish(channelName, event.encode().getBytes(), (String ackedNuid, Exception err) -> {
                if (err != null) {
                    fut.completeExceptionally(err);
                } else {
                    fut.complete(event);
                }
            });
        } catch (IOException exp) {
            fut.completeExceptionally(exp);
        }
        return fut;
    }


    @Override
    public void close() {
        try {
            nats.close();
        } catch (Exception exp) {
            logger.error("Error attempting close Nats Streaming Server Event Sink", exp);
        }
    }

}
