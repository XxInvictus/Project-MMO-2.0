package harmonised.pmmo.events.impl;

import java.util.Map;

import harmonised.pmmo.api.enums.EventType;
import harmonised.pmmo.core.Core;
import harmonised.pmmo.network.Networking;
import harmonised.pmmo.network.clientpackets.CP_SyncData_DataSkills;
import harmonised.pmmo.network.clientpackets.CP_SyncData_PerkSettings;
import harmonised.pmmo.network.clientpackets.CP_UpdateExperience;
import harmonised.pmmo.network.clientpackets.CP_UpdateLevelCache;
import harmonised.pmmo.storage.PmmoSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.LogicalSide;

public class LoginHandler {

	public static void handle(PlayerLoggedInEvent event) {
		ServerPlayer player = (ServerPlayer) event.getPlayer();
		
		Core.get(LogicalSide.SERVER).getPerkRegistry().terminatePerk(EventType.DISABLE_PERK, player);
		Core.get(LogicalSide.CLIENT).getPerkRegistry().terminatePerk(EventType.DISABLE_PERK, player);
		//===========UPDATE DATA MIRROR=======================
		for (Map.Entry<String, Long> skillMap : PmmoSavedData.get().getXpMap(player.getUUID()).entrySet()) {
			Networking.sendToClient(new CP_UpdateExperience(skillMap.getKey(), skillMap.getValue()), player);
		}
		Networking.sendToClient(new CP_UpdateLevelCache(PmmoSavedData.get().getLevelCache()), player);
		Networking.sendToClient(new CP_SyncData_DataSkills(Core.get(LogicalSide.SERVER).getDataConfig().getSkillData()), player);
		Networking.sendToClient(new CP_SyncData_PerkSettings(Core.get(LogicalSide.SERVER).getPerkRegistry().getSettings()), player);		
		//===========EXECUTE FEATURE LOGIC====================
		PmmoSavedData.get().awardScheduledXP(player.getUUID());
	}
}
