package org.carpetorgaddition.rule;

import carpet.api.settings.CarpetRule;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class CustomRuleEntry {
    private final String name;
    private final CarpetRule<?> rule;
    private final CustomRuleControl<?> control;
    private static final HashMap<CustomRuleControl<?>, CustomRuleEntry> CACHE = new HashMap<>();

    private CustomRuleEntry(String name, CarpetRule<?> rule, CustomRuleControl<?> control) {
        this.name = name;
        this.rule = rule;
        this.control = control;
    }

    public static Optional<CustomRuleEntry> of(String name, CarpetRule<?> rule, CustomRuleControl<?> control) {
        if (name == null || rule == null || control == null) {
            return Optional.empty();
        }
        return Optional.of(CACHE.computeIfAbsent(control, __ -> new CustomRuleEntry(name, rule, control)));
    }

    public String getName() {
        return name;
    }

    public CarpetRule<?> getRule() {
        return rule;
    }

    public CustomRuleControl<?> getControl() {
        return control;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        CustomRuleEntry that = (CustomRuleEntry) obj;
        return Objects.equals(this.name, that.name) && Objects.equals(this.rule, that.rule) && Objects.equals(this.control, that.control);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rule, control);
    }
}
