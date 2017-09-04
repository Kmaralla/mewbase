package io.mewbase.log;

import io.mewbase.ServerTestBase;
import io.mewbase.bson.BsonObject;
import io.mewbase.server.ServerOptions;
import io.mewbase.server.impl.ServerImpl;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.TestContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by tim on 14/10/16.
 */
public class LogTestBase extends ServerTestBase {

    protected Log log;
    protected ServerOptions serverOptions;

    // Override setup so we don't start the server
    @Override
    protected void setup0() throws Exception {
        createDirectories();
        serverOptions = super.createServerOptions();
    }

    protected void startLog() throws Exception {
        startServer();
        setupChannelsAndBinders();
        log = ((ServerImpl)server).getLog(TEST_CHANNEL_1);
    }

    @Override
    protected ServerOptions createServerOptions() {
        return serverOptions;
    }

    protected ServerOptions origServerOptions() {
        return super.createServerOptions();
    }

    @Override
    protected void setupChannelsAndBinders() throws Exception {
        server.createChannel(TEST_CHANNEL_1).get();
    }


    protected String getLogFileName(String channel, int i) {
        return channel + "-" + String.format("%012d", i) + ".log";
    }


    protected Buffer readFileIntoBuffer(File f) throws IOException {
        byte[] bytes = Files.readAllBytes(f.toPath());
        return Buffer.buffer(bytes);
    }

    protected File[] listLogFiles(File logDir, String channel) {
        File[] files = logDir.listFiles(file -> {
            String name = file.getName();
            int lpos = name.lastIndexOf("-");
            if (name.endsWith("-log-info.dat")) {
                return false;
            }
            String chName = name.substring(0, lpos);
            return chName.equals(channel);
        });
        return files;
    }

    protected void waitUntil(BooleanSupplier supplier) {
        waitUntil(supplier, 10000);
    }

    protected void waitUntil(BooleanSupplier supplier, long timeout) {
        long start = System.currentTimeMillis();
        while (true) {
            if (supplier.getAsBoolean()) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignore) {
            }
            long now = System.currentTimeMillis();
            if (now - start > timeout) {
                throw new IllegalStateException("Timed out");
            }
        }
    }

    protected void assertExists(int fileNumber) {
        File file = new File(logsDir, getLogFileName(TEST_CHANNEL_1, fileNumber));
        assertTrue("Does not exist " + file, file.exists());
    }

    protected void assertLogChunkLength(int fileNumber, int length) {
        File file = new File(logsDir, getLogFileName(TEST_CHANNEL_1, fileNumber));
        assertTrue(file.exists());
        assertEquals(length, file.length());
    }

    protected void assertLogChunkLengthAsync(TestContext testContext, int fileNumber, long length) {
        File file = new File(logsDir, getLogFileName(TEST_CHANNEL_1, fileNumber));
        testContext.assertEquals(length, file.length());
    }

    protected void assertNumFiles(String channel, int expected) {
        File[] files = listLogFiles(logsDir, channel);
        assertEquals(expected, files.length);
    }

    protected void publishObjectsSequentially(int num, Function<Integer, BsonObject> objectFunction) throws Exception {
        for (int i = 0; i < num; i++) {
            ((ServerImpl)server).publishEvent(log, objectFunction.apply(i)).get();
        }
    }

    protected List<Long> publishObjectsConcurrently(int num, Function<Integer, BsonObject> objectFunction) throws Exception {
        List<CompletableFuture<Long>> cfs = new ArrayList<>();
        List<Long> futuresInCompletedOrder = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            CompletableFuture<Long> eventNum = ((ServerImpl)server).publishEvent(log, objectFunction.apply(i));
            eventNum.whenComplete( (l,e) -> futuresInCompletedOrder.add(l));
            cfs.add(eventNum);
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(cfs.toArray(new CompletableFuture[num]));
        all.get();
        return futuresInCompletedOrder;
    }


}
