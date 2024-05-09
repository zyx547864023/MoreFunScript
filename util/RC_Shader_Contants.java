package real_combat.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.apache.log4j.Level;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.vector.Vector2f;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class RC_Shader_Contants {
    public static final String VERT = "#version 110\n" +
            "\n" +
            "varying vec2 fragCoord;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = ftransform();\n" +
            "    fragCoord = 0.25*gl_MultiTexCoord0.xy;\n" +
            "}\n";
    public static final String FRAG =
            "#version 110\n" +
            "uniform float iTime;\n" +
            "uniform vec3 iResolution;\n" +
            "uniform vec2 iLocation;\n" +
            "uniform float iViewMult;\n" +
            "uniform float iRadius;\n" +
            "uniform float iEffectLevel;\n" +
            "varying vec2 fragCoord;\n" +
            "void main()\n" +
            "{\n" +
            "\tvec2 p = (iViewMult*gl_FragCoord.xy-iResolution.xy-iLocation.xy)/iResolution.y;\n" +
            "    float tau = 3.1415926535*2.0;\n" +
            "    float a = atan(p.x,p.y);\n" +
            "    float r = length(p)*iRadius*iViewMult;\n" +
            "    vec2 uv = vec2(a/tau,r);\n" +
            "\t\n" +
            "\t//get the color\n" +
            "\tfloat xCol = (uv.x - (iTime / 3.0)) * 3.0;\n" +
            "\txCol = mod(xCol, 3.0);\n" +
            "\tvec3 horColour = vec3(0.25, 0.25, 0.25);\n" +
            "\t\n" +
            "\tif (xCol < 1.0) {\n" +
            "\t\t\n" +
            "\t\thorColour.r += 1.0 - xCol;\n" +
            "\t\thorColour.g += xCol;\n" +
            "\t}\n" +
            "\telse if (xCol < 2.0) {\n" +
            "\t\t\n" +
            "\t\txCol -= 1.0;\n" +
            "\t\thorColour.g += 1.0 - xCol;\n" +
            "\t\thorColour.b += xCol;\n" +
            "\t}\n" +
            "\telse {\n" +
            "\t\t\n" +
            "\t\txCol -= 2.0;\n" +
            "\t\thorColour.b += 1.0 - xCol;\n" +
            "\t\thorColour.r += xCol;\n" +
            "\t}\n" +
            "\n" +
            "\t// draw color beam\n" +
            "\tuv = (2.0 * uv) - 1.0;\n" +
            "\tfloat beamWidth = (0.7+0.5*cos(uv.x*10.0*tau*0.15*clamp(floor(5.0 - 10.0), 0.0, 10.0))) * abs(1.0 / (30.0 * uv.y));\n" +
            "\tvec3 horBeam = vec3(beamWidth);\n" +
            "\tgl_FragColor = vec4((( horBeam) * horColour), iEffectLevel);\n" +
            //"\tif (horBeam.x<0.01&&horBeam.y<0.01&&horBeam.z<0.01) {\n" +
            //"\t        gl_FragColor = vec4((( horBeam) * horColour), 0.0);\n" +
            //"\t    } else {\n" +
            //"\t        gl_FragColor = vec4((( horBeam) * horColour), 1.0);\n" +
            //"\t    }"+
            "}";
}
