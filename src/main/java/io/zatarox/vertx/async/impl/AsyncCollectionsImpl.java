/*
 * Copyright 2004-2016 Guillaume Chauvet.
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
package io.zatarox.vertx.async.impl;

import io.zatarox.vertx.async.utils.DefaultAsyncResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.zatarox.vertx.async.api.AsyncCollections;
import io.zatarox.vertx.async.api.BiHandler;
import io.zatarox.vertx.async.api.Pair;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public final class AsyncCollectionsImpl implements AsyncCollections {
    
    private final Context context;

    public AsyncCollectionsImpl(final Context context) {
        this.context = context;
    }

    /**
     * Applies the function {@code consumer} to each item in {@code iterable},
     * in parallel. The {@code consumer} is called with an item from the list,
     * and a callback when it has finished. If the {@code consumer} passes an
     * error to its {@code callback}, the main {@code handler} (for the
     * {@code each} function) is immediately called with the error.
     *
     * Note, that since this function applies {@code consumer} to each item in
     * parallel, there is no guarantee that the consumer functions will complete
     * in order.
     *
     * @param <T> Define the manipulated type.
     * @param iterable A collection to iterate over.
     * @param consumer A function to apply to each item in {@code iterable}. The
     * iteratee is passed a {@code consumer} which must be called once it has
     * completed. If no error has occurred, the {@code callback} should be run
     * without arguments or with an explicit {@code null} argument. The array
     * index is not passed to the consumer. If you need the index, use
     * {@code eachOf}.
     * @param handler A callback which is called when all {@code consumer}
     * functions have finished, or an error occurs.
     */
    @Override
    public <T> void each(final Collection<T> iterable, final BiHandler<T, Handler<AsyncResult<Void>>> consumer, final Handler<AsyncResult<Void>> handler) {
        if (iterable.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed());
        } else {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger counter = new AtomicInteger(iterable.size());
            iterable.stream().forEach(item -> {
                if (!stop.get()) {
                    context.runOnContext(aVoid -> {
                        try {
                            consumer.handle(item, result -> {
                                if (result.failed() || stop.get()) {
                                    if (!stop.get()) {
                                        stop.set(true);
                                        handler.handle(DefaultAsyncResult.fail(result));
                                    }
                                } else if (counter.decrementAndGet() == 0 && !stop.get()) {
                                    handler.handle(DefaultAsyncResult.succeed());
                                }
                            });
                        } catch (Throwable ex) {
                            if (!stop.get()) {
                                stop.set(true);
                                handler.handle(DefaultAsyncResult.fail(ex));
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * Like {@code each}, except that it passes the tuple key/value as argument
     * to the consumer.
     *
     * @param <K> Define type of key.
     * @param <V> Define type of value.
     * @param iterable A collection to iterate over.
     * @param consumer A function to apply to each item in {@code iterable}. The
     * {@code key} is the item's key. The iteratee is passed a {@code handler}
     * which must be called once it has completed. If no error has occurred, the
     * callback should be run without arguments or with an explicit {@code null}
     * argument.
     * @param handler A callback which is called when all {@code consumer}
     * functions have finished, or an error occurs.
     */
    @Override
    public <K, V> void each(final Map<K, V> iterable, final BiHandler<Pair<K, V>, Handler<AsyncResult<Void>>> consumer, final Handler<AsyncResult<Void>> handler) {
        if (iterable.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed());
        } else {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger counter = new AtomicInteger(iterable.size());
            iterable.entrySet().stream().forEach(item -> {
                context.runOnContext(aVoid -> {
                    try {
                        consumer.handle(new PairImpl<>(item.getKey(), item.getValue()), result -> {
                            if (result.failed() || stop.get()) {
                                if (!stop.get()) {
                                    stop.set(true);
                                    handler.handle(DefaultAsyncResult.fail(result));
                                }
                            } else if (counter.decrementAndGet() == 0 && !stop.get()) {
                                handler.handle(DefaultAsyncResult.succeed());
                            }
                        });
                    } catch (Throwable ex) {
                        handler.handle(DefaultAsyncResult.fail(ex));
                    }

                });
            });
        }
    }

    /**
     * Produces a new collection of values by mapping each value in
     * {@code iterable} through the {@code consumer} function. The
     * {@code consumer} is called with an item from {@code iterbale} and a
     * callback for when it has finished processing. Each of these callback
     * takes 2 arguments: an {@code error}, and the transformed item from
     * {@code iterable}. If {@code consumer} passes an error to its callback,
     * the main {@code handler} (for the {@code map} function) is immediately
     * called with the error.
     *
     * Note, that since this function applies the {@code consumer} to each item
     * in parallel, there is no guarantee that the {@code consumer} functions
     * will complete in order. However, the results array will be in the same
     * order as the original {@code iterable}.
     *
     * @param <I> Define input type.
     * @param <O> Define output type.
     * @param iterable A collection to iterate over.
     * @param consumer A function to apply to each item in {@code iterable}. The
     * iteratee is passed a {@code handler} which must be called once it has
     * completed with an error and a transformed item. Invoked with (item,
     * callback).
     * @param handler A callback which is called when all {@code consumer}
     * functions have finished, or an error occurs. Results is a List of the
     * transformed items from the {@code iterable}.
     */
    @Override
    public <I, O> void map(final List<I> iterable, final BiHandler<I, Handler<AsyncResult<O>>> consumer, final Handler<AsyncResult<Collection<O>>> handler) {
        final List<O> mapped = new ArrayList<>(iterable.size());
        if (iterable.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed(mapped));
        } else {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger counter = new AtomicInteger(iterable.size());

            for (int i = 0; i < iterable.size(); i++) {
                final I item = iterable.get(i);
                final int pos = i;
                context.runOnContext(aVoid -> {
                    try {
                        consumer.handle(item, result -> {
                            if (result.failed() || stop.get()) {
                                if (!stop.get()) {
                                    stop.set(true);
                                    handler.handle(DefaultAsyncResult.fail(result));
                                }
                            } else {
                                mapped.add(pos, result.result());
                                if (counter.decrementAndGet() == 0 && !stop.get()) {
                                    handler.handle(DefaultAsyncResult.succeed(mapped));
                                }
                            }
                        });
                    } catch (Throwable ex) {
                        if (!stop.get()) {
                            stop.set(true);
                            handler.handle(DefaultAsyncResult.fail(ex));
                        }
                    }
                });
            }
        }
    }

    /**
     * Returns a new collection of all the values in {@code iterable} which pass
     * an async truth test. This operation is performed in parallel, but the
     * results array will be in the same order as the original.
     *
     * @param <T> Define the manipulated type.
     * @param iterable A collection to iterate over.
     * @param consumer A truth test to apply to each item in {@code iterable}.
     * The {@code consumer} is passed a {@code handler}, which must be called
     * with a boolean argument once it has completed.
     * @param handler A callback which is called after all the {@code consumer}
     * functions have finished.
     */
    @Override
    public <T> void filter(final Collection<T> iterable, final BiHandler<T, Handler<AsyncResult<Boolean>>> consumer, final Handler<AsyncResult<Collection<T>>> handler) {
        final List<T> filtered = new LinkedList<>();
        if (iterable.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed(filtered));
        } else {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger counter = new AtomicInteger(iterable.size());

            iterable.stream().forEach((item) -> {
                context.runOnContext(aVoid -> {
                    try {
                        consumer.handle(item, result -> {
                            if (result.failed() || stop.get()) {
                                if (!stop.get()) {
                                    stop.set(true);
                                    handler.handle(DefaultAsyncResult.fail(result));
                                }
                            } else {
                                if (result.result()) {
                                    filtered.add(item);
                                }
                                if (counter.decrementAndGet() == 0 && !stop.get()) {
                                    handler.handle(DefaultAsyncResult.succeed(filtered));
                                }
                            }
                        });
                    } catch (Throwable ex) {
                        if (!stop.get()) {
                            stop.set(true);
                            handler.handle(DefaultAsyncResult.fail(ex));
                        }
                    }
                });
            });
        }
    }

    /**
     * The opposite of {@code filter}. Removes values that pass an {@code async}
     * truth test.
     *
     * @param <T> Define the manipulated type.
     * @param iterable A collection to iterate over.
     * @param consumer A falsy test to apply to each item in {@code iterable}.
     * The {@code consumer} is passed a {@code handler}, which must be called
     * with a boolean argument once it has completed.
     * @param handler A callback which is called after all the {@code consumer}
     * functions have finished.
     */
    @Override
    public <T> void reject(final Collection<T> iterable, final BiHandler<T, Handler<AsyncResult<Boolean>>> consumer, final Handler<AsyncResult<Collection<T>>> handler) {
        filter(iterable, (t, u) -> {
            consumer.handle(t, event -> {
                if (event.succeeded()) {
                    u.handle(DefaultAsyncResult.succeed(!event.result()));
                } else {
                    u.handle(event);
                }
            });
        }, handler);
    }

    /**
     * A relative of {@code reduce}. Takes a Collection, and iterates over each
     * element in series, each step potentially mutating an {@code accumulator}
     * value. The type of the accumulator defaults to the type of collection
     * passed in.
     *
     * @param <I> Define the type of input data
     * @param <O> Define the type of output data
     * @param iterable A collection to iterate over.
     * @param consumer A function applied to each item in the collection that
     * potentially modifies the accumulator. The {@code consumer} is passed a
     * {@code handler}. If an error is passed to the callback, the transform is
     * stopped and the main {@code handler} is immediately called with the
     * error.
     * @param handler A callback which is called after all the {@code consumer}
     * functions have finished. Result is the transformed accumulator.
     */
    @Override
    public <I, O> void transform(final Collection<I> iterable, final BiHandler<I, Handler<AsyncResult<O>>> consumer, final Handler<AsyncResult<Collection<O>>> handler) {
        context.runOnContext(new Handler<Void>() {
            final Iterator<I> iterator = iterable.iterator();
            final List<O> result = new ArrayList<>(iterable.size());

            @Override
            public void handle(Void event) {
                if (!iterator.hasNext()) {
                    handler.handle(DefaultAsyncResult.succeed(result));
                } else {
                    try {
                        consumer.handle(iterator.next(), event1 -> {
                            if (event1.succeeded()) {
                                result.add(event1.result());
                                context.runOnContext(this);
                            } else {
                                handler.handle(DefaultAsyncResult.fail(event1));
                            }
                        });
                    } catch (Throwable ex) {
                        handler.handle(DefaultAsyncResult.fail(ex));
                    }
                }
            }
        });
    }

    /**
     * A relative of {@code reduce}. Takes a Map, and iterates over each element
     * in series, each step potentially mutating an {@code accumulator} value.
     * The type of the accumulator defaults to the type of collection passed in.
     *
     * @param <K> Define the type of input key.
     * @param <V> Define the type of input value.
     * @param <T> Define the type of output key.
     * @param <R> Define the type of output value.
     * @param iterable A collection to iterate over.
     * @param consumer A function applied to each item in the collection that
     * potentially modifies the accumulator. The {@code consumer} is passed a
     * {@code handler}. If an error is passed to the callback, the transform is
     * stopped and the main {@code handler} is immediately called with the
     * error.
     * @param handler A callback which is called after all the {@code consumer}
     * functions have finished. Result is the transformed accumulator.
     */
    @Override
    public <K, V, T, R> void transform(final Map<K, V> iterable, final BiHandler<Pair<K, V>, Handler<AsyncResult<Pair<T, R>>>> consumer, final Handler<AsyncResult<Map<T, R>>> handler) {
        context.runOnContext(new Handler<Void>() {
            final Iterator<Map.Entry<K, V>> iterator = iterable.entrySet().iterator();
            final Map<T, R> results = new HashMap<>(iterable.size());

            @Override
            public void handle(Void event) {
                if (!iterator.hasNext()) {
                    handler.handle(DefaultAsyncResult.succeed(results));
                } else {
                    final Map.Entry<K, V> item = iterator.next();
                    try {
                        consumer.handle(new PairImpl<>(item.getKey(), item.getValue()), event1 -> {
                            if (event1.succeeded()) {
                                results.put(event1.result().getKey(), event1.result().getValue());
                                context.runOnContext(this);
                            } else {
                                handler.handle(DefaultAsyncResult.fail(event1));
                            }
                        });
                    } catch (Throwable ex) {
                        handler.handle(DefaultAsyncResult.fail(ex));
                    }
                }
            }
        });
    }

    /**
     * Reduces {@code collection} into a single value using an async
     * {@code consumer} to return each successive step. {@code memo} is the
     * initial state of the reduction. This function only operates in series.
     *
     * This function is for situations where each step in the reduction needs to
     * be async; if you can get the data before reducing it, then it's probably
     * a good idea to do so.
     *
     * @param <I> Define the type of input data
     * @param <O> Define the type of output data
     * @param collection A collection to iterate over.
     * @param memo Initial state of the reduction.
     * @param function A function applied to each item in the array to produce
     * the next step in the reduction. The {@code function} is passed a
     * {@code handler} which accepts an optional error as its first argument,
     * and the state of the reduction as the second. If an error is passed to
     * the callback, the reduction is stopped and the main {@code handler} is
     * immediately called.
     * @param handler A callback which is called after all the {@code function}
     * functions have finished. Result is the transformed accumulator.
     */
    @Override
    public <I, O> void reduce(final Collection<I> collection, final O memo, final BiHandler<Pair<I, O>, Handler<AsyncResult<O>>> function, final Handler<AsyncResult<O>> handler) {
        context.runOnContext(new Handler<Void>() {
            final Iterator<I> iterator = collection.iterator();
            final AtomicReference<O> value = new AtomicReference<>(memo);

            @Override
            public void handle(Void event) {
                if (!iterator.hasNext()) {
                    handler.handle(DefaultAsyncResult.succeed(value.get()));
                } else {
                    try {
                        function.handle(new PairImpl<>(iterator.next(), value.get()), event1 -> {
                            if (event1.failed()) {
                                handler.handle(DefaultAsyncResult.fail(event1));
                            } else {
                                value.set(event1.result());
                                context.runOnContext(this);
                            }
                        });
                    } catch (Throwable ex) {
                        handler.handle(DefaultAsyncResult.fail(ex));
                    }
                }
            }
        });
    }

    /**
     * Returns the first value in {@code collection} that passes an async truth
     * test. The {@code function} is applied in parallel, meaning the first
     * iteratee to return {@code true} will fire the detect {@code callback}
     * with that result. That means the result might not be the first item in
     * the original {@code collection} (in terms of order) that passes the test.
     *
     * @param <T> Define the manipulated type.
     * @param collection A collection to iterate over.
     * @param function A truth test to apply to each item in {@code collection}.
     * The iteratee is passed a {@code callback} which must be called with a
     * boolean argument once it has completed.
     * @param handler A callback which is called as soon as any iteratee returns
     * {@code true}, or after all the {@code function} functions have finished.
     * Result will be the first item in the array that passes the truth test
     * (function) or the value {@code null} if none passed.
     */
    @Override
    public <T> void detect(final Collection<T> collection, final BiHandler<T, Handler<AsyncResult<Boolean>>> function, final Handler<AsyncResult<T>> handler) {
        if (collection.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed(null));
        } else {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger counter = new AtomicInteger(collection.size());
            collection.stream().forEach(item -> {
                context.runOnContext(aVoid -> {
                    try {
                        function.handle(item, event -> {
                            if (event.succeeded()) {
                                if (event.result() || stop.get()) {
                                    if (!stop.get()) {
                                        stop.set(true);
                                        handler.handle(DefaultAsyncResult.succeed(item));
                                    }
                                } else if (counter.decrementAndGet() == 0 && !stop.get()) {
                                    handler.handle(DefaultAsyncResult.succeed(null));
                                }
                            } else if (!stop.get()) {
                                stop.set(true);
                                handler.handle(DefaultAsyncResult.fail(event));
                            }
                        });
                    } catch (Throwable ex) {
                        if (!stop.get()) {
                            stop.set(true);
                            handler.handle(DefaultAsyncResult.fail(ex));
                        }
                    }
                });
            });
        }
    }

    /**
     * Returns {@code true} if at least one element in the {@code collection}
     * satisfies an async test. If any iteratee call returns {@code true}, the
     * main {@code callback} is immediately called.
     *
     * @param <T> Define the manipulated type.
     * @param collection A collection to iterate over.
     * @param function A truth test to apply to each item in the array in
     * parallel.
     * @param handler A callback which is called as soon as any iteratee returns
     * {@code true}, or after all the iteratee functions have finished. Result
     * will be either {@code true} or {@code false} depending on the values of
     * the async tests.
     */
    @Override
    public <T> void some(final Collection<T> collection, final BiHandler<T, Handler<AsyncResult<Boolean>>> function, final Handler<AsyncResult<Boolean>> handler) {
        if (collection.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed(false));
        } else {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger counter = new AtomicInteger(collection.size());

            collection.stream().forEach(item -> {
                context.runOnContext(aVoid -> {
                    try {
                        function.handle(item, event -> {
                            if (event.succeeded()) {
                                // Prevent Unhandled exception in Netty
                                if (null != event.result() && event.result() || stop.get()) {
                                    if (!stop.get()) {
                                        stop.set(true);
                                        handler.handle(DefaultAsyncResult.succeed(true));
                                    }
                                } else if (counter.decrementAndGet() == 0 && !stop.get()) {
                                    handler.handle(DefaultAsyncResult.succeed(false));
                                }
                            } else if (!stop.get()) {
                                stop.set(true);
                                handler.handle(DefaultAsyncResult.fail(event));
                            }
                        });
                    } catch (Throwable ex) {
                        if (!stop.get()) {
                            stop.set(true);
                            handler.handle(DefaultAsyncResult.fail(ex));
                        }
                    }
                });
            });
        }
    }

    /**
     * Returns {@code true} if every element in {@code collection} satisfies an
     * async test. If any iteratee call returns {@code false}, the main
     * {@code callback} is immediately called.
     *
     * @param <T> Define the manipulated type.
     * @param collection A collection to iterate over.
     * @param function A truth test to apply to each item in the collection in
     * parallel.
     * @param handler A callback which is called after all the {code collection}
     * functions have finished. Result will be either {@code true} or
     * {@code false} depending on the values of the async tests.
     */
    @Override
    public <T> void every(final Collection<T> collection, final BiHandler<T, Handler<AsyncResult<Boolean>>> function, final Handler<AsyncResult<Boolean>> handler) {
        if (collection.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed(false));
        } else {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger counter = new AtomicInteger(collection.size());
            collection.stream().forEach(item -> {
                context.runOnContext(aVoid -> {
                    try {
                        function.handle(item, event -> {
                            if (event.succeeded()) {
                                // Prevent Unhandled exception in Netty
                                if (null != event.result() && !event.result() || stop.get()) {
                                    if (!stop.get()) {
                                        stop.set(true);
                                        handler.handle(DefaultAsyncResult.succeed(false));
                                    }
                                } else if (counter.decrementAndGet() == 0 && !stop.get()) {
                                    handler.handle(DefaultAsyncResult.succeed(true));
                                }
                            } else {
                                stop.set(true);
                                handler.handle(DefaultAsyncResult.fail(event));
                            }
                        });
                    } catch (Throwable ex) {
                        if (!stop.get()) {
                            stop.set(true);
                            handler.handle(DefaultAsyncResult.fail(ex));
                        }
                    }
                });
            });
        }
    }

    /**
     * Applies {@code consumer} to each item in {@code collection},
     * concatenating the results. Returns the concatenated list. The
     * {@code iteratee}s are called in parallel, and the results are
     * concatenated as they return. There is no guarantee that the results array
     * will be returned in the original order of {@code collection} passed to
     * the {@code iteratee} function.
     *
     * @param <I> Define input type.
     * @param <O> Define output type.
     * @param iterable A collection to iterate over.
     * @param consumer A function to apply to each item in collection
     * @param handler A callback which is called after all the {@code iterable}
     * functions have finished, or an error occurs.
     */
    @Override
    public <I, O> void concat(final Collection<I> iterable, final BiHandler<I, Handler<AsyncResult<Collection<O>>>> consumer, final Handler<AsyncResult<Collection<O>>> handler) {
        final List<O> results = new ArrayList<>(iterable.size());
        if (iterable.isEmpty()) {
            handler.handle(DefaultAsyncResult.succeed(results));
        } else {
            final AtomicBoolean stop = new AtomicBoolean(false);
            final AtomicInteger counter = new AtomicInteger(iterable.size());

            iterable.stream().forEach(item -> {
                context.runOnContext(aVoid -> {
                    try {
                        consumer.handle(item, result -> {
                            if (result.failed() || stop.get()) {
                                if (!stop.get()) {
                                    stop.set(true);
                                    handler.handle(DefaultAsyncResult.fail(result));
                                }
                            } else {
                                if (result.result() != null) {
                                    results.addAll(result.result());
                                }
                                if (counter.decrementAndGet() == 0 && !stop.get()) {
                                    handler.handle(DefaultAsyncResult.succeed(results));
                                }
                            }
                        });
                    } catch (Throwable ex) {
                        if (!stop.get()) {
                            stop.set(true);
                            handler.handle(DefaultAsyncResult.fail(ex));
                        }
                    }
                });
            });
        }
    }

    /**
     * Sorts a list by the results of running each {@code collection} value
     * through the internal comparator.
     *
     * @param <T> Define the manipulated type.
     * @param iterable A collection to iterate over.
     * @param handler A callback which is called after all the {@code iterable}
     * functions have finished, or an error occurs. Results is the items from
     * the original {@code collection} sorted by the values returned by the
     * {@code iterable} calls.
     */
    @Override
    public <T> void sort(final Collection<T> iterable, final Handler<AsyncResult<Collection<T>>> handler) {
        sort(iterable, null, handler);
    }

    /**
     * Sorts a list by the results of running each {@code collection} value
     * through an async {@code comparator}.
     *
     * @param <T> Define the manipulated type.
     * @param iterable A collection to iterate over.
     * @param comparator A function used as comparator.
     * @param handler A callback which is called after all {@code comparator}
     * functions have finished, or an error occurs. Results is the items from
     * the original {@code collection} sorted by the values returned by the
     * {@code comparator} calls.
     */
    @Override
    public <T> void sort(final Collection<T> iterable, final Comparator<T> comparator, final Handler<AsyncResult<Collection<T>>> handler) {
        context.runOnContext(event -> {
            try {
                Stream<T> stream = iterable.parallelStream();
                if (comparator != null) {
                    stream = stream.sorted(comparator);
                } else {
                    stream = stream.sorted();
                }
                handler.handle(DefaultAsyncResult.succeed(new ArrayList<>(Arrays.asList((T[]) stream.toArray()))));
            } catch (Throwable ex) {
                handler.handle(DefaultAsyncResult.fail(ex));
            }
        });
    }
}
