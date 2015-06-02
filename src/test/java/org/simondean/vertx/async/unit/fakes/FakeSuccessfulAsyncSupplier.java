package org.simondean.vertx.async.unit.fakes;

import org.simondean.vertx.async.DefaultAsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.impl.DefaultFutureResult;

public class FakeSuccessfulAsyncSupplier<T> extends FakeAsyncSupplier<T> {
  private final T result;

  public FakeSuccessfulAsyncSupplier(T result) {
    this.result = result;
  }

  @Override
  public void accept(AsyncResultHandler<T> handler) {
    incrementRunCount();
    handler.handle(DefaultAsyncResult.succeed(result));
  }

  public T result() {
    return result;
  }
}
