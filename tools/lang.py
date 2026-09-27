"""English and Russian translations for Dunes & Relics."""
import json
import os

# key suffix -> (English, Russian)
BLOCKS = {
    "limestone": ("Limestone", "Известняк"),
    "limestone_stairs": ("Limestone Stairs", "Известняковые ступеньки"),
    "limestone_slab": ("Limestone Slab", "Известняковая плита"),
    "limestone_wall": ("Limestone Wall", "Известняковая ограда"),
    "polished_limestone": ("Polished Limestone", "Полированный известняк"),
    "polished_limestone_stairs": ("Polished Limestone Stairs", "Ступеньки из полированного известняка"),
    "polished_limestone_slab": ("Polished Limestone Slab", "Плита из полированного известняка"),
    "polished_limestone_wall": ("Polished Limestone Wall", "Ограда из полированного известняка"),
    "limestone_bricks": ("Limestone Bricks", "Известняковые кирпичи"),
    "limestone_brick_stairs": ("Limestone Brick Stairs", "Ступеньки из известняковых кирпичей"),
    "limestone_brick_slab": ("Limestone Brick Slab", "Плита из известняковых кирпичей"),
    "limestone_brick_wall": ("Limestone Brick Wall", "Ограда из известняковых кирпичей"),
    "cracked_limestone_bricks": ("Cracked Limestone Bricks", "Потрескавшиеся известняковые кирпичи"),
    "chiseled_limestone_bricks": ("Chiseled Limestone Bricks", "Резные известняковые кирпичи"),
    "limestone_pillar": ("Limestone Pillar", "Известняковая колонна"),
    "gilded_limestone": ("Gilded Limestone", "Позолоченный известняк"),
    "amber_ore": ("Amber Ore", "Янтарная руда"),
    "amber_block": ("Block of Amber", "Янтарный блок"),
    "amber_lamp": ("Amber Lamp", "Янтарная лампа"),
    "bronze_block": ("Block of Bronze", "Бронзовый блок"),
    "palm_log": ("Palm Log", "Пальмовое бревно"),
    "palm_wood": ("Palm Wood", "Пальмовая древесина"),
    "stripped_palm_log": ("Stripped Palm Log", "Обтёсанное пальмовое бревно"),
    "stripped_palm_wood": ("Stripped Palm Wood", "Обтёсанная пальмовая древесина"),
    "palm_planks": ("Palm Planks", "Пальмовые доски"),
    "palm_stairs": ("Palm Stairs", "Пальмовые ступеньки"),
    "palm_slab": ("Palm Slab", "Пальмовая плита"),
    "palm_fence": ("Palm Fence", "Пальмовый забор"),
    "palm_fence_gate": ("Palm Fence Gate", "Пальмовая калитка"),
    "palm_door": ("Palm Door", "Пальмовая дверь"),
    "palm_trapdoor": ("Palm Trapdoor", "Пальмовый люк"),
    "palm_button": ("Palm Button", "Пальмовая кнопка"),
    "palm_pressure_plate": ("Palm Pressure Plate", "Пальмовая нажимная плита"),
    "palm_leaves": ("Palm Leaves", "Пальмовые листья"),
    "palm_sapling": ("Palm Sapling", "Саженец пальмы"),
    "potted_palm_sapling": ("Potted Palm Sapling", "Саженец пальмы в горшке"),
    "dune_grass": ("Dune Grass", "Дюнная трава"),
    "desert_rose": ("Desert Rose", "Роза пустыни"),
    "potted_desert_rose": ("Potted Desert Rose", "Роза пустыни в горшке"),
    "aloe_vera": ("Aloe Vera", "Алоэ вера"),
    "cattail": ("Cattail", "Рогоз"),
    "quicksand": ("Quicksand", "Зыбучий песок"),
    "ancient_urn": ("Ancient Urn", "Древняя урна"),
    "sarcophagus": ("Sarcophagus", "Саркофаг"),
}

