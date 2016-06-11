package io.zatarox.vertx.async.examples;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import java.util.List;
import io.zatarox.vertx.async.Async;
import io.zatarox.vertx.async.DefaultAsyncResult;

public class SeriesExample extends BaseExample {

    private final boolean succeed;
    private List<String> results;

    public SeriesExample(boolean succeed) {
        this.succeed = succeed;
    }

    public void seriesExample(Handler<AsyncResult<List<String>>> handler) {
        Async.<String>series()
                .task(taskHandler -> {
                    String result = getSomeResult();
                    taskHandler.handle(DefaultAsyncResult.succeed(result));
                })
                .task(taskHandler -> {
                    someAsyncMethodThatTakesAHandler(taskHandler);
                })
                .run(result -> {
                    if (result.failed()) {
                        handler.handle(DefaultAsyncResult.fail(result));
                        return;
                    }

                    List<String> resultList = result.result();
                    doSomethingWithTheResults(resultList);

                    handler.handle(DefaultAsyncResult.succeed(resultList));
                });
    }

    private String getSomeResult() {
        return "Result";
    }

    private void someAsyncMethodThatTakesAHandler(Handler<AsyncResult<String>> handler) {
        if (!succeed) {
            handler.handle(DefaultAsyncResult.fail(new Exception("Fail")));
            return;
        }

        handler.handle(DefaultAsyncResult.succeed("Async result"));
    }

    private void doSomethingWithTheResults(List<String> results) {
        this.results = results;
    }

    public List<String> results() {
        return results;
    }
}