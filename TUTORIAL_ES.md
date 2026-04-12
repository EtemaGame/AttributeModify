# Tutorial Paso a Paso: AttributeModify 1.20.1 y NeoAttributeModify 1.21.1

Este tutorial esta basado en el comportamiento actual del parser y los handlers de ambos proyectos.
La estructura general del datapack es la misma en las dos versiones, pero hay diferencias reales que conviene documentar por separado.

Los archivos JSON siempre van en:

```text
data/<namespace>/item_attributes/<archivo>.json
```

Puedes repartir tus reglas en varios archivos. El mod las fusiona al cargar el datapack.

## 1.20.1 - Forge - AttributeModify

### Paso 1. Crear el datapack

En singleplayer:

```text
.minecraft/saves/<TuMundo>/datapacks/<tu_pack>
```

En servidor dedicado:

```text
<servidor>/world/datapacks/<tu_pack>
```

Crea este `pack.mcmeta`:

```json
{
  "pack": {
    "description": "Tutorial AttributeModify 1.20.1",
    "pack_format": 15
  }
}
```

Estructura recomendada:

```text
<tu_pack>/
  pack.mcmeta
  data/
    tutorial/
      item_attributes/
        armor.json
        tools.json
        quality.json
```

### Paso 2. Crear tu primera regla

En `data/tutorial/item_attributes/armor.json`:

```json
{
  "minecraft:diamond_chestplate": {
    "attribute": "minecraft:generic.luck",
    "action": "add",
    "amount": 1.0,
    "operation": "add_value"
  }
}
```

Esta forma corta funciona en `1.20.1`.
El mod intenta detectar el slot automaticamente.

### Paso 3. Usar slots explicitos

Si quieres control total, usa `equipment_slots`:

```json
{
  "minecraft:diamond_sword": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "modify",
          "amount": 10.0,
          "operation": "add_value"
        },
        {
          "attribute": "minecraft:generic.attack_speed",
          "action": "remove"
        }
      ]
    }
  }
}
```

Usa `modify` cuando quieras reemplazar el atributo base del item en ese slot.
Usa `remove` cuando quieras quitar ese atributo del item en ese slot.

### Paso 4. Aplicar una regla a un tag entero

