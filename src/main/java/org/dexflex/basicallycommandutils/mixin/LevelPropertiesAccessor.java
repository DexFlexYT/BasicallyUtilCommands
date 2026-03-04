package org.dexflex.basicallycommandutils.mixin;

import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelProperties.class)
public interface LevelPropertiesAccessor {

    @Accessor("time")
    void setGameTime(long value);
}