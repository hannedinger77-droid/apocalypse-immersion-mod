package com.apocalypseimmersion.client;
import com.mojang.blaze3d.vertex.*;

public class SafeTesselator {
    public static BufferBuilder begin(VertexFormat.Mode mode, VertexFormat fmt) {
        Tesselator tes = Tesselator.getInstance();
        BufferBuilder buf = tes.getBuilder();
        if (buf.building()) { try { tes.end(); } catch (Throwable ignored) {} }
        buf.begin(mode, fmt);
        return buf;
    }
    public static void end() {
        Tesselator tes = Tesselator.getInstance();
        if (tes.getBuilder().building()) { try { tes.end(); } catch (Throwable ignored) {} }
    }
}