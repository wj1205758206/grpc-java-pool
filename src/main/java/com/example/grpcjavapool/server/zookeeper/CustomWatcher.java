package com.example.grpcjavapool.server.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomWatcher implements Watcher {
    private static final Logger logger = LoggerFactory.getLogger(CustomWatcher.class);

    @Override
    public void process(WatchedEvent watchedEvent) {
        logger.info("watchedEvent status:{}", watchedEvent.getState());
        logger.info("watchedEvent path:{}", watchedEvent.getPath());
        logger.info("watchedEvent type:{}", watchedEvent.getType());
    }
}
