```java
  /**
     * Applies the given window function to each window. The window function is called for each
     * evaluation of the window for each key individually. The output of the window function is
     * interpreted as a regular non-windowed stream.
     *
     * <p>Note that this function requires that all data in the windows is buffered until the window
     * is evaluated, as the function provides no means of incremental aggregation.
     *
     * @param function The window function.
     * @return The data stream that is the result of applying the window function to the window.
     */
    public <R> SingleOutputStreamOperator<R> apply(WindowFunction<T, R, K, W> function) {
        TypeInformation<R> resultType = getWindowFunctionReturnType(function, getInputType());

        return apply(function, resultType);
    }
```
