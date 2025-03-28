package org.opencds.cqf.fhir.cr.hapi.common;

import java.util.concurrent.ThreadFactory;

/**
 * This class resolves issues with loading JAXB in a server environment and using CompletableFutures
 * <a href="https://stackoverflow.com/questions/49113207/completablefuture-forkjoinpool-set-class-loader">...</a>
 **/
public class CqlThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        return new CqlThread(r);
    }

    private static class CqlThread extends Thread {
        private CqlThread(Runnable runnable) {
            super(runnable);
            // set the correct classloader here
            setContextClassLoader(Thread.currentThread().getContextClassLoader());
        }
    }
}
