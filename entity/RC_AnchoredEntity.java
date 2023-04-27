package real_combat.entity;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.lazywizard.lazylib.combat.entities.AnchoredEntity;
import org.lwjgl.util.vector.Vector2f;

public class RC_AnchoredEntity extends AnchoredEntity {
    /**
     * Creates a {@code CombatEntityAPI} that follows and rotates with another
     * anchoring {@code CombatEntityAPI}.
     *
     * @param anchor   The {@link CombatEntityAPI} to follow and rotate with.
     * @param location The location relative to {@code anchor} to track.
     * @since 1.5
     */
    private float relativeAngle;
    public RC_AnchoredEntity(CombatEntityAPI anchor, Vector2f location, float relativeAngle) {
        super(anchor, location);
        this.relativeAngle = relativeAngle;
    }

    public float getRelativeAngle() {
        return relativeAngle;
    }
}
