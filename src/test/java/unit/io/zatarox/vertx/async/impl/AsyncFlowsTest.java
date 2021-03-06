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
import io.vertx.core.Handler;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Repeat;
import io.vertx.ext.unit.junit.RepeatRule;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.zatarox.vertx.async.api.AsyncFlows;
import io.zatarox.vertx.async.api.BiHandler;
import io.zatarox.vertx.async.fakes.FakeAsyncSupplier;
import io.zatarox.vertx.async.fakes.FakeFailingAsyncFunction;
import io.zatarox.vertx.async.fakes.FakeFailingAsyncSupplier;
import io.zatarox.vertx.async.fakes.FakeSuccessfulAsyncFunction;
import io.zatarox.vertx.async.fakes.FakeSuccessfulAsyncSupplier;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(VertxUnitRunner.class)
public final class AsyncFlowsTest {

    /**
     * Limits
     */
    private static final int TIMEOUT_LIMIT = 1000;
    private static final int REPEAT_LIMIT = 100;

    @Rule
    public RepeatRule repeater = new RepeatRule();
    @Rule
    public RunTestOnContext rule = new RunTestOnContext();
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    private AsyncFlows instance;
    
    @Before
    public void setUp() {
        instance = new AsyncFlowsImpl(rule.vertx().getOrCreateContext());
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seriesStillExecutesWhenThereAreNoTasks(final TestContext context) {
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.series(Arrays.<Handler<Handler<AsyncResult<Void>>>>asList(), result -> {
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertNotNull(result.result());
            context.assertTrue(result.result().isEmpty());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seriesExecutesOneTask(final TestContext context) {
        final FakeSuccessfulAsyncSupplier<String> task1 = new FakeSuccessfulAsyncSupplier<>("Task 1");
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.series(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertNotNull(result.result());
            context.assertTrue(result.result().containsAll(Arrays.asList(task1.result())));
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seriesExecutesTwoTasks(final TestContext context) {
        final FakeSuccessfulAsyncSupplier<String> task1 = new FakeSuccessfulAsyncSupplier<>("Task 1");
        final FakeSuccessfulAsyncSupplier<String> task2 = new FakeSuccessfulAsyncSupplier<>("Task 2");
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.series(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1, task2), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertEquals(1, task2.runCount());
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertNotNull(result.result());
            context.assertTrue(result.result().containsAll(Arrays.asList(task1.result(), task2.result())));
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seriesFailsWhenATaskFails(final TestContext context) {
        final FakeFailingAsyncSupplier<String> task1 = new FakeFailingAsyncSupplier<>(new RuntimeException("Failed"));
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.series(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task1.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seriesExecutesNoMoreTasksWhenATaskFails(final TestContext context) {
        final FakeFailingAsyncSupplier<String> task1 = new FakeFailingAsyncSupplier<>(new RuntimeException("Failed"));
        final FakeSuccessfulAsyncSupplier<String> task2 = new FakeSuccessfulAsyncSupplier<>("Task 2");
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.series(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1, task2), result -> {
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task1.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, (int) task1.runCount());
            context.assertEquals(0, (int) task2.runCount());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void foreverExecutesTheTaskUntilItFails(final TestContext context) {
        final FakeFailingAsyncSupplier<Void> task1 = new FakeFailingAsyncSupplier<>(2, null, new RuntimeException("Failed"));
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.forever(task1, result -> {
            context.assertEquals(3, task1.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            final Object resultValue = result.result();
            context.assertNull(resultValue);
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void foreverExecutesWithRaisedException(final TestContext context) {
        final FakeFailingAsyncSupplier<Void> task1 = new FakeFailingAsyncSupplier<>(2, null, new RuntimeException("Failed"), false);
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.forever(task1, result -> {
            context.assertEquals(3, task1.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            final Object resultValue = result.result();
            context.assertNull(resultValue);
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void waterfallOneTask(final TestContext context) {
        final FakeSuccessfulAsyncFunction<Void, String> task1 = new FakeSuccessfulAsyncFunction<>("Task 1");
        final Async async = context.async();

        instance.waterfall(Arrays.<BiHandler<Void, Handler<AsyncResult<String>>>>asList(task1), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            final String resultValue = (String) result.result();
            context.assertNotNull(resultValue);
            context.assertEquals(task1.result(), resultValue);
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void waterfallTwoTasks(final TestContext context) {
        final FakeSuccessfulAsyncFunction<Void, String> task1 = new FakeSuccessfulAsyncFunction<>("Task 1");
        final FakeSuccessfulAsyncFunction<String, Integer> task2 = new FakeSuccessfulAsyncFunction<>(2);
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.waterfall(Arrays.<BiHandler<Object, Handler<AsyncResult<Object>>>>asList((BiHandler) task1, (BiHandler) task2), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertEquals(task1.result(), task2.consumedValue());
            context.assertEquals(1, task2.runCount());
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            final Integer resultValue = (Integer) result.result();
            context.assertNotNull(resultValue);
            context.assertEquals(task2.result(), resultValue);
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void waterfallFailsWhenATaskFails(final TestContext context) {
        final FakeFailingAsyncFunction<Void, String> task1 = new FakeFailingAsyncFunction<>(new RuntimeException("Failed"));
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.waterfall(Arrays.<BiHandler<Object, Handler<AsyncResult<Object>>>>asList((BiHandler) task1), result -> {
            context.assertEquals(1, (int) task1.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task1.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void waterfallFailsWhenAExceptionTaskRaised(final TestContext context) {
        final Async async = context.async();

        instance.waterfall(Arrays.asList((item, handler) -> {
            throw new ClassCastException();
        }), result -> {
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertTrue(result.cause() instanceof ClassCastException);
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void waterfallNoMoreTasksWhenATaskFails(final TestContext context) {
        final FakeFailingAsyncFunction<Void, String> task1 = new FakeFailingAsyncFunction<>(new RuntimeException("Failed"));
        final FakeSuccessfulAsyncFunction<String, Integer> task2 = new FakeSuccessfulAsyncFunction<>(2);
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.waterfall(Arrays.<BiHandler<Object, Handler<AsyncResult<Object>>>>asList((BiHandler) task1, (BiHandler) task2), result -> {
            context.assertEquals(1, (int) task1.runCount());
            context.assertEquals(0, task2.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task1.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void waterfallFailsWhenAConsumerTaskFails(final TestContext context) {
        final FakeSuccessfulAsyncFunction<Void, String> task1 = new FakeSuccessfulAsyncFunction<>("Task 1");
        final FakeFailingAsyncFunction<String, Integer> task2 = new FakeFailingAsyncFunction<>(new RuntimeException("Failed"));
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.waterfall(Arrays.<BiHandler<Object, Handler<AsyncResult<Object>>>>asList((BiHandler) task1, (BiHandler) task2), result -> {
            context.assertEquals(1, (int) task1.runCount());
            context.assertEquals(task1.result(), task2.consumedValue());
            context.assertEquals(1, task2.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task2.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void waterfallExecutesNoMoreTasksWhenAConsumerTaskFails(final TestContext context) {
        final FakeSuccessfulAsyncFunction<Void, String> task1 = new FakeSuccessfulAsyncFunction<>("Task 1");
        final FakeFailingAsyncFunction<String, Integer> task2 = new FakeFailingAsyncFunction<>(new RuntimeException("Failed"));
        final FakeSuccessfulAsyncFunction<Integer, String> task3 = new FakeSuccessfulAsyncFunction<>("Task 3");
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.waterfall(Arrays.<BiHandler<Object, Handler<AsyncResult<Object>>>>asList((BiHandler) task1, (BiHandler) task2, (BiHandler) task3), result -> {
            context.assertEquals(1, (int) task1.runCount());
            context.assertEquals(task1.result(), task2.consumedValue());
            context.assertEquals(1, task2.runCount());
            context.assertEquals(0, task3.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task2.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void parallelStillExecutesWhenThereAreNoTasks(final TestContext context) {
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.parallel(Arrays.<Handler<Handler<AsyncResult<Void>>>>asList(), result -> {
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertNotNull(result.result());
            context.assertTrue(result.result().isEmpty());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void parallelExecutesOneTask(final TestContext context) {
        final FakeSuccessfulAsyncSupplier<String> task1 = new FakeSuccessfulAsyncSupplier<>("Task 1");
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.parallel(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertNotNull(result.result());
            context.assertTrue(result.result().containsAll(Arrays.asList(task1.result())));
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void parallelExecutesTwoTasks(final TestContext context) {
        final FakeSuccessfulAsyncSupplier<String> task1 = new FakeSuccessfulAsyncSupplier<>("Task 1");
        final FakeSuccessfulAsyncSupplier<String> task2 = new FakeSuccessfulAsyncSupplier<>("Task 2");
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.parallel(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1, task2), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertEquals(1, task2.runCount());
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertNotNull(result.result());
            context.assertTrue(result.result().containsAll(Arrays.asList(task1.result(), task2.result())));
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void parallelFailsWhenATaskFails(final TestContext context) {
        final FakeFailingAsyncSupplier<String> task1 = new FakeFailingAsyncSupplier<>(new RuntimeException("Failed"));
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.parallel(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task1.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void parallelExecutesNoMoreTasksWhenATaskFails(final TestContext context) {
        final FakeFailingAsyncSupplier<String> task1 = new FakeFailingAsyncSupplier<>(new RuntimeException("Failed"));
        final FakeSuccessfulAsyncSupplier<String> task2 = new FakeSuccessfulAsyncSupplier<>("Task 2");
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.parallel(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1, task2), result -> {
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task1.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, (int) task1.runCount());
            context.assertEquals(0, (int) task2.runCount());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void parallelFailsWhenATaskRaisedException(final TestContext context) {
        final FakeFailingAsyncSupplier<String> task1 = new FakeFailingAsyncSupplier<>(new RuntimeException("Failed"), false);
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.parallel(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertEquals(task1.cause(), result.cause());
            context.assertNull(result.result());
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void whilstExecutesEmpty(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(() -> counter.incrementAndGet() < 1, t -> {
            t.handle(DefaultAsyncResult.fail(new IllegalAccessException()));
        }, e -> {
            context.assertTrue(e.succeeded());
            context.assertEquals(1, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void whilstExecutesMany(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(() -> counter.incrementAndGet() < 100, t -> {
            t.handle(DefaultAsyncResult.succeed());
        }, e -> {
            context.assertTrue(e.succeeded());
            context.assertEquals(100, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void whilstExecutesWithFails(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(() -> counter.incrementAndGet() < 2, t -> {
            t.handle(DefaultAsyncResult.fail(new IllegalAccessException()));
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof IllegalAccessException);
            context.assertEquals(1, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void whilstExecutesWithUnhandledExceptionInLoop(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(() -> counter.incrementAndGet() < 2, t -> {
            throw new IllegalAccessError();
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof IllegalAccessError);
            context.assertEquals(1, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void whilstExecutesWithUnhandledExceptionInTester(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(() -> {
            throw new RuntimeException();
        }, t -> {
            t.handle(DefaultAsyncResult.succeed());
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof RuntimeException);
            context.assertEquals(0, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void untilExecutesEmpty(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.until(() -> true, t -> {
            counter.incrementAndGet();
            t.handle(DefaultAsyncResult.succeed());
        }, e -> {
            context.assertTrue(e.succeeded());
            context.assertEquals(1, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void untilExecutesMany(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.until(() -> counter.incrementAndGet() >= 100, t -> {
            t.handle(DefaultAsyncResult.succeed());
        }, e -> {
            context.assertTrue(e.succeeded());
            context.assertEquals(100, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void untilExecutesAndFails(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.until(() -> counter.incrementAndGet() >= 2, t -> {
            t.handle(DefaultAsyncResult.fail(new IllegalAccessException()));
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof IllegalAccessException);
            context.assertEquals(0, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void untilExecutesUnhandledException(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.until(() -> counter.incrementAndGet() >= 2, t -> {
            throw new IllegalAccessError();
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof IllegalAccessError);
            context.assertEquals(0, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seqWithoutFunctionsExecutes(final TestContext context) {
        final Async async = context.async();
        final BiHandler<Object, Handler<AsyncResult<Void>>> result = instance.seq();

        context.assertNotNull(result);
        rule.vertx().runOnContext(e -> {
            result.handle(null, e1 -> {
                context.assertTrue(e1.succeeded());
                async.complete();
            });
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seqFunctions(final TestContext context) {
        final Async async = context.async();

        final BiHandler<Integer, Handler<AsyncResult<Integer>>> result = instance.seq(
                (t, u) -> {
                    u.handle(DefaultAsyncResult.succeed(t + 1));
                }, (t, u) -> {
                    u.handle(DefaultAsyncResult.succeed(t * 4));
                });

        context.assertNotNull(result);
        rule.vertx().runOnContext(e -> {
            result.handle(3, e1 -> {
                context.assertTrue(e1.succeeded());
                context.assertEquals(16, e1.result());
                async.complete();
            });
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seqFunctionsWithFails(final TestContext context) {
        final Async async = context.async();

        final BiHandler<Integer, Handler<AsyncResult<Integer>>> result = instance.seq(
                (t, u) -> {
                    u.handle(DefaultAsyncResult.succeed(t + 1));
                }, (t, u) -> {
                    u.handle(DefaultAsyncResult.fail(new IllegalArgumentException()));
                });

        context.assertNotNull(result);
        rule.vertx().runOnContext(e -> {
            result.handle(3, e1 -> {
                context.assertFalse(e1.succeeded());
                context.assertTrue(e1.cause() instanceof IllegalArgumentException);
                context.assertNull(e1.result());
                async.complete();
            });
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void seqFunctionsWithUnhandledException(final TestContext context) {
        final Async async = context.async();

        final BiHandler<Integer, Handler<AsyncResult<Integer>>> result = instance.seq(
                (t, u) -> {
                    u.handle(DefaultAsyncResult.succeed(t + 1));
                }, (t, u) -> {
                    throw new IllegalAccessError();
                });

        context.assertNotNull(result);
        rule.vertx().runOnContext(e -> {
            result.handle(3, e1 -> {
                context.assertFalse(e1.succeeded());
                context.assertTrue(e1.cause() instanceof IllegalAccessError);
                context.assertNull(e1.result());
                async.complete();
            });
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void timesWhenThereAreNoItems(final TestContext context) {
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.times((Integer) 0, (value, handler) -> {
            handlerCallCount.incrementAndGet();
            handler.handle(DefaultAsyncResult.succeed(value.toString()));
        }, result -> {
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertTrue(result.result().isEmpty());
            context.assertEquals(0, handlerCallCount.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void timesInFail(final TestContext context) {
        final FakeFailingAsyncFunction function = new FakeFailingAsyncFunction<>(2, null, new RuntimeException("Failed"), true);
        final Async async = context.async();

        instance.times(3, function, result -> {
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertNull(result.result());
            context.assertEquals(3, function.runCount());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void timesWithUnhandledException(final TestContext context) {
        final FakeFailingAsyncFunction function = new FakeFailingAsyncFunction<>(2, null, new RuntimeException("Failed"), false);
        final Async async = context.async();

        instance.times(3, function, result -> {
            context.assertNotNull(result);
            context.assertFalse(result.succeeded());
            context.assertNull(result.result());
            context.assertEquals(3, function.runCount());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void timesWithThreeIteration(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger(0);
        final Async async = context.async();

        instance.times(3, (value, handler) -> {
            counter.incrementAndGet();
            handler.handle(DefaultAsyncResult.succeed(value.toString()));
        }, result -> {
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertEquals(Arrays.asList("0", "1", "2"), result.result());
            context.assertEquals(3, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void raceExecutesEmptyTask(final TestContext context) {
        final Async async = context.async();

        instance.race(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(), result -> {
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertNull(result.result());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void raceExecutesOneTask(final TestContext context) {
        final FakeSuccessfulAsyncSupplier<String> task1 = new FakeSuccessfulAsyncSupplier<>("Task 1");
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.race(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1), result -> {
            context.assertEquals(1, task1.runCount());
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertEquals(result.result(), "Task 1");
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void raceExecutesTwoTasks(final TestContext context) {
        final FakeAsyncSupplier<String> task1 = new FakeAsyncSupplier<String>() {
            @Override
            public void handle(Handler<AsyncResult<String>> u) {
                rule.vertx().setTimer(200, id -> {
                    incrementRunCount();
                    u.handle(DefaultAsyncResult.succeed("Task 1"));
                });
            }
        };
        final FakeAsyncSupplier<String> task2 = new FakeAsyncSupplier<String>() {
            @Override
            public void handle(Handler<AsyncResult<String>> u) {
                rule.vertx().setTimer(100, id -> {
                    incrementRunCount();
                    u.handle(DefaultAsyncResult.succeed("Task 2"));
                });
            }
        };
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.race(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1, task2), result -> {
            context.assertEquals(0, task1.runCount());
            context.assertEquals(1, task2.runCount());
            context.assertNotNull(result);
            context.assertTrue(result.succeeded());
            context.assertEquals(result.result(), "Task 2");
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void raceExecutesTaskInFails(final TestContext context) {
        final FakeAsyncSupplier<String> task1 = new FakeAsyncSupplier<String>() {
            @Override
            public void handle(Handler<AsyncResult<String>> u) {
                rule.vertx().setTimer(200, id -> {
                    incrementRunCount();
                    u.handle(DefaultAsyncResult.succeed("Task 1"));
                });
            }
        };
        final FakeAsyncSupplier<String> task2 = new FakeAsyncSupplier<String>() {
            @Override
            public void handle(Handler<AsyncResult<String>> u) {
                rule.vertx().setTimer(100, id -> {
                    incrementRunCount();
                    u.handle(DefaultAsyncResult.fail(new IllegalArgumentException()));
                });
            }
        };
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.race(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1, task2), result -> {
            context.assertEquals(0, task1.runCount());
            context.assertEquals(1, task2.runCount());
            context.assertFalse(result.succeeded());
            context.assertNotNull(result);
            context.assertNull(result.result());
            context.assertTrue(result.cause() instanceof IllegalArgumentException);
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void raceExecutesTaskWithUnhandledException(final TestContext context) {
        final FakeAsyncSupplier<String> task1 = new FakeAsyncSupplier<String>() {
            @Override
            public void handle(Handler<AsyncResult<String>> u) {
                rule.vertx().setTimer(200, id -> {
                    incrementRunCount();
                    u.handle(DefaultAsyncResult.succeed("Task 1"));
                });
            }
        };
        final FakeAsyncSupplier<String> task2 = new FakeAsyncSupplier<String>() {
            @Override
            public void handle(Handler<AsyncResult<String>> u) {
                incrementRunCount();
                throw new IllegalAccessError();
            }
        };
        final AtomicInteger handlerCallCount = new AtomicInteger(0);
        final Async async = context.async();

        instance.race(Arrays.<Handler<Handler<AsyncResult<String>>>>asList(task1, task2), result -> {
            context.assertEquals(0, task1.runCount());
            context.assertEquals(1, task2.runCount());
            context.assertFalse(result.succeeded());
            context.assertNotNull(result);
            context.assertNull(result.result());
            context.assertTrue(result.cause() instanceof IllegalAccessError);
            context.assertEquals(1, handlerCallCount.incrementAndGet());
            async.complete();
        });
    }

    @Test
    public void createQueue(final TestContext context) {
        context.assertNotNull(instance.<Integer>createQueue((t, u) -> {
            rule.vertx().setTimer(t, event -> {
                u.handle(DefaultAsyncResult.succeed());
            });
        }));
    }

    @Test
    public void createCargo(final TestContext context) {
        context.assertNotNull(instance.<Integer>createCargo((delay, u) -> {
            rule.vertx().setTimer(delay, event -> {
                u.handle(DefaultAsyncResult.succeed());
            });
        }));
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void eachWithNoFunctions(final TestContext context) {
        final Async async = context.async();
        instance.each(Arrays.<BiHandler<String, Handler<AsyncResult<Void>>>>asList(), "TEST", result -> {
            context.assertTrue(result.succeeded());
            context.assertNull(result.result());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void eachWithSingleFunction(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger(0);
        final Async async = context.async();
        instance.each(Arrays.asList((t, u) -> {
            context.assertEquals("TEST", t);
            counter.incrementAndGet();
            u.handle(DefaultAsyncResult.succeed());
        }), "TEST", result -> {
            context.assertTrue(result.succeeded());
            context.assertNull(result.result());
            context.assertEquals(1, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void eachWithtwoFunctions(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger(0);
        final Async async = context.async();
        final BiHandler<String, Handler<AsyncResult<Void>>> function = (t, u) -> {
            context.assertEquals("TEST2", t);
            counter.incrementAndGet();
            u.handle(DefaultAsyncResult.succeed());
        };

        instance.each(Arrays.asList(function, function), "TEST2", result -> {
            context.assertTrue(result.succeeded());
            context.assertNull(result.result());
            context.assertEquals(2, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void eachWithFailingFunction(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger(0);
        final Async async = context.async();
        instance.each(Arrays.asList((t, u) -> {
            context.assertEquals("TEST3", t);
            counter.incrementAndGet();
            u.handle(DefaultAsyncResult.succeed());
        }, (t, u) -> {
            context.assertEquals("TEST3", t);
            counter.incrementAndGet();
            u.handle(DefaultAsyncResult.fail(new IllegalArgumentException()));
        }), "TEST3", result -> {
            context.assertFalse(result.succeeded());
            context.assertNull(result.result());
            context.assertTrue(result.cause() instanceof IllegalArgumentException);
            context.assertEquals(2, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void eachWithRaisExceptionFunction(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger(0);
        final Async async = context.async();
        instance.each(Arrays.asList((t, u) -> {
            context.assertEquals("TEST3", t);
            counter.incrementAndGet();
            u.handle(DefaultAsyncResult.succeed());
        }, (t, u) -> {
            counter.incrementAndGet();
            throw new ClassCastException();
        }), "TEST3", result -> {
            context.assertFalse(result.succeeded());
            context.assertNull(result.result());
            context.assertTrue(result.cause() instanceof ClassCastException);
            context.assertEquals(2, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void duringExecutesEmpty(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(handler -> {
            handler.handle(DefaultAsyncResult.succeed(counter.incrementAndGet() < 1));
        }, t -> {
            t.handle(DefaultAsyncResult.fail(new IllegalAccessException()));
        }, e -> {
            context.assertTrue(e.succeeded());
            context.assertEquals(1, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void duringExecutesMany(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(handler -> {
            handler.handle(DefaultAsyncResult.succeed(counter.incrementAndGet() < 100));
        }, t -> {
            t.handle(DefaultAsyncResult.succeed());
        }, e -> {
            context.assertTrue(e.succeeded());
            context.assertEquals(100, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void duringExecutesWithHandledExceptionInLoop(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(handler -> {
            handler.handle(DefaultAsyncResult.succeed(counter.incrementAndGet() < 2));
        }, t -> {
            t.handle(DefaultAsyncResult.fail(new IllegalAccessException()));
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof IllegalAccessException);
            context.assertEquals(1, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void duringExecutesWithUnhandledExceptionInLoop(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(handler -> {
            handler.handle(DefaultAsyncResult.succeed(counter.incrementAndGet() < 2));
        }, t -> {
            throw new IllegalAccessError();
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof IllegalAccessError);
            context.assertEquals(1, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void duringExecutesWithUnhandledExceptionInTester(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(handler -> {
            throw new IllegalAccessError();
        }, t -> {
            t.handle(DefaultAsyncResult.succeed());
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof IllegalAccessError);
            context.assertEquals(0, counter.get());
            async.complete();
        });
    }

    @Test(timeout = AsyncFlowsTest.TIMEOUT_LIMIT)
    @Repeat(value = AsyncFlowsTest.REPEAT_LIMIT, silent = true)
    public void duringExecutesWithHandledExceptionInTester(final TestContext context) {
        final AtomicInteger counter = new AtomicInteger();
        final Async async = context.async();
        instance.whilst(handler -> {
            handler.handle(DefaultAsyncResult.fail(new IllegalAccessError()));
        }, t -> {
            t.handle(DefaultAsyncResult.succeed());
        }, e -> {
            context.assertFalse(e.succeeded());
            context.assertTrue(e.cause() instanceof IllegalAccessError);
            context.assertEquals(0, counter.get());
            async.complete();
        });
    }

}
