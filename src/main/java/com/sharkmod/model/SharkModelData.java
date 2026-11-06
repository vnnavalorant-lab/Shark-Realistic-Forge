package com.sharkmod.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Dados do tubarão convertidos do FBX original (malha low-poly + esqueleto + animações).
 * Esta classe NÃO depende de nenhuma classe do Minecraft (pode ser testada isoladamente).
 */
public final class SharkModelData {

    /** Uma animação já "assada" a 24 fps: para cada frame e osso, 3 floats de posição + 4 de quaternion. */
    public static final class Clip {
        public final String name;
        public final float fps;
        public final int frames;
        public final boolean loop;
        public final float[] data; // frames * boneCount * 7

        Clip(String name, float fps, int frames, boolean loop, float[] data) {
            this.name = name;
            this.fps = fps;
            this.frames = frames;
            this.loop = loop;
            this.data = data;
        }

        /** Duração em segundos. */
        public float duration() {
            return loop ? frames / fps : (frames - 1) / fps;
        }
    }

    public final int boneCount;
    public final int[] parent;
    /** Matriz 3x4 (linha a linha) do "Shark Rig" — pai constante do osso raiz. */
    public final float[] rig = new float[12];
    /** Por osso: inverse(bindPose) * matrizDaMalha, 3x4 linha a linha. */
    public final float[] invBind;
    public final float[] center = new float[3];
    public final float[] boundsMin = new float[3];
    public final float[] boundsMax = new float[3];

    public final int vertexCount;
    public final float[] pos;
    public final float[] normal;
    public final float[] uv;
    /** Influências (CSR): vértice i usa índices infStart[i] .. infStart[i+1]-1. */
    public final int[] infStart;
    public final int[] infBone;
    public final float[] infWeight;

    public final int triangleCount;
    public final int[] indices;

    private final Map<String, Clip> clips = new HashMap<>();

    public Clip clip(String name) {
        Clip c = clips.get(name);
        if (c == null) throw new IllegalArgumentException("Animação inexistente: " + name);
        return c;
    }

    private SharkModelData(ByteBuffer m, ByteBuffer a) {
        if (m.getInt() != 0x53484B31) throw new IllegalStateException("shark_model.bin inválido");
        boneCount = m.getInt();
        parent = new int[boneCount];
        for (int i = 0; i < boneCount; i++) parent[i] = m.getInt();
        float[] rig16 = new float[16];
        for (int i = 0; i < 16; i++) rig16[i] = m.getFloat();
        System.arraycopy(rig16, 0, rig, 0, 12);
        invBind = new float[boneCount * 12];
        for (int i = 0; i < invBind.length; i++) invBind[i] = m.getFloat();
        for (int i = 0; i < 3; i++) center[i] = m.getFloat();
        for (int i = 0; i < 3; i++) boundsMin[i] = m.getFloat();
        for (int i = 0; i < 3; i++) boundsMax[i] = m.getFloat();

        vertexCount = m.getInt();
        pos = new float[vertexCount * 3];
        normal = new float[vertexCount * 3];
        uv = new float[vertexCount * 2];
        infStart = new int[vertexCount + 1];
        int cap = vertexCount * 12;
        int[] bones = new int[cap];
        float[] weights = new float[cap];
        int n = 0;
        for (int i = 0; i < vertexCount; i++) {
            pos[i * 3] = m.getFloat();
            pos[i * 3 + 1] = m.getFloat();
            pos[i * 3 + 2] = m.getFloat();
            normal[i * 3] = m.getFloat();
            normal[i * 3 + 1] = m.getFloat();
            normal[i * 3 + 2] = m.getFloat();
            uv[i * 2] = m.getFloat();
            uv[i * 2 + 1] = m.getFloat();
            int count = m.get() & 0xFF;
            infStart[i] = n;
            if (n + count > bones.length) {
                bones = java.util.Arrays.copyOf(bones, bones.length * 2);
                weights = java.util.Arrays.copyOf(weights, weights.length * 2);
            }
            for (int k = 0; k < count; k++) {
                bones[n] = m.getShort() & 0xFFFF;
                weights[n] = m.getFloat();
                n++;
            }
        }
        infStart[vertexCount] = n;
        infBone = java.util.Arrays.copyOf(bones, n);
        infWeight = java.util.Arrays.copyOf(weights, n);

        triangleCount = m.getInt();
        indices = new int[triangleCount * 3];
        for (int i = 0; i < indices.length; i++) indices[i] = m.getShort() & 0xFFFF;

        if (a.getInt() != 0x53484B32) throw new IllegalStateException("shark_anim.bin inválido");
        int clipCount = a.getInt();
        for (int c = 0; c < clipCount; c++) {
            int nameLen = a.get() & 0xFF;
            byte[] nb = new byte[nameLen];
            a.get(nb);
            String name = new String(nb, StandardCharsets.UTF_8);
            float fps = a.getFloat();
            int frames = a.getInt();
            boolean loop = a.get() != 0;
            float[] data = new float[frames * boneCount * 7];
            for (int i = 0; i < data.length; i++) data[i] = a.getFloat();
            clips.put(name, new Clip(name, fps, frames, loop, data));
        }
    }

    public static SharkModelData load(InputStream model, InputStream anim) throws IOException {
        ByteBuffer m = ByteBuffer.wrap(model.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer a = ByteBuffer.wrap(anim.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        return new SharkModelData(m, a);
    }

    private static SharkModelData cached;

    /** Carrega (uma única vez) os arquivos que ficam dentro do .jar do mod. */
    public static synchronized SharkModelData get() {
        if (cached == null) {
            try (InputStream m = SharkModelData.class.getResourceAsStream("/assets/sharkmod/shark/shark_model.bin");
                 InputStream a = SharkModelData.class.getResourceAsStream("/assets/sharkmod/shark/shark_anim.bin")) {
                if (m == null || a == null) throw new IOException("arquivos do modelo do tubarão não encontrados no jar");
                cached = load(m, a);
            } catch (IOException e) {
                throw new RuntimeException("Falha ao carregar o modelo do tubarão", e);
            }
        }
        return cached;
    }
}
