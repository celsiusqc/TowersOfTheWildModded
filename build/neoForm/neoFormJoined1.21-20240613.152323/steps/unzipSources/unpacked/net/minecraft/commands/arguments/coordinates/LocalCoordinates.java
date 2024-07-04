package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class LocalCoordinates implements Coordinates {
    public static final char PREFIX_LOCAL_COORDINATE = '^';
    private final double left;
    private final double up;
    private final double forwards;

    public LocalCoordinates(double pLeft, double pUp, double pForwards) {
        this.left = pLeft;
        this.up = pUp;
        this.forwards = pForwards;
    }

    @Override
    public Vec3 getPosition(CommandSourceStack pSource) {
        Vec2 vec2 = pSource.getRotation();
        Vec3 vec3 = pSource.getAnchor().apply(pSource);
        float f = Mth.cos((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
        float f1 = Mth.sin((vec2.y + 90.0F) * (float) (Math.PI / 180.0));
        float f2 = Mth.cos(-vec2.x * (float) (Math.PI / 180.0));
        float f3 = Mth.sin(-vec2.x * (float) (Math.PI / 180.0));
        float f4 = Mth.cos((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
        float f5 = Mth.sin((-vec2.x + 90.0F) * (float) (Math.PI / 180.0));
        Vec3 vec31 = new Vec3((double)(f * f2), (double)f3, (double)(f1 * f2));
        Vec3 vec32 = new Vec3((double)(f * f4), (double)f5, (double)(f1 * f4));
        Vec3 vec33 = vec31.cross(vec32).scale(-1.0);
        double d0 = vec31.x * this.forwards + vec32.x * this.up + vec33.x * this.left;
        double d1 = vec31.y * this.forwards + vec32.y * this.up + vec33.y * this.left;
        double d2 = vec31.z * this.forwards + vec32.z * this.up + vec33.z * this.left;
        return new Vec3(vec3.x + d0, vec3.y + d1, vec3.z + d2);
    }

    @Override
    public Vec2 getRotation(CommandSourceStack pSource) {
        return Vec2.ZERO;
    }

    @Override
    public boolean isXRelative() {
        return true;
    }

    @Override
    public boolean isYRelative() {
        return true;
    }

    @Override
    public boolean isZRelative() {
        return true;
    }

    public static LocalCoordinates parse(StringReader pReader) throws CommandSyntaxException {
        int i = pReader.getCursor();
        double d0 = readDouble(pReader, i);
        if (pReader.canRead() && pReader.peek() == ' ') {
            pReader.skip();
            double d1 = readDouble(pReader, i);
            if (pReader.canRead() && pReader.peek() == ' ') {
                pReader.skip();
                double d2 = readDouble(pReader, i);
                return new LocalCoordinates(d0, d1, d2);
            } else {
                pReader.setCursor(i);
                throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(pReader);
            }
        } else {
            pReader.setCursor(i);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(pReader);
        }
    }

    private static double readDouble(StringReader pReader, int pStart) throws CommandSyntaxException {
        if (!pReader.canRead()) {
            throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(pReader);
        } else if (pReader.peek() != '^') {
            pReader.setCursor(pStart);
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(pReader);
        } else {
            pReader.skip();
            return pReader.canRead() && pReader.peek() != ' ' ? pReader.readDouble() : 0.0;
        }
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof LocalCoordinates localcoordinates)
                ? false
                : this.left == localcoordinates.left && this.up == localcoordinates.up && this.forwards == localcoordinates.forwards;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.left, this.up, this.forwards);
    }
}
