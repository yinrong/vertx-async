/*
 * Copyright 2016 Guillaume Chauvet.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zatarox.vertx.async.collections;

import io.vertx.core.*;
import io.zatarox.vertx.async.AsyncFactorySingleton;
import io.zatarox.vertx.async.utils.DefaultAsyncResult;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class EachCollection {
    
    public static void main(String[] args) {
        AsyncFactorySingleton.getInstance().createCollections(Vertx.vertx().getOrCreateContext())
        .each(IntStream.iterate(0, i -> i + 1).limit(100).boxed().collect(Collectors.toList()), (item, handler) -> {
            System.out.println("get " + item);
            handler.handle(DefaultAsyncResult.succeed());
        }, e -> {
            System.out.println("done.");
            Vertx.currentContext().owner().close();
        });
    }

}
