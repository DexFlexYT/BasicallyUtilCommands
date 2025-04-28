package org.dexflex.basicallycommandutils.mixin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerEntity.class)
public abstract class PlayerNbtMixin {
    private String savedPlayerName;
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void writePlayerName(NbtCompound nbt, CallbackInfo ci) {
        String username = ((PlayerEntity)(Object)this).getName().getString();
        nbt.putString("PlayerName", username);
    }
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void readPlayerName(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("PlayerName", 8)) {
            this.savedPlayerName = nbt.getString("PlayerName");
        }
    }
    public String getSavedPlayerName() {
        return this.savedPlayerName;
    }
}