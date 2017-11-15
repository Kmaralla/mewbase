package io.mewbase.projection.impl;


import io.mewbase.binders.Binder;
import io.mewbase.binders.BinderStore;
import io.mewbase.bson.BsonObject;
import io.mewbase.eventsource.Event;
import io.mewbase.eventsource.EventHandler;
import io.mewbase.eventsource.EventSource;
import io.mewbase.eventsource.Subscription;
import io.mewbase.projection.Projection;
import io.mewbase.projection.ProjectionBuilder;
import io.mewbase.projection.ProjectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;


public class ProjectionManagerImpl implements ProjectionManager {

    private final static Logger log = LoggerFactory.getLogger(ProjectionManager.class);

    private final EventSource source;
    private final BinderStore store;

    final String PROJ_STATE_BINDER_NAME = "mewbase.proj.state";
    final String EVENT_NUM_FIELD = "eventNum";
    private final Binder stateBinder;


    private final Map<String, ProjectionImpl> projections = new ConcurrentHashMap<>();

    public ProjectionManagerImpl(EventSource source, BinderStore store) throws Exception {
        this.source = source;
        this.store = store;
        this.stateBinder = store.open(PROJ_STATE_BINDER_NAME);
    }

    @Override
    public ProjectionBuilder builder() {
       return new ProjectionBuilderImpl(this);
    }

    /**
     * Instance a projection and register it with the factory
     * @param projectionName
     * @param channelName
     * @param binderName
     * @param eventFilter
     * @param docIDSelector
     * @param projectionFunction
     * @return
     */
    Projection createProjection(final String projectionName,
                                final String channelName,
                                final String binderName,
                                final Function<Event, Boolean> eventFilter,
                                final Function<Event, String> docIDSelector,
                                final BiFunction<BsonObject, Event, BsonObject> projectionFunction) {


        EventHandler eventHandler =  event -> {
            if (eventFilter.apply(event)) {

                String docID = docIDSelector.apply(event);
                if (docID == null) {
                    log.error("In projection " + projectionName + " document id selector returned null");
                } else {

                    Binder binder = store.open(binderName);

                    binder.get(docID).whenComplete((inputDoc, innerExp) -> {
                        // Case 1 - Something broke in the store/binder
                        if (innerExp != null) {
                            log.error("Error in binder " + binderName + " while finding " + docID, innerExp);
                        }
                        // case 2 - Nothing broke but the document doesnt exists
                        if (inputDoc == null && innerExp == null) {
                            inputDoc = new BsonObject();
                        }
                        // case 3 - We now have the doc
                        if (inputDoc != null && innerExp == null) {
                            BsonObject outputDoc = projectionFunction.apply(inputDoc, event);
                            binder.put(docID, outputDoc);
                            // write the number of this projections most recent event into the store.
                            BsonObject projStateDoc = new BsonObject().put(EVENT_NUM_FIELD, event.getEventNumber());
                            stateBinder.put(projectionName, projStateDoc);
                        }
                    });
                }
            }
        };

        Subscription subs = subscribeFromLastKnownEvent(projectionName,channelName,eventHandler);

        // register it with the Factory
        ProjectionImpl proj = new ProjectionImpl(projectionName,subs);
        projections.put(projectionName,proj);
        return proj;
    }


    @Override
    public boolean isProjection(String projectionName) {
        return projections.keySet().contains(projectionName);
    }

    @Override
    public Stream<String> projectionNames() {
        return projections.keySet().stream() ;
    }

    @Override
    public void stopAll() { projections.forEach( (name, proj) -> proj.stop() ); }


    private Subscription subscribeFromLastKnownEvent(String projectionName, String channelName, EventHandler eventHandler) {
        try {
            final BsonObject stateDoc = stateBinder.get(projectionName).get();
            if (stateDoc == null) {
                log.info("Projection " + projectionName + " subscribing from start of channel " + channelName);
                return source.subscribeAll(channelName, eventHandler);
            } else {
                Long nextEvent = stateDoc.getLong(EVENT_NUM_FIELD) + 1;
                log.info("Projection " + projectionName + " subscribing from event number " + nextEvent);
                return source.subscribeFromEventNumber(channelName, nextEvent, eventHandler);
            }
        } catch (Exception exp) {
            log.error("Failed to recover last known state of the projection " + projectionName, exp);
        }
        return null;
    }

}
