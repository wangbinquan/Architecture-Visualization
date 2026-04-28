package com.archvis.model;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class InMemoryArchModel {

    private final AtomicReference<ModelSnapshot> ref = new AtomicReference<>(ModelSnapshot.empty());

    public void update(ModelSnapshot snapshot) {
        ref.set(snapshot);
    }

    public ModelSnapshot get() {
        return ref.get();
    }
}
