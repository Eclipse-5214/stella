package co.stellarskys.stella.mixins.accessors;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GpuDevice.class)
public interface AccessorGpuDevice {
    @Accessor("backend")
    GpuDeviceBackend getBackend();
}
