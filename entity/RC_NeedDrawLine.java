package real_combat.entity;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RC_NeedDrawLine {
    public ShipAPI ship;
    public Color color;
    public float timer;
    public java.util.List<Vector2f> startList = new ArrayList<>();
    public java.util.List<Vector2f> endList = new ArrayList<>();
    public RC_NeedDrawLine(ShipAPI ship, float timer, java.util.List<Vector2f> startList, java.util.List<Vector2f> endList, Color color){
        this.ship = ship;
        this.timer = timer;
        this.startList = startList;
        this.endList = endList;
        this.color = color;
    }
    public java.util.List<Vector2f> getStartList()
    {
        return startList;
    }
    public List<Vector2f> getEndList()
    {
        return endList;
    }

    public Color getColor() {
        return color;
    }

    public float getTimer() {
        return timer;
    }

    public void setTimer(float timer) {
        this.timer = timer;
    }
}
