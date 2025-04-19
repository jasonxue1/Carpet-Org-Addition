package org.carpetorgaddition.mixin.rule.carpet;

import carpet.api.settings.CarpetRule;
import carpet.api.settings.Rule;
import carpet.api.settings.SettingsManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.rule.Hidden;
import org.carpetorgaddition.rule.Removed;
import org.carpetorgaddition.rule.RuleSelf;
import org.carpetorgaddition.rule.RuleSelfManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

@Mixin(value = SettingsManager.class, remap = false)
public class SettingsManagerMixin {


    @SuppressWarnings("unchecked")
    @WrapOperation(method = "parseSettingsClass", at = @At(value = "INVOKE", target = "Ljava/lang/reflect/Field;getAnnotation(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;", ordinal = 0))
    private <T extends Annotation> T shouldRegister(Field field, Class<Rule> annotationClass, Operation<Rule> original) {
        return this.shouldRegister(field) ? (T) original.call(field, annotationClass) : null;
    }


    @Inject(method = "parseSettingsClass", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private void customizableRule(Class<?> settingsClass, CallbackInfo ci, @Local CarpetRule<?> rule, @Local Field field) {
        if (field.isAnnotationPresent(RuleSelf.class)) {
            RuleSelfManager.RULES.put(rule.name(), rule);
        }
    }

    @Unique
    private boolean shouldRegister(Field field) {
        if (field.isAnnotationPresent(Removed.class)) {
            return false;
        }
        if (field.isAnnotationPresent(Hidden.class)) {
            return CarpetOrgAddition.ENABLE_HIDDEN_FUNCTION;
        }
        return true;
    }
}
