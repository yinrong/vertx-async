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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import java.util.*;
import java.util.function.BiConsumer;

public final class CollectionsAsync {

    private CollectionsAsync() {
    }

    public static <T> void each(final Vertx instance, final Collection<T> iterable, final BiConsumer<T, Handler<AsyncResult<Void>>> consumer, final Handler<AsyncResult<Void>> handler) {
        if (iterable.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed());
        } else {
            final ObjectWrapper<Boolean> failed = new ObjectWrapper<>(false);
            final ObjectWrapper<Integer> counter = new ObjectWrapper<>(iterable.size());
            for (T item : iterable) {
                instance.runOnContext(aVoid -> consumer.accept(item, result -> {
                    counter.setObject(counter.getObject() - 1);
                    if (result.failed()) {
                        if (!failed.getObject()) {
                            handler.handle(DefaultAsyncResult.fail(result));
                            failed.setObject(true);
                        }
                    } else if (counter.getObject() == 0 && !failed.getObject()) {
                        handler.handle(DefaultAsyncResult.succeed());
                    }
                }));

                if (failed.getObject()) {
                    break;
                }
            }
        }
    }

    public static <I, O> void map(final Vertx instance, final Collection<I> iterable, final BiConsumer<I, Handler<AsyncResult<O>>> consumer, final Handler<AsyncResult<Collection<O>>> handler) {
        final List<O> mapped = new LinkedList<>();
        if (iterable.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed(mapped));
        } else {
            final ObjectWrapper<Boolean> failed = new ObjectWrapper<>(false);
            final ObjectWrapper<Integer> counter = new ObjectWrapper<>(iterable.size());

            for (I item : iterable) {
                instance.runOnContext(aVoid -> consumer.accept(item, result -> {
                    counter.setObject(counter.getObject() - 1);
                    if (result.failed()) {
                        if (!failed.getObject()) {
                            handler.handle(DefaultAsyncResult.fail(result));
                            failed.setObject(true);
                        }
                    } else {
                        mapped.add(result.result());
                        if (counter.getObject() == 0 && !failed.getObject()) {
                            handler.handle(DefaultAsyncResult.succeed(mapped));
                        }
                    }
                }));

                if (failed.getObject()) {
                    break;
                }
            }
        }
    }
    
    public static <T> void filter(final Vertx instance, final Collection<T> iterable, final BiConsumer<T, Handler<AsyncResult<Boolean>>> consumer, final Handler<AsyncResult<Collection<T>>> handler) {
        final List<T> filtered = new LinkedList<>();
        if (iterable.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed(filtered));
        } else {
            final ObjectWrapper<Boolean> failed = new ObjectWrapper<>(false);
            final ObjectWrapper<Integer> counter = new ObjectWrapper<>(iterable.size());
            for (T item : iterable) {
                instance.runOnContext(aVoid -> consumer.accept(item, result -> {
                    counter.setObject(counter.getObject() - 1);
                    if (result.failed()) {
                        if (!failed.getObject()) {
                            handler.handle(DefaultAsyncResult.fail(result));
                            failed.setObject(true);
                        }
                    } else {
                        if(result.result()) {
                            filtered.add(item);
                        }
                        if (counter.getObject() == 0 && !failed.getObject()) {
                            handler.handle(DefaultAsyncResult.succeed(filtered));
                        }
                    }
                }));

                if (failed.getObject()) {
                    break;
                }
            }
        }
    }
    
    public static <T> void reject(final Vertx instance, final Collection<T> iterable, final BiConsumer<T, Handler<AsyncResult<Boolean>>> consumer, final Handler<AsyncResult<Collection<T>>> handler) {
        filter(instance, iterable, (T t, Handler<AsyncResult<Boolean>> u) -> {
            consumer.accept(t, (Handler<AsyncResult<Boolean>>) (AsyncResult<Boolean> event) -> {
                if(event.succeeded()) {
                    u.handle(DefaultAsyncResult.succeed(!event.result()));
                } else {
                    u.handle(event);
                }
            });
        }, handler);
    }
}
