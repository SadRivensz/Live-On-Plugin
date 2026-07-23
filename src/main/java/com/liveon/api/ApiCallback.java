package com.liveon.api;

public interface ApiCallback<T>
{
    void onSuccess(T value);

    void onFailure(String message);
}
