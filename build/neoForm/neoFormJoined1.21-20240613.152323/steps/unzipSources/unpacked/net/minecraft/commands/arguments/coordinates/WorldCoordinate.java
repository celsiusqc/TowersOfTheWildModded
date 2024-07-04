package net.minecraft.commands.arguments.coordinates;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

public class WorldCoordinate {
    private static final char PREFIX_RELATIVE = '~';
    public static final SimpleCommandExceptionType ERROR_EXPECTED_DOUBLE = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.double"));
    public static final SimpleCommandExceptionType ERROR_EXPECTED_INT = new SimpleCommandExceptionType(Component.translatable("argument.pos.missing.int"));
    private final boolean relative;
    private final double value;

    public WorldCoordinate(boolean pRelative, double pValue) {
        this.relative = pRelative;
        this.value = pValue;
    }

    public double get(double pCoord) {
        return this.relative ? this.value + pCoord : this.value;
    }

    public static WorldCoordinate parseDouble(StringReader pReader, boolean pCenterCorrect) throws CommandSyntaxException {
        if (pReader.canRead() && pReader.peek() == '^') {
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(pReader);
        } else if (!pReader.canRead()) {
            throw ERROR_EXPECTED_DOUBLE.createWithContext(pReader);
        } else {
            boolean flag = isRelative(pReader);
            int i = pReader.getCursor();
            double d0 = pReader.canRead() && pReader.peek() != ' ' ? pReader.readDouble() : 0.0;
            String s = pReader.getString().substring(i, pReader.getCursor());
            if (flag && s.isEmpty()) {
                return new WorldCoordinate(true, 0.0);
            } else {
                if (!s.contains(".") && !flag && pCenterCorrect) {
                    d0 += 0.5;
                }

                return new WorldCoordinate(flag, d0);
            }
        }
    }

    public static WorldCoordinate parseInt(StringReader pReader) throws CommandSyntaxException {
        if (pReader.canRead() && pReader.peek() == '^') {
            throw Vec3Argument.ERROR_MIXED_TYPE.createWithContext(pReader);
        } else if (!pReader.canRead()) {
            throw ERROR_EXPECTED_INT.createWithContext(pReader);
        } else {
            boolean flag = isRelative(pReader);
            double d0;
            if (pReader.canRead() && pReader.peek() != ' ') {
                d0 = flag ? pReader.readDouble() : (double)pReader.readInt();
            } else {
                d0 = 0.0;
            }

            return new WorldCoordinate(flag, d0);
        }
    }

    public static boolean isRelative(StringReader pReader) {
        boolean flag;
        if (pReader.peek() == '~') {
            flag = true;
            pReader.skip();
        } else {
            flag = false;
        }

        return flag;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (!(pOther instanceof WorldCoordinate worldcoordinate)) {
            return false;
        } else {
            return this.relative != worldcoordinate.relative ? false : Double.compare(worldcoordinate.value, this.value) == 0;
        }
    }

    @Override
    public int hashCode() {
        int i = this.relative ? 1 : 0;
        long j = Double.doubleToLongBits(this.value);
        return 31 * i + (int)(j ^ j >>> 32);
    }

    public boolean isRelative() {
        return this.relative;
    }
}