ITEMS = {
    "amber": ("Amber", "Янтарь"),
    "bronze_ingot": ("Bronze Ingot", "Бронзовый слиток"),
    "bronze_nugget": ("Bronze Nugget", "Бронзовый самородок"),
    "linen": ("Linen", "Льняная ткань"),
    "scorpion_stinger": ("Scorpion Stinger", "Жало скорпиона"),
    "vulture_feather": ("Vulture Feather", "Перо стервятника"),
    "bronze_khopesh": ("Bronze Khopesh", "Бронзовый хопеш"),
    "bronze_shovel": ("Bronze Shovel", "Бронзовая лопата"),
    "bronze_pickaxe": ("Bronze Pickaxe", "Бронзовая кирка"),
    "bronze_axe": ("Bronze Axe", "Бронзовый топор"),
    "bronze_hoe": ("Bronze Hoe", "Бронзовая мотыга"),
    "bronze_helmet": ("Bronze Helmet", "Бронзовый шлем"),
    "bronze_chestplate": ("Bronze Chestplate", "Бронзовый нагрудник"),
    "bronze_leggings": ("Bronze Leggings", "Бронзовые поножи"),
    "bronze_boots": ("Bronze Boots", "Бронзовые ботинки"),
    "amber_goggles": ("Amber Goggles", "Янтарные очки"),
    "dates": ("Dates", "Финики"),
    "honeyed_dates": ("Honeyed Dates", "Финики в меду"),
    "flatbread": ("Flatbread", "Лепёшка"),
    "aloe_leaf": ("Aloe Leaf", "Лист алоэ"),
    "bandage": ("Bandage", "Бинт"),
    "pharaoh_armor_trim_smithing_template": ("Smithing Template", "Кузнечный шаблон"),
    "scepter_of_sands": ("Scepter of Sands", "Скипетр песков"),
    "mummy_spawn_egg": ("Mummy Spawn Egg", "Яйцо призыва мумии"),
    "pharaoh_spawn_egg": ("Pharaoh Spawn Egg", "Яйцо призыва фараона"),
    "scorpion_spawn_egg": ("Scorpion Spawn Egg", "Яйцо призыва скорпиона"),
    "scarab_spawn_egg": ("Scarab Spawn Egg", "Яйцо призыва скарабея"),
    "meerkat_spawn_egg": ("Meerkat Spawn Egg", "Яйцо призыва суриката"),
    "vulture_spawn_egg": ("Vulture Spawn Egg", "Яйцо призыва стервятника"),
}

ENTITIES = {
    "mummy": ("Mummy", "Мумия"),
    "pharaoh": ("Pharaoh", "Фараон"),
    "scorpion": ("Scorpion", "Скорпион"),
    "scarab": ("Scarab", "Скарабей"),
    "meerkat": ("Meerkat", "Сурикат"),
    "vulture": ("Vulture", "Стервятник"),
}

ADVANCEMENTS = {
    "root": (("Dunes & Relics", "The desert remembers everything the sand has buried"),
             ("Пески и Реликвии", "Пустыня помнит всё, что погребено под песком")),
    "ancient_dunes": (("Sea of Sand", "Enter the Ancient Dunes"),
                      ("Море песка", "Побывайте в Древних дюнах")),
    "quicksand": (("Sinking Feeling", "Step into quicksand. Hold jump to climb out!"),
                  ("Засасывает", "Угодите в зыбучий песок. Удерживайте прыжок, чтобы выбраться!")),
    "mummy": (("It's a Wrap", "Defeat a Mummy"), ("Размотано", "Победите мумию")),
    "meerkats": (("On the Lookout", "Breed two Meerkats with Dates"),
                 ("На страже", "Разведите сурикатов с помощью фиников")),
    "ruins": (("Relic Hunter", "Loot a chest in the ancient ruins"),
              ("Охотник за реликвиями", "Обыщите сундук в древних руинах")),
    "tomb": (("Tomb Raider", "Find the tomb hidden beneath the ruins and loot its treasures"),
             ("Расхититель гробниц", "Найдите гробницу под руинами и заберите её сокровища")),
    "pharaoh": (("Curse of the Pharaoh", "Awaken the Pharaoh from his sarcophagus... and defeat him"),
                ("Проклятие фараона", "Пробудите фараона в его саркофаге... и одолейте его")),
    "bronze": (("Bronze Age", "Alloy copper with amber into a Bronze Ingot"),
               ("Бронзовый век", "Сплавьте медь с янтарём в бронзовый слиток")),
    "goggles": (("Eye of the Storm", "Get Amber Goggles to see through sandstorms"),
                ("Глаз бури", "Получите янтарные очки, чтобы видеть сквозь песчаную бурю")),
}

