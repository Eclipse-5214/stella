import { renderBoxOutline, renderFilledBox } from "../utils/renderUtils";
import { calcDistance, shortPrefix } from "../utils/utils";
import { FeatManager } from "../utils/helpers";
import { Render3D } from "../../tska/rendering/Render3D";
import settings from "../utils/config";
import Dungeon from "../../tska/skyblock/dungeon/Dungeon";

/*  -------------- Terminal Things ---------------

    Numbers the terminals in dungeon
    (for calling terms)

    terminal tracker

    ------------------- To Do -------------------

    - Done :D

    --------------------------------------------- */

//feature
const TermNumbers = FeatManager.createFeature("termNumbers", "catacombs");
const termTracker = FeatManager.createFeature("termTracker", "catacombs");

// [X, Y, Z, #, Class, M7]
const terms = JSON.parse(FileLib.read("stella", "data/dungeons/terms.json"));

//Thanks Kiwidotzip!
const termLabels = {
    T: ["&7( &6Tank &7)", [1, 170 / 255, 0]],
    M: ["&7( &bMage &7)", [85 / 255, 1, 1]],
    B: ["&7( &cBers &7)", [1, 85 / 255, 85 / 255]],
    A: ["&7( &2Arch &7)", [0, 170 / 255, 0]],
    S: ["&7( &6S&bt&ca&2c&dk &7)", [1, 1, 1]],
    1: "T",
    2: "M",
    3: "B",
    4: "A",
    5: 5,
};

TermNumbers.register("renderWorld", () => {
    if (!Dungeon.inBoss() || Dungeon.floorNumber != 7) return;

    let [r, g, b] = [settings().termColor[0] / 255, settings().termColor[1] / 255, settings().termColor[2] / 255];
    let playerPos = [Math.round(Player.getX() + 0.25) - 1, Math.round(Player.getY()), Math.round(Player.getZ() + 0.25) - 1];
    let t = settings().termNumber + 1;

    if (settings().m7Roles) t = termLabels[settings().termClass + 1];

    Object.entries(terms).forEach(([phase, data]) => {
        data.forEach((term) => {
            let [x, y, z, n, c, m7] = term;
            let text = " ";

            if (((settings().m7Roles && m7 !== "S") || (!settings().m7Roles && c !== "S")) && t !== n && t !== m7 && t !== 5) return;
            if (!settings().hideNumber || !settings().showTermClass) text += "\n&l&8[ &f" + n.toString() + " &8]";
            if (settings().showTermClass) {
                if (!settings().m7Roles) text += "\n" + termLabels[c][0];
                else text += "\n" + termLabels[m7][0];
            }
            if (settings().classColor) {
                if (!settings().m7Roles) [r, g, b] = termLabels[c][1];
                else [r, g, b] = termLabels[m7][1];
            }

            let pdistance = calcDistance(playerPos, [x, y, z]);
            if (pdistance < 1600) {
                if (pdistance > 13) Render3D.renderString(text, x + 0.5, y + 1.95, z + 0.5, [0, 0, 0, 180], false, 2, true, true, true);
                else Render3D.renderString(text, x + 0.5, y + 1.95, z + 0.5, [0, 0, 0, 180], false, 0.03, false, true, true);

                if (settings().highlightTerms) {
                    renderBoxOutline(x + 0.501, y - 0.001, z + 0.501, 1.002, 1.002, 1.002, r, g, b, 1, 1, false);
                    renderFilledBox(x + 0.501, y - 0.001, z + 0.501, 1.002, 1.002, 1.002, r, g, b, 50 / 255, false);
                }
            }
        });
    });
});

//terminal tracker
const completed = new Map();

termTracker
    .register(
        "chat",
        (name, type) => {
            if (name.includes(">")) return;
            let data = completed.get(name) || { terminal: 0, device: 0, lever: 0 };
            data[type]++;
            completed.set(name, data);
        },
        /^(\w{1,16}) (?:activated|completed) a (\w+)! \(\d\/\d\)$/
    )
    .register(
        "chat",
        () => {
            completed.forEach((data, name) => {
                ChatLib.chat(shortPrefix + "&b" + name + " &7completed &f" + data.terminal + "&7 terms, &f" + data.device + "&7 devices, and &f" + data.lever + " &7levers!");
            });
        },
        "The Core entrance is opening!"
    )
    .onRegister(() => completed.clear())
    .onUnregister(() => completed.clear());
