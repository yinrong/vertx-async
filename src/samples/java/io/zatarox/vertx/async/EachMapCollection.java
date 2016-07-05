/*
 * The MIT License
 *
 * Copyright 2016 Guillaume Chauvet.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.zatarox.vertx.async;

import io.vertx.core.*;
import java.util.HashMap;
import java.util.Map;

public class EachMapCollection extends AbstractVerticle {

    @Override
    public void start(final Future<Void> startFuture) {
        final Map<String, Integer> values = new HashMap<>();
        for(int i = 0; i < 50; i++) {
            values.put(Integer.toString(i), i);
        }
        
        AsyncCollections.each(this.getVertx(), values, (item, handler) -> {
            System.out.println(item.getKey() + " -> " + item.getValue());
            handler.handle(DefaultAsyncResult.succeed());
        }, e -> {
            System.out.println("done.");
            startFuture.complete(e.result());
        });
    }

}
