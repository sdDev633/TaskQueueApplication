package com.taskqueue.www.handler;

public interface TaskHandler {
    String getType();
    void handle(String data) throws Exception;
}
