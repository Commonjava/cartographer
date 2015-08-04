package org.commonjava.maven.cartographer.ops.fn;

import java.util.function.Consumer;

public class ValueHolder<T>
{

    private T value;

    public T getValue()
    {
        return value;
    }

    public void setValue( final T value )
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
