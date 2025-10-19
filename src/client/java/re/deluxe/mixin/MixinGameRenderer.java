package re.deluxe.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.deluxe.Darkness;
import re.deluxe.LightmapAccess;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow @Final
    private Minecraft minecraft;

    @Shadow @Final
    private LightTexture lightTexture;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void onRenderLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        final LightmapAccess lightmap = (LightmapAccess) lightTexture;

        if (lightmap.darkness_isDirty()) {
            ProfilerFiller profilerFiller = Profiler.get();
            profilerFiller.push("lightTex");

            // Usa el delta desde DeltaTracker
            float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);

            Darkness.updateLuminance(tickDelta, minecraft, (GameRenderer) (Object) this, lightmap.darkness_prevFlicker());

            profilerFiller.pop();
        }
    }
}
