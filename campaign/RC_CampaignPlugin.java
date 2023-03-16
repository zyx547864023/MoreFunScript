package real_combat.campaign;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public class RC_CampaignPlugin extends BaseCampaignPlugin {

    @Override
    public String getId() {
        return "RC_CampaignPlugin";
    }

    @Override
    public boolean isTransient() {
        return true;
    }

    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(SectorEntityToken interactionTarget) {
        if ((interactionTarget instanceof CampaignFleetAPI) && interactionTarget.getFaction().getId().contentEquals("famous_bounty")) {
            return new PluginPick<InteractionDialogPlugin>(new RC_IBBInteractionDialogPlugin(), PickPriority.HIGHEST);
        }
        return null;
    }
}
