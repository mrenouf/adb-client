package com.bitgrind.android.adb;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by mrenouf on 2/26/17.
 */
public class CtorRef {
    public CtorRef() {}

    public CtorRef(String s) {}

    interface CtorRefFactory {
        CtorRef make(String s);
    }

    static {
        CtorRefFactory f = CtorRef::new;
        Supplier<CtorRef> s = CtorRef::new;
        f.make("");
        s.get();
        Collectors.toList();
        Function<ArrayList, Object> arrayListObjectFunction = ArrayList::new;
        arrayListObjectFunction.apply(null);
    }
}
