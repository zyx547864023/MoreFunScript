package real_combat.campaign.customstart;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.newgame.NGCAddStartingShipsByFleetType;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.util.Misc;
import exerelin.campaign.customstart.CustomStart;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import com.fs.starfarer.api.impl.campaign.rulecmd.newgame.NGCAddStartingShipsByFleetType;
//import exerelin.campaign.PlayerFactionStore;
//import exerelin.campaign.customstart.CustomStart;
//import exerelin.utilities.NexUtils;

public class RC_SuperStart extends CustomStart {

    protected List<String> ships = new ArrayList<>(Arrays.asList(new String[]{
        "hyperion_meta_variant_n"
    }));

    private static final Color HIGHLIGHT_COLOR = Global.getSettings().getColor("buttonShortcut");

    //@Override
    public void execute(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        CharacterCreationData data = (CharacterCreationData) memoryMap.get(MemKeys.LOCAL).get("$characterData");
        NGCAddStartingShipsByFleetType.generateFleetFromVariantIds(dialog, data, null, ships);
        NGCAddStartingShipsByFleetType.addStartingDModScript(memoryMap.get(MemKeys.LOCAL));
        data.addScript(new Script() {
            @Override
            public void run() {
                CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
                for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                    ShipVariantAPI v = member.getVariant().clone();
                    v.setSource(VariantSource.REFIT);
                    v.setHullVariantId(Misc.genUID());
                    member.setVariant(v, false, false);
                }
                fleet.getFleetData().setSyncNeeded();
                fleet.getFleetData().syncIfNeeded();
            }
        });

        dialog.getTextPanel().addPara("一个全新的英仙座 传奇 即将诞生。",
                HIGHLIGHT_COLOR, Misc.ucFirst("传奇"));

        FireBest.fire(null, dialog, memoryMap, "ExerelinNGCStep4");
    }
}
