package com.xored.javafx.packeteditor.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TrexGuiceModule {
    private static Injector injector = Guice.createInjector(new GuiceModule());

    private TrexGuiceModule() {}

    public static Injector injector() {
        return injector;
    }
}
