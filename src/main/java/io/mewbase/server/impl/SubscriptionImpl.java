package io.mewbase.server.impl;

import io.mewbase.bson.BsonObject;
import io.mewbase.common.SubDescriptor;
import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tim on 26/09/16.
 */
public class SubscriptionImpl  {

    private final static Logger logger = LoggerFactory.getLogger(SubscriptionImpl.class);

    private final int maxUnackedBytes;
    private final ConnectionImpl connection;
    private final int id;
    private int unackedBytes;

    public SubscriptionImpl(ConnectionImpl connection, int id, SubDescriptor subDescriptor) {

        this.id = id;
        this.connection = connection;
        this.maxUnackedBytes = connection.server().getServerOptions().getSubscriptionMaxUnackedBytes();
    }


    protected void onReceiveFrame(long pos, BsonObject frame) {
        frame = frame.copy();
        frame.put(Protocol.RECEV_SUBID, id);
        frame.put(Protocol.RECEV_POS, pos);
        Buffer buff = connection.writeResponse(Protocol.RECEV_FRAME, frame);
        unackedBytes += buff.length();
        if (unackedBytes > maxUnackedBytes) {
            // TODO - Possibly use Event source or is Superceeded by Subscription
            // readStream.pause();
        }
    }




}
