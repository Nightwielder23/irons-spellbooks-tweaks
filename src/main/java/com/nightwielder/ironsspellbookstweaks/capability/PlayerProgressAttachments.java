// Data attachment registry for per-player progress.
package com.nightwielder.ironsspellbookstweaks.capability;

import com.nightwielder.ironsspellbookstweaks.IronsSpellbooksTweaks;
import java.util.function.Supplier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class PlayerProgressAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, IronsSpellbooksTweaks.MOD_ID);

    // copyOnDeath round-trips via INBTSerializable, so death/respawn cloning is automatic. Cross-dimension travel goes through the player NBT save path so attachments survive that without help.
    public static final Supplier<AttachmentType<PlayerProgress>> PLAYER_PROGRESS = ATTACHMENTS.register(
            "player_progress",
            () -> AttachmentType.serializable(PlayerProgress::new).copyOnDeath().build());

    private PlayerProgressAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
