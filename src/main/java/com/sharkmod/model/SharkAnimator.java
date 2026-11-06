package com.sharkmod.model;

/**
 * Máquina de estados de animação (roda a 20 ticks/s). Usa as 3 animações originais do FBX:
 *  - "swim"   (swimming_skeletal): nadando de verdade
 *  - "circle" (circling_skeletal): parado/rondando devagar (cross-fade com a swim)
 *  - "bite"   (bite_skeletal): mordida, tocada por cima das outras quando o tubarão ataca
 * Sem dependência do Minecraft.
 */
public final class SharkAnimator {
    public static final float TICK = 0.05f;
    private static final float WRAP = 4.0f; // múltiplo comum dos loops (swim = 2s, circle = 4s)

    private float time;
    private float rate = 1f;
    private float mix;            // 0 = swim, 1 = circle
    private float biteTime = -1f; // < 0 = sem mordida

    private float[] a, b;

    /** Chamar uma vez por tick. {@code speed} = blocos percorridos neste tick. */
    public void tick(boolean moving, float speed) {
        float targetMix = moving ? 0f : 1f;
        float step = TICK / 0.5f;
        float diff = targetMix - mix;
        mix += Math.max(-step, Math.min(step, diff));

        float targetRate = moving ? Math.max(0.8f, Math.min(2.2f, 0.8f + speed * 6f)) : 0.8f;
        rate += (targetRate - rate) * 0.15f;
        time = (time + TICK * rate) % WRAP;

        if (biteTime >= 0f) {
            biteTime += TICK;
            if (biteTime > 1.0f) biteTime = -1f;
        }
    }

    public void triggerBite() {
        biteTime = 0f;
    }

    public boolean isBiting() {
        return biteTime >= 0f;
    }

    /** Escreve em {@code pose} a pose final no instante tick + partial. */
    public void evaluate(SharkModelData d, SharkSkinner sk, float partial, float[] pose) {
        if (a == null || a.length != pose.length) {
            a = new float[pose.length];
            b = new float[pose.length];
        }
        float t = (time + partial * TICK * rate) % WRAP;
        sk.sample(d.clip("swim"), t, pose);
        if (mix > 0.001f) {
            sk.sample(d.clip("circle"), t, a);
            sk.blend(pose, a, mix, pose);
        }
        if (biteTime >= 0f) {
            float bt = biteTime + partial * TICK;
            float dur = 1.0f;
            float w = Math.min(Math.min(1f, bt / 0.08f), (dur - bt) / 0.25f);
            if (w > 0f) {
                sk.sample(d.clip("bite"), bt, b);
                sk.blend(pose, b, Math.min(1f, w), pose);
            }
        }
    }
}
