package org.commonjava.cartographer.graph.fn;

import java.util.function.Consumer;

public class ValueHolder<T>
{

    private T value;

    public T get()
    {
        return value;
    }

    public void set( final T value )
    {
        this.value = value;
    }

    public Consumer<T> consumer()
    {
        return ( t ) -> {
            value = t;
        };
    }

}
