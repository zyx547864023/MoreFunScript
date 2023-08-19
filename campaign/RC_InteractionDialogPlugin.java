package real_combat.campaign;

import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

public class RC_InteractionDialogPlugin extends FleetInteractionDialogPluginImpl {

    public RC_InteractionDialogPlugin() {
        this(null);
    }

    public RC_InteractionDialogPlugin(FIDConfig params) {
        super(params);
        context = new RC_FleetEncounterContext();
    }
}
