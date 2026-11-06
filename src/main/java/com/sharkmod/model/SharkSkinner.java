package com.sharkmod.model;

/**
 * Calcula a pose do esqueleto e deforma (skinning por CPU) a malha do tubarão.
 * Uma "pose" é um float[boneCount * 7]: por osso, x y z (posição local) + x y z w (quaternion local).
 * Sem dependência do Minecraft.
 */
public final class SharkSkinner {
    private final SharkModelData d;
    private final int bones;
    private final float[] world;
    private final float[] skin;
    private final float[] local = new float[12];

    /** Posições deformadas (x,y,z por vértice) em unidades do modelo (≈ centímetros). */
    public final float[] outPos;
    /** Normais deformadas e normalizadas. */
    public final float[] outNormal;

    public SharkSkinner(SharkModelData data) {
        this.d = data;
        this.bones = data.boneCount;
        this.world = new float[bones * 12];
        this.skin = new float[bones * 12];
        this.outPos = new float[data.vertexCount * 3];
        this.outNormal = new float[data.vertexCount * 3];
    }

    public float[] newPose() {
        return new float[bones * 7];
    }

    /** Lê a animação no instante {@code timeSec}, interpolando entre frames. */
    public void sample(SharkModelData.Clip c, float timeSec, float[] pose) {
        float f = timeSec * c.fps;
        int f0, f1;
        float t;
        if (c.loop) {
            float cyc = f % c.frames;
            if (cyc < 0) cyc += c.frames;
            f0 = (int) cyc;
            if (f0 >= c.frames) f0 = c.frames - 1;
            t = cyc - f0;
            f1 = (f0 + 1) % c.frames;
        } else {
            if (f < 0) f = 0;
            if (f > c.frames - 1) f = c.frames - 1;
            f0 = (int) f;
            t = f - f0;
            f1 = Math.min(f0 + 1, c.frames - 1);
        }
        float[] data = c.data;
        for (int b = 0; b < bones; b++) {
            int i0 = (f0 * bones + b) * 7;
            int i1 = (f1 * bones + b) * 7;
            int o = b * 7;
            for (int k = 0; k < 3; k++) pose[o + k] = data[i0 + k] + (data[i1 + k] - data[i0 + k]) * t;
            nlerp(data, i0 + 3, data, i1 + 3, t, pose, o + 3);
        }
    }

    /** out = a * (1-w) + b * w (posição linear, rotação nlerp). Pode usar out == a. */
    public void blend(float[] a, float[] b, float w, float[] out) {
        for (int bi = 0; bi < bones; bi++) {
            int o = bi * 7;
            for (int k = 0; k < 3; k++) out[o + k] = a[o + k] + (b[o + k] - a[o + k]) * w;
            nlerp(a, o + 3, b, o + 3, w, out, o + 3);
        }
    }

    private static void nlerp(float[] a, int ao, float[] b, int bo, float t, float[] out, int oo) {
        float ax = a[ao], ay = a[ao + 1], az = a[ao + 2], aw = a[ao + 3];
        float bx = b[bo], by = b[bo + 1], bz = b[bo + 2], bw = b[bo + 3];
        if (ax * bx + ay * by + az * bz + aw * bw < 0f) {
            bx = -bx; by = -by; bz = -bz; bw = -bw;
        }
        float x = ax + (bx - ax) * t;
        float y = ay + (by - ay) * t;
        float z = az + (bz - az) * t;
        float w = aw + (bw - aw) * t;
        float len = (float) Math.sqrt(x * x + y * y + z * z + w * w);
        if (len < 1e-8f) { x = 0; y = 0; z = 0; w = 1; len = 1; }
        out[oo] = x / len;
        out[oo + 1] = y / len;
        out[oo + 2] = z / len;
        out[oo + 3] = w / len;
    }

    /** Matriz afim 3x4 (linha a linha): o = a * b. Seguro contra aliasing. */
    private static void mul34(float[] a, int ao, float[] b, int bo, float[] o, int oo) {
        float[] r = new float[12];
        for (int i = 0; i < 3; i++) {
            float a0 = a[ao + i * 4], a1 = a[ao + i * 4 + 1], a2 = a[ao + i * 4 + 2], a3 = a[ao + i * 4 + 3];
            for (int j = 0; j < 3; j++) {
                r[i * 4 + j] = a0 * b[bo + j] + a1 * b[bo + 4 + j] + a2 * b[bo + 8 + j];
            }
            r[i * 4 + 3] = a0 * b[bo + 3] + a1 * b[bo + 7] + a2 * b[bo + 11] + a3;
        }
        System.arraycopy(r, 0, o, oo, 12);
    }

    /** Calcula as matrizes de skinning de todos os ossos para a pose dada. */
    public void solve(float[] pose) {
        for (int b = 0; b < bones; b++) {
            int o = b * 7;
            float x = pose[o + 3], y = pose[o + 4], z = pose[o + 5], w = pose[o + 6];
            local[0] = 1 - 2 * (y * y + z * z);
            local[1] = 2 * (x * y - z * w);
            local[2] = 2 * (x * z + y * w);
            local[3] = pose[o];
            local[4] = 2 * (x * y + z * w);
            local[5] = 1 - 2 * (x * x + z * z);
            local[6] = 2 * (y * z - x * w);
            local[7] = pose[o + 1];
            local[8] = 2 * (x * z - y * w);
            local[9] = 2 * (y * z + x * w);
            local[10] = 1 - 2 * (x * x + y * y);
            local[11] = pose[o + 2];
            int p = d.parent[b];
            if (p < 0) mul34(d.rig, 0, local, 0, world, b * 12);
            else mul34(world, p * 12, local, 0, world, b * 12);
            mul34(world, b * 12, d.invBind, b * 12, skin, b * 12);
        }
    }

    /** Deforma todos os vértices usando as matrizes calculadas por {@link #solve}. */
    public void skinMesh() {
        for (int i = 0; i < d.vertexCount; i++) {
            float px = d.pos[i * 3], py = d.pos[i * 3 + 1], pz = d.pos[i * 3 + 2];
            float nx = d.normal[i * 3], ny = d.normal[i * 3 + 1], nz = d.normal[i * 3 + 2];
            float ox = 0, oy = 0, oz = 0, mx = 0, my = 0, mz = 0;
            for (int k = d.infStart[i]; k < d.infStart[i + 1]; k++) {
                int s = d.infBone[k] * 12;
                float w = d.infWeight[k];
                ox += w * (skin[s] * px + skin[s + 1] * py + skin[s + 2] * pz + skin[s + 3]);
                oy += w * (skin[s + 4] * px + skin[s + 5] * py + skin[s + 6] * pz + skin[s + 7]);
                oz += w * (skin[s + 8] * px + skin[s + 9] * py + skin[s + 10] * pz + skin[s + 11]);
                mx += w * (skin[s] * nx + skin[s + 1] * ny + skin[s + 2] * nz);
                my += w * (skin[s + 4] * nx + skin[s + 5] * ny + skin[s + 6] * nz);
                mz += w * (skin[s + 8] * nx + skin[s + 9] * ny + skin[s + 10] * nz);
            }
            float len = (float) Math.sqrt(mx * mx + my * my + mz * mz);
            if (len < 1e-8f) { mx = 0; my = 1; mz = 0; len = 1; }
            outPos[i * 3] = ox;
            outPos[i * 3 + 1] = oy;
            outPos[i * 3 + 2] = oz;
            outNormal[i * 3] = mx / len;
            outNormal[i * 3 + 1] = my / len;
            outNormal[i * 3 + 2] = mz / len;
        }
    }
}
