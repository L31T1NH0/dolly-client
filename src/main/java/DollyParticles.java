public final class DollyParticles {
    public static int ambientSamples() { return DollySettings.config.lowAnimationTick ? 100 : 1000; }
    public static int brightness(beb particle, float partialTicks) {
        return DollySettings.config.staticParticleColor ? 15728880 : particle.b(partialTicks);
    }
    public static int limit() { return DollySettings.config.particleLimit; }
    public static void ambient(bdb world, int x, int y, int z) {
        if (DollySettings.config.ambientParticles) world.b(x, y, z);
    }
}
