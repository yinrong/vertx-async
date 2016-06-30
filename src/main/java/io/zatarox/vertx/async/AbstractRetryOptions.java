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
package io.zatarox.vertx.async;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import java.util.function.Consumer;

/**
 * This class define an abstract parameters entity for a retry method call.
 * 
 */
public abstract class AbstractRetryOptions<T> {

    protected final long tries;

    protected AbstractRetryOptions(long tries) {
        if (tries < 1) {
            throw new IllegalArgumentException("Tries must be positive");
        }
        
        this.tries = tries;
    }

    public long getTries() {
        return tries;
    }
    
    public abstract Handler<Void> build(final Consumer<Handler<AsyncResult<T>>> task, final Handler<AsyncResult<T>> handler);

}