```json
{
  "#minecraft:swords": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_knockback",
          "action": "add",
          "amount": 0.5,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

Esto sirve para afectar grupos completos de items, incluidos items modded si entran en el tag.

### Paso 5. Crear una regla condicional por NBT

`1.20.1` evalua el NBT del item.

```json
{
  "minecraft:iron_pickaxe": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "add",
          "amount": 3.0,
          "operation": "add_value",
          "nbt": {
            "path": "quality",
            "operator": "equals",
            "value": "mythic"
          }
        }
      ]
    }
  }
}
```

Tambien puedes usar el alias de compatibilidad:

```json
{
  "nbt": {
    "path": "component:apotheosis:rarity",
    "operator": "equals",
    "value": "apotheosis:mythic"
  }
}
```

En `1.20.1` ese alias solo esta implementado para mapear a `affix_data.rarity`.
No es un lector generico de components.

### Paso 6. Usar quality_system

```json
{
  "minecraft:iron_sword": {
    "quality_system": {
      "tag_path": "quality",
      "triggers": ["craft", "loot", "villager_trade", "apotheosis_sync"],
      "levels": [
        { "value": "common", "weight": 60 },
        { "value": "rare", "weight": 30 },
        { "value": "mythic", "weight": 10 }
      ]
    }
  }
}
```

Esto escribe la calidad dentro del NBT del item en la ruta `quality`.
Luego puedes reutilizar ese valor en otras reglas con `nbt.path`.

### Paso 7. Cambiar mineria

```json
{
  "minecraft:iron_pickaxe": {
    "mining": [
      {
        "speed": 12.0,
        "tier": "diamond",
        "nbt": {
          "path": "quality",
          "operator": "equals",
          "value": "mythic"
        }
      },
      {
        "speed": 8.0,
        "tier": "iron"
      }
    ]
  }
}
```

La primera entrada que haga match es la que gana.

### Paso 8. Sobrescribir durabilidad (Virtual Durability)

En `1.20.1`, AttributeModify utiliza un sistema de **Durabilidad Virtual** para los objetos que ya tienen durabilidad (vainilla o mods). Esto significa que no cambiamos el valor global del Item, sino que cada herramienta individual sabe cuál es su nuevo máximo.

```json
{
  "minecraft:diamond_pickaxe": {
    "durability": 3000
  }
}
```

**Ventajas del sistema virtual:**

- **Compatibilidad**: No interfiere con otros mods que lean el valor base del Item.
- **Escalado automático**: Si cambias el límite de 3000 a 5000, las herramientas que ya existan en tu mundo escalarán su desgaste automáticamente para mantener la proporción.
- **Persistencia**: El máximo aplicado se guarda en el NBT del objeto (`attributemodify.vdur.applied_max`).

#### Gatillos (Triggers) para objetos SIN durabilidad

Si quieres que un objeto que normalmente NO tiene durabilidad (como un Palo) la tenga, AttributeModify usará **Custom Durability**. Para estos casos, debes definir qué acciones rompen el objeto:

```json
{
  "minecraft:stick": {
    "durability": 100,
    "durability_triggers": ["melee_hit", "block_break"]
  }
}
```

Gatillos soportados:

- `melee_hit`: Al golpear una entidad.
- `block_break`: Al romper un bloque.
- `right_click`: Al usar el objeto con click derecho.

### Paso 9. Curios

Si tienes Curios instalado, puedes declarar slots explicitos:

```json
{
  "mymod:magic_ring": {
    "curios_slots": {
      "ring": [
        {
          "attribute": "minecraft:generic.max_health",
          "action": "add",
          "amount": 4.0,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

### Paso 10. Recargar y probar

1. Entra al mundo o servidor.
2. Ejecuta `/reload`.
3. Vuelve a coger el item o vuelve a fabricarlo si estas probando `quality_system`.
4. Revisa tooltip, velocidad de mineria o durabilidad segun el caso.

### Limitaciones de 1.20.1

- `equipment_slots` solo acepta `mainhand`, `offhand`, `head`, `chest`, `legs` y `feet`.
- `body` no esta implementado en esta rama.
- La forma corta con `attribute` si existe y funciona solo aqui.
- El bloque `nbt` lee NBT del item. El prefijo `component:` no es generico; solo hay compatibilidad especial para `component:apotheosis:rarity`.
- `durability_triggers` solo existe en esta rama.
- `durability_triggers` tiene sentido en modo custom, que en la practica apunta a items no damageables y con stack size 1.
- `modify` no crea atributos nuevos. Si el item no tenia ese atributo en ese slot, no hace nada.
- `tier` solo acepta nombres vanilla: `wood`, `stone`, `iron`, `diamond`, `netherite`, `gold`.
- El autodetect de slot solo cubre armaduras, `SwordItem`, `TieredItem`, `TridentItem` y algunos tags comunes de Curios como `ring`, `necklace`, `belt` y `charm`.
- En `mining`, gana la primera regla que haga match.

## 1.21.1 - NeoForge - NeoAttributeModify

### Paso 1. Crear el datapack

La ubicacion del datapack es la misma, pero `pack_format` cambia:

```json
{
  "pack": {
    "description": "Tutorial NeoAttributeModify 1.21.1",
    "pack_format": 34
  }
}
```

### Paso 2. Crear tu primera regla

En `1.21.1` usa `attributes` o `equipment_slots`.
La forma corta con una sola clave `attribute` ya no esta parseada en esta rama.

```json
{
  "minecraft:diamond_boots": {
    "attributes": [
      {
        "attribute": "minecraft:generic.armor",
        "action": "add",
        "amount": 1.0,
        "operation": "add_value"
      },
      {
        "attribute": "minecraft:generic.movement_speed",
        "action": "add",
        "amount": 0.03,
        "operation": "add_multiplied_total"
      }
    ]
  }
}
```

### Paso 3. Usar slots explicitos

```json
{
  "minecraft:diamond_sword": {
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "modify",
          "amount": 10.0,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

En esta rama tambien existe el slot `body`.

### Paso 4. Crear una regla condicional

`1.21.1` trabaja con `minecraft:custom_data` para sus comprobaciones condicionales.
La forma mas segura es escribir primero un valor ahi y luego leerlo desde `nbt.path`.

```json
{
  "minecraft:iron_sword": {
    "quality_system": {
      "tag_path": "quality",
      "triggers": ["craft", "loot", "villager_trade"],
      "levels": [
        { "value": "common", "weight": 60 },
        { "value": "rare", "weight": 30 },
        { "value": "mythic", "weight": 10 }
      ]
    },
    "equipment_slots": {
      "mainhand": [
        {
          "attribute": "minecraft:generic.attack_damage",
          "action": "add",
          "amount": 5.0,
          "operation": "add_value",
          "nbt": {
            "path": "quality",
            "operator": "equals",
            "value": "mythic"
          }
        }
      ]
    }
  }
}
```

Recomendacion importante:
define siempre `tag_path` de forma explicita si quieres packs faciles de portar.

### Paso 5. Cambiar mineria

```json
{
  "minecraft:iron_pickaxe": {
    "mining": [
      {
        "speed": 12.0,
        "tier": "diamond",
        "nbt": {
          "path": "quality",
          "operator": "equals",
          "value": "mythic"
        }
      },
      {
        "speed": 8.0,
        "tier": "iron"
      }
    ]
  }
}
```

Igual que en `1.20.1`, la primera regla valida es la que se aplica.

### Paso 6. Cambiar durabilidad

```json
{
  "minecraft:diamond_pickaxe": {
    "durability": 3000
  }
}
```

En esta rama no hay `durability_triggers` en el parser del datapack.
Si defines `durability`, el mod maneja la logica interna de desgaste segun sus eventos.

### Paso 7. Curios

Curios sigue siendo opcional. Si esta instalado:

```json
{
  "mymod:magic_ring": {
    "curios_slots": {
      "ring": [
        {
          "attribute": "minecraft:generic.max_health",
          "action": "add",
          "amount": 4.0,
          "operation": "add_value"
        }
      ]
    }
  }
}
```

### Paso 8. Recargar y probar

1. Ejecuta `/reload`.
2. Prueba el item otra vez.
3. Si usas `quality_system`, fabrica o consigue un item nuevo para forzar el roll.
4. Si usas `nbt.path`, comprueba que el dato realmente viva en `custom_data`.

### Limitaciones de 1.21.1

- La forma corta con una sola clave `attribute` no esta soportada. Usa `attributes` o `equipment_slots`.
- `equipment_slots` acepta `body` ademas de los slots clasicos.
- El bloque `nbt` compara contra `minecraft:custom_data`, no contra todos los data components vanilla del item.
- `durability_triggers` no esta implementado en el datapack de esta rama.
- `quality_system.tag_path` usa `Quality` como valor por defecto si no lo defines. Para evitar diferencias con `1.20.1`, es mejor declararlo siempre a mano.
- No hay integracion equivalente a `apotheosis_sync` ni un alias especial como `component:apotheosis:rarity`.
- `modify` no crea atributos nuevos. Solo sustituye un atributo que ya exista en ese slot.
- `tier` solo acepta nombres vanilla: `wood`, `stone`, `iron`, `diamond`, `netherite`, `gold`.
- El autodetect de slot sigue siendo limitado a armaduras, `SwordItem`, `TieredItem`, `TridentItem` y algunos tags comunes de Curios.
- En `mining`, gana la primera regla que haga match.

## Migracion rapida de 1.20.1 a 1.21.1

1. Cambia `pack_format` de `15` a `34`.
2. Reemplaza la forma corta con `attribute` por `attributes` o `equipment_slots`.
3. Define siempre `quality_system.tag_path` para no depender de defaults distintos.
4. Si una condicion dependia de NBT arbitrario, confirma que ese dato ahora este dentro de `minecraft:custom_data`.
5. Si usabas `durability_triggers`, replantea esa config porque en `1.21.1` no se parsea.
6. Si dependias de `apotheosis_sync` o de `component:apotheosis:rarity`, deja una version separada para `1.20.1`.

## Consejos practicos

- Si dos mods tocan el mismo atributo, usa `modifier_id` para mantener una identidad estable del modificador.
- Si una regla no parece funcionar, prueba primero con `equipment_slots` antes de depender del autodetect.
- Si un `modify` no hace nada, normalmente significa que el item original no tenia ese atributo en ese slot.
- Si un `tier` no funciona, revisa que sea uno de los nombres vanilla soportados.
