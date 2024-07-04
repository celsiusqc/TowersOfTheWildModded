package net.minecraft.util;

import java.io.DataOutput;
import java.io.IOException;

public class DelegateDataOutput implements DataOutput {
    private final DataOutput parent;

    public DelegateDataOutput(DataOutput pParent) {
        this.parent = pParent;
    }

    @Override
    public void write(int pValue) throws IOException {
        this.parent.write(pValue);
    }

    @Override
    public void write(byte[] pData) throws IOException {
        this.parent.write(pData);
    }

    @Override
    public void write(byte[] pData, int pOffset, int pLength) throws IOException {
        this.parent.write(pData, pOffset, pLength);
    }

    @Override
    public void writeBoolean(boolean pValue) throws IOException {
        this.parent.writeBoolean(pValue);
    }

    @Override
    public void writeByte(int pValue) throws IOException {
        this.parent.writeByte(pValue);
    }

    @Override
    public void writeShort(int pValue) throws IOException {
        this.parent.writeShort(pValue);
    }

    @Override
    public void writeChar(int pValue) throws IOException {
        this.parent.writeChar(pValue);
    }

    @Override
    public void writeInt(int pValue) throws IOException {
        this.parent.writeInt(pValue);
    }

    @Override
    public void writeLong(long pValue) throws IOException {
        this.parent.writeLong(pValue);
    }

    @Override
    public void writeFloat(float pValue) throws IOException {
        this.parent.writeFloat(pValue);
    }

    @Override
    public void writeDouble(double pValue) throws IOException {
        this.parent.writeDouble(pValue);
    }

    @Override
    public void writeBytes(String pValue) throws IOException {
        this.parent.writeBytes(pValue);
    }

    @Override
    public void writeChars(String pValue) throws IOException {
        this.parent.writeChars(pValue);
    }

    @Override
    public void writeUTF(String pValue) throws IOException {
        this.parent.writeUTF(pValue);
    }
}
