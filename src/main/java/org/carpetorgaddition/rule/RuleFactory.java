package org.carpetorgaddition.rule;

import carpet.api.settings.CarpetRule;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.validator.AbstractValidator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RuleFactory {
    public static <T> Builder<T> create(Class<T> type, String rule, T value) {
        return new Builder<>(type, rule, value);
    }

    public static class Builder<T> {
        private final Class<T> type;
        private final String name;
        private final Collection<String> categories = new ArrayList<>();
        private final Collection<String> suggestions;
        @NotNull
        private final T value;
        private final List<AbstractValidator<T>> validators = new ArrayList<>();
        private final List<RuleObserver<T>> observers = new ArrayList<>();
        private boolean canBeToggledClientSide = false;
        private boolean strict = true;
        private String displayName = "";
        private String displayDesc = "";

        private Builder(Class<T> type, String rule, @NotNull T value) {
            if (type != value.getClass()) {
                // 基本数据类型和它们对应的包装类是不同的数据类型
                throw new IllegalArgumentException();
            }
            this.type = type;
            this.value = value;
            this.name = rule;
            if (this.type == Boolean.class) {
                // 不可变集合
                this.suggestions = List.of("true", "false");
            } else if (this.type.isEnum()) {
                this.suggestions = Arrays.stream(this.type.getEnumConstants())
                        .map(e -> (Enum<?>) e)
                        .map(Enum::name)
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .toList();
            } else {
                this.suggestions = new ArrayList<>();
            }
            this.categories.add(CarpetOrgAdditionSettings.ORG);
        }

        public Builder<T> addCategories(String... categories) {
            this.categories.addAll(List.of(categories));
            return this;
        }

        public Builder<T> addOptions(String... options) {
            this.suggestions.addAll(List.of(options));
            return this;
        }

        public Builder<T> setClientSide(boolean clientSide) {
            this.canBeToggledClientSide = clientSide;
            return this;
        }

        public Builder<T> setStrict(boolean strict) {
            this.strict = strict;
            return this;
        }

        public Builder<T> setDisplayName(String name) {
            this.displayName = name;
            return this;
        }

        public Builder<T> setDisplayDesc(String desc) {
            this.displayDesc = desc;
            return this;
        }

        @SafeVarargs
        public final Builder<T> addValidators(AbstractValidator<T>... validators) {
            this.validators.addAll(List.of(validators));
            return this;
        }

        @SafeVarargs
        public final Builder<T> addObservers(RuleObserver<T>... observers) {
            this.observers.addAll(List.of(observers));
            return this;
        }

        public CarpetRule<T> build() {
            return new OrgRule<>(
                    this.type,
                    this.name,
                    this.categories,
                    this.suggestions,
                    this.value,
                    this.canBeToggledClientSide,
                    this.validators,
                    this.observers,
                    this.strict,
                    this.displayName,
                    this.displayDesc
            );
        }
    }
}
