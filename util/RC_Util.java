package real_combat.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class RC_Util {

    public static int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
    }

    public static Vector2f getCollisionRayCircle(Vector2f start, Vector2f end, Vector2f circle, float radius, boolean getNear) {
        float x1 = start.x - circle.x;
        float x2 = end.x - circle.x;
        float y1 = start.y - circle.y;
        float y2 = end.y - circle.y;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dr_sqrd = (dx * dx) + (dy * dy);
        float D = (x1 * y2) - (x2 * y1);
        float delta = (radius * radius * dr_sqrd) - (D * D);
        if (delta < 0f) {
            return null;
        } else if (delta > 0f) {
            float x_sub = Math.signum(dy) * dx * (float) Math.sqrt(delta);
            float x_a = ((D * dy) + x_sub) / dr_sqrd;
            float x_b = ((D * dy) - x_sub) / dr_sqrd;
            float y_sub = Math.abs(dy) * (float) Math.sqrt(delta);
            float y_a = ((-D * dx) + y_sub) / dr_sqrd;
            float y_b = ((-D * dx) - y_sub) / dr_sqrd;
            float dax = x_a - x1;
            float dbx = x_b - x1;
            float day = y_a - y1;
            float dby = y_b - y1;
            float dist_a_sqrt = (dax * dax) + (day * day);
            float dist_b_sqrt = (dbx * dbx) + (dby * dby);
            if ((dist_a_sqrt < dist_b_sqrt) ^ !getNear) {
                return new Vector2f(x_a + circle.x, y_a + circle.y);
            } else {
                return new Vector2f(x_b + circle.x, y_b + circle.y);
            }
        } else {
            float x = (D * dy) / dr_sqrd;
            float y = (-D * dx) / dr_sqrd;
            return new Vector2f(x + circle.x, y + circle.y);
        }
    }

    /* LazyLib 2.4b revert */
    public static List<ShipAPI> getShipsWithinRange(Vector2f location, float range) {
        List<ShipAPI> ships = new ArrayList<>();

        for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
            if (tmp.isShuttlePod()) {
                continue;
            }

            if (MathUtils.isWithinRange(tmp, location, range)) {
                ships.add(tmp);
            }
        }

        return ships;
    }

    /* LazyLib 2.4b revert */
    public static List<MissileAPI> getMissilesWithinRange(Vector2f location, float range) {
        List<MissileAPI> missiles = new ArrayList<>();

        for (MissileAPI tmp : Global.getCombatEngine().getMissiles()) {
            if (MathUtils.isWithinRange(tmp.getLocation(), location, range)) {
                missiles.add(tmp);
            }
        }

        return missiles;
    }

    public static boolean hide(CombatEngineAPI engine){
        if (engine == null || engine.getCombatUI() == null) {return true;}
        if (engine.getCombatUI().isShowingCommandUI() || engine.isUIShowingDialog()) {return true;}
        return !engine.isUIShowingHUD();
    }

    /**
     * 获取两条直线的交点
     * @param line1
     * @param line2
     * @return
     */
    public static Point2D getIntersectPointBy2Line(Line2D line1, Line2D line2) {
        return getIntersectPointBy2Line(line1.getP1(),line1.getP2(),line2.getP1(),line2.getP2());

    }


    /**
     * 获取两条直线的交点
     * @param p1 line1 第一个点
     * @param p2 line1 第二个点
     * @param p3 line2 第一个点
     * @param p4 line2 第二个点
     * @return
     */
    public static Point2D getIntersectPointBy2Line(Point2D p1, Point2D p2, Point2D p3, Point2D p4){


        double A1=p1.getY()-p2.getY();
        double B1=p2.getX()-p1.getX();
        double C1=A1*p1.getX()+B1*p1.getY();

        double A2=p3.getY()-p4.getY();
        double B2=p4.getX()-p3.getX();
        double C2=A2*p3.getX()+B2*p3.getY();

        double det_k=A1*B2-A2*B1;

        if(Math.abs(det_k)<0.00001){
            return null;
        }

        double a=B2/det_k;
        double b=-1*B1/det_k;
        double c=-1*A2/det_k;
        double d=A1/det_k;

        double x=a*C1+b*C2;
        double y=c*C1+d*C2;

        return  new Point2D.Double(x,y);
    }
}
