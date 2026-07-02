package com.sharenote.security.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Registry {
    // Thread-safe immutable map holding our global definitions
    private Map<Long, String> registry = Map.of();

    /**
     * Called exactly once at startup during the warm-up phase to load definitions.
     */
    public synchronized void initialize(Map<Long, String> definitions) {
        if (!this.registry.isEmpty()) {
            throw new IllegalStateException("Registry has already been initialized!");
        }
        this.registry = Map.copyOf(definitions); // Immutable copy
    }

    /**
     * O(1) Look-up used by authorization interceptors/filters on every
     * request.
     */
    public String getDefinition(Long Id) {
        return Optional.ofNullable(registry.get(Id)).orElse(null);
    }

    /**
     * Retrieve definitions for a list of IDs.
     * 
     * @param Ids the list of entries keys
     * @return the list of corresponding definitions
     */
    public List<String> getDefinitions(List<Long> Ids) {
        if (Ids == null || Ids.isEmpty()) {
            return Collections.emptyList();
        }

        // Pre-allocate space to reduce array resizing overhead
        List<String> definitions = new ArrayList<>(Ids.size());

        for (Long id : Ids) {
            String definition = this.registry.get(id);
            if (definition != null) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    /**
     * Removes a entry from the registry. This is a rare operation and should
     * be used with caution, as it can affect authorization decisions.
     *
     * @param Id the ID of the entry to remove
     * @throws IllegalStateException if the Id ID is not found in the
     *                               registry
     */
    public synchronized void removeEntry(Long Id) {
        if (!this.registry.containsKey(Id)) {
            throw new IllegalStateException("Id of entry not found in registry!");
        }
        Map<Long, String> map = new HashMap<>(this.registry);
        map.remove(Id);
        this.registry = Map.copyOf(map);
    }

    /**
     * Adds a new entry to the registry. This is a rare operation and should
     * be used with caution, as it can affect authorization decisions.
     *
     * @param Id the ID of the entry to add
     * @param definition   the definition of the entry
     * @throws IllegalStateException if the entry ID already exists in the
     *                               registry
     */
    public synchronized void addEntry(Long Id, String definition) {
        if (this.registry.containsKey(Id)) {
            throw new IllegalStateException("Id of entry already exists in registry!");
        }
        this.updateEntry(Id, definition);
    }

    /**
     * Updates the definition of an existing entry in the registry. This is a
     * rare operation and should
     * be used with caution, as it can affect authorization decisions.
     *
     * @param Id  the ID of the entry to update
     * @param newDefinition the new definition of the entry
     * @throws IllegalStateException if the entry ID is not found in the registry
     */
    public synchronized void updateEntry(Long Id, String newDefinition) {
        if (!this.registry.containsKey(Id)) {
            throw new IllegalStateException("Entry ID not found in registry!");
        }
        // 1. Instantly copy all elements into a mutable map shell
        Map<Long, String> map = new HashMap<>(this.registry);

        // 2. Overwrite the target Entry (replaces the old definition automatically)
        map.put(Id, newDefinition);

        // 3. Lock it back down into an immutable map
        this.registry = Map.copyOf(map);
    }

    public boolean contains(Long Id) {
        return registry.containsKey(Id);
    }

    public int totalEntries() {
        return registry.size();
    }
}
