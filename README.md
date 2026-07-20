# Magik RPG

Um mod RPG completo, moderno e otimizado para **Minecraft Java Edition 1.20.1 + Forge (47.x)**, mantendo totalmente o estilo visual original do jogo. Compatível com singleplayer e servidores multiplayer.

![Forge](https://img.shields.io/badge/Forge-1.20.1--47.2.0-orange) ![Java](https://img.shields.io/badge/Java-17-blue)

## Conteúdo

| Sistema | Descrição |
|---|---|
| **Progressão** | XP RPG próprio (separado do XP vanilla), curva configurável, barra animada |
| **Atributos** | Vida, Força, Resistência, Agilidade, Precisão, Inteligência, Vel. de Mineração |
| **Mana** | Barra própria, regeneração natural, poções, consumo por magias |
| **Stamina** | Barra própria, regeneração com atraso, usada por dash e golpes |
| **5 Árvores** | Arcano, Espadachim, Arqueiro, Armamento Pesado, Defesa — 40 habilidades |
| **Classes livres** | Sem classe fixa; títulos por progressão (Aprendiz → Lenda) |
| **Arsenal** | 3 espadas, 3 armas pesadas, 6 arcos, 6 escudos 3D, 6 cajados 3D, armadura Arcanita |
| **Raridades** | Comum → Incomum → Raro → Épico → **Lendário** → **Mítico** |
| **Requisitos** | Nível e atributos mínimos por equipamento (tooltip colorido) |
| **HUD** | Vida/Mana/Stamina, nível+título, XP animado, 4 slots de habilidade com cooldown |

## Controles (padrão)

| Tecla | Ação |
|---|---|
| `K` | Menu do personagem (Status → Habilidades) |
| `Alt Esq.` | Dash (requer a habilidade Dash do Espadachim) |
| `Z` `X` `C` `V` | Slots de habilidade 1–4 |
| Scroll / arrastar | Zoom e pan na árvore de habilidades |
| `1`–`4` (na árvore) | Atribui a habilidade ativa sob o cursor ao slot |

## Como compilar

```bash
./gradlew build        # gera o jar em build/libs/
./gradlew runClient    # abre o cliente de desenvolvimento
```

Requer Java 17. O primeiro build baixa e decompila o Minecraft (demora alguns minutos).

## Configuração

- `config/magik-server.toml` — curva de XP, pontos por nível, fontes de XP, mana/stamina base, custo do dash.
- `config/magik-client.toml` — posições e visibilidade de cada elemento da HUD.
- `config/magik/balance.json` — **JSON de balanceamento**: custo de mana/stamina, cooldown e nível mínimo de cada habilidade + multiplicadores globais de XP e regeneração. Gerado completo no primeiro launch; aplicado no servidor (não trapaceável pelo cliente).
- `/magikrpg xp <n> | level <n> | reset` — comandos de administração (permissão 2).

## Arquitetura

```
com.magik
├── MagikMod            # bootstrap: registries, configs, rede
├── config/             # TOML (Forge) + balance.json (GSON)
├── player/             # capability PlayerRpg (dados), RpgStats (fórmulas),
│                       # RpgEvents (ciclo de vida), PlayerTitles
├── progression/        # fontes de XP, curva, comandos admin
├── combat/             # atributos → gameplay (dano, crit, mitigação, flechas)
├── skills/             # definição das árvores + execução server-side
├── network/            # SimpleChannel: syncs S2C + intents C2S
├── item/               # arsenal RPG (requisitos, raridades, passivas)
├── entity/             # MagicBoltEntity (projétil elemental dos cajados)
└── client/             # HUD, telas, keybinds, cache local (predição)
```

### Princípios

- **Servidor autoritativo**: o cliente só envia intenções (gastar ponto, desbloquear, conjurar); tudo é validado no servidor. Multiplayer seguro.
- **Predição no cliente**: mana/stamina regeneram localmente com as mesmas fórmulas e são corrigidas por syncs periódicos leves (10 em 10 ticks) — barras suaves sem tráfego pesado.
- **Modificadores transientes**: bônus de atributo usam `AttributeModifier` transiente reaplicado do zero a cada mudança — nunca acumulam nem vazam para o NBT.
- **Fórmulas centralizadas**: tudo em `RpgStats`, usadas por gameplay E interface — os números exibidos são sempre os reais.
- **Persistência**: capability serializada no NBT do jogador; progresso sobrevive a morte, relog e restart. Na morte, mana/stamina voltam ao máximo e cooldowns limpam.

### Como estender

- **Nova habilidade**: uma linha em `SkillTrees`, efeito em `SkillCasting` (ativa) ou no hook relevante (passiva), lang + ícone `textures/gui/skills/<id>.png`.
- **Novo item**: registrar em `ModItems` (classes prontas: `RpgSwordItem`, `RpgAxeItem`, `RpgBowItem`, `RpgShieldItem`, `StaffItem`), modelo + textura + receita + lang.
- **Nova raridade**: `ModRarities` via `Rarity.create`.

## Escopo desta versão

Sem mobs novos, sem chefes, sem IA customizada — o foco é o sistema RPG completo (progressão, atributos, mana, stamina, árvores, equipamentos, interface, multiplayer), preparado para expansões futuras.
