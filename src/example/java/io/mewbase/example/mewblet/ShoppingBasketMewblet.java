package io.mewbase.example.mewblet;

import io.mewbase.bson.BsonPath;
import io.mewbase.server.Mewbase;
import io.mewbase.server.Mewblet;

/**
 * Created by tim on 16/12/16.
 */
public class ShoppingBasketMewblet implements Mewblet {

    @Override
    public void setup(Mewbase mewbase) throws Exception {

        mewbase.createBinder("baskets").get();

        mewbase.buildProjection("maintain_basket")                                // projection name
                .projecting("orders")                                           // channel name
                .filteredBy(ev -> ev.getString("eventType").equals("add_item")) // event filter
                .onto("baskets")                                                // binder name
                .identifiedBy(ev -> ev.getString("basketID"))                   // document id selector; how to obtain the doc id from the event bson
                .as((basket, del) ->                                            // projection function
                        BsonPath.add(basket, del.event().getInteger("quantity"), "products", del.event().getString("productID")))
                .create();

    }
}
