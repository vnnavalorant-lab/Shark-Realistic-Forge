# Shark Mod (Forge 1.20.1)

Tubarões realistas feitos a partir do `Realistic Shark.fbx`, em duas versões:

| Entidade | ID | Tamanho | Comportamento |
|---|---|---|---|
| Tubarão adulto (dad) | `sharkmod:shark` | ~3,7 blocos de comprimento | ataca peixes e quem o machucar |
| Tubarão filhote (baby) | `sharkmod:baby_shark` | 37,5% do adulto (~1,4 bloco) | pacífico, foge quando machucado |

Os dois usam o **mesmo modelo, a mesma textura e as 3 animações originais do FBX**:

- `swimming_skeletal` -> animação **swim** (quando está se movendo; a velocidade da animação acompanha a velocidade do tubarão)
- `circling_skeletal` -> animação **circle** (quando está parado/rondando; faz transição suave com a swim)
- `bite_skeletal`     -> animação **bite** (toca por cima das outras toda vez que ele ataca)

Comandos para testar:

    /summon sharkmod:shark
    /summon sharkmod:baby_shark

Também há dois ovos de spawn na aba "Spawn Eggs" do criativo. Spawn natural: adultos (raros) em oceanos profundos, filhotes em oceanos (definido por biome modifiers em `data/sharkmod/forge/biome_modifier/`).

## Como compilar

Precisa de JDK 17.

    gradle wrapper --gradle-version 8.1.1   # só na primeira vez, se não tiver o ./gradlew
    ./gradlew build                          # o .jar fica em build/libs/
    ./gradlew runClient                      # abre o Minecraft de teste

(Se preferir, abra a pasta no IntelliJ IDEA como projeto Gradle.)
Se alguma versão em `gradle.properties` não for encontrada, pegue as atuais para 1.20.1 em https://files.minecraftforge.net

O projeto usa ForgeGradle 6 com os mapeamentos oficiais da Mojang (antes eram Yarn, no Fabric).

## Como funciona

O FBX tem uma malha com esqueleto de 31 ossos (não é um modelo de cubos), então não dá para usar o sistema de modelos do Minecraft.
O FBX foi convertido para 2 arquivos binários em `src/main/resources/assets/sharkmod/shark/`:

- `shark_model.bin`: a malha low-poly (1693 vértices / 2542 triângulos), pesos de skinning e esqueleto
- `shark_anim.bin`: as 3 animações já amostradas a 24 fps

O código em `com.sharkmod.model` (sem dependência do Minecraft) faz o skinning por CPU, e `SharkRenderer` desenha o resultado.
A textura original (1920x1920 JPEG) virou PNG 1024x1024 em `textures/entity/shark.png`.

## Ajustes rápidos

- Tamanho do adulto: `MODEL_SCALE` em `client/SharkRenderer.java`; do filhote: `BABY_SIZE` em `client/SharkModClient.java`
- Hitbox: `EntityDimensions.fixed(...)` em `entity/ModEntities.java`
- Vida, dano e velocidade: `createDadAttributes` / `createBabyAttributes` em `entity/SharkEntity.java`
- Frequência de spawn: `weight`/`minCount`/`maxCount` nos JSONs de `data/sharkmod/forge/biome_modifier/`
- Distância de rastreamento: `clientTrackingRange(...)` em `entity/ModEntities.java` (em chunks)

## Observações

- Os dentes do FBX (malhas de ~45 mil triângulos) ficaram de fora para manter o desempenho; a malha low-poly já inclui boca e olhos.
- Os sons usam os do golfinho do jogo (troque em `getHurtSound` / `getDeathSound`).
- Confira a licença do modelo original (`Realistic Shark`) e dê o crédito ao autor, se ela exigir.
