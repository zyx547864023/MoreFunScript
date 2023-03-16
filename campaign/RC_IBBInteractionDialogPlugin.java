package real_combat.campaign;

import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl;

public class RC_IBBInteractionDialogPlugin extends FleetInteractionDialogPluginImpl {

    public RC_IBBInteractionDialogPlugin() {
        this(null);
    }

    public RC_IBBInteractionDialogPlugin(FIDConfig params) {
        super(params);
        context = new RC_IBBFleetEncounterContext();
    }
}