EXTRA = {
    "itemGroup.dunesrelics": ("Dunes & Relics", "Пески и Реликвии"),
    "biome.dunesrelics.ancient_dunes": ("Ancient Dunes", "Древние дюны"),
    "trim_pattern.dunesrelics.pharaoh": ("Pharaoh Armor Trim", "Отделка «Фараон»"),
    "trim_material.dunesrelics.amber": ("Amber Material", "Янтарный материал"),
    "death.attack.dunesrelics.quicksand": ("%1$s sank into quicksand", "%1$s утонул(а) в зыбучем песке"),
    "death.attack.dunesrelics.quicksand.player": ("%1$s sank into quicksand while fighting %2$s",
                                                  "%1$s утонул(а) в зыбучем песке, сражаясь с %2$s"),
    "block.dunesrelics.sarcophagus.peaceful": ("The seal holds. Nothing stirs within... in peace.",
                                               "Печать держится. Внутри ничто не шевелится... в мирном режиме."),
    "entity.dunesrelics.pharaoh.enraged": ("The Pharaoh is enraged!", "Фараон пришёл в ярость!"),
    "item.dunesrelics.scepter_of_sands.storm": ("A sandstorm is rising...", "Поднимается песчаная буря..."),
    "item.dunesrelics.scepter_of_sands.calm": ("The sands settle.", "Пески успокаиваются."),
    "item.dunesrelics.scepter_of_sands.desc": ("Use: blast a cone of scouring sand",
                                               "Использование: выпускает конус режущего песка"),
    "item.dunesrelics.scepter_of_sands.desc2": ("Sneak + Use: summon or calm a sandstorm",
                                                "Присесть + использовать: вызвать или унять песчаную бурю"),
    "item.dunesrelics.bandage.desc": ("Heals, and cures Poison and Weakness", "Лечит, снимает отравление и слабость"),
}


def generate(assets):
    import volcanic_data
    for table, extra in ((BLOCKS, volcanic_data.BLOCKS), (ITEMS, volcanic_data.ITEMS), (ENTITIES, volcanic_data.ENTITIES),
                         (ADVANCEMENTS, volcanic_data.ADVANCEMENTS), (EXTRA, volcanic_data.EXTRA)):
        table.update(extra)
    for index, code in ((0, "en_us"), (1, "ru_ru")):
        entries = {}
        for key, names in BLOCKS.items():
            entries["block.dunesrelics." + key] = names[index]
        for key, names in ITEMS.items():
            entries["item.dunesrelics." + key] = names[index]
        for key, names in ENTITIES.items():
            entries["entity.dunesrelics." + key] = names[index]
        for key, texts in ADVANCEMENTS.items():
            title, description = texts[index]
            entries["advancements.dunesrelics.%s.title" % key] = title
            entries["advancements.dunesrelics.%s.description" % key] = description
        for key, texts in EXTRA.items():
            entries[key] = texts[index]
        path = os.path.join(assets, "lang", code + ".json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8") as out:
            json.dump(entries, out, indent=2, ensure_ascii=False)
            out.write("\n")
