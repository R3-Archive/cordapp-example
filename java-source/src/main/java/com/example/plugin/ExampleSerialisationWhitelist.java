package com.example.plugin;

import kotlin.Triple;
import net.corda.core.serialization.SerializationWhitelist;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ExampleSerialisationWhitelist implements SerializationWhitelist {
    @NotNull
    @Override
    public List<Class<?>> getWhitelist() {
        return Collections.singletonList(Triple.class);
    }
}
