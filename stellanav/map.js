import { getCheckmarks, WhiteMarker, GreenMarker, mapRGBs, defaultMapImage, renderPlayerHeads, oscale, getTextColor, typeToName, typeToColor, assets } from "./utils/mapUtils";
import { formatTime, isBetween } from "../utils/utils";
import { FeatManager } from "../utils/helpers";
import { fetch } from "../../tska/polyfill/Fetch.js";
import { hud } from "../utils/hud";
import DungeonScanner from "../../tska/skyblock/dungeon/DungeonScanner";
import InternalEvents from "../../tska/event/InternalEvents";
import EventListener from "../../tska/event/EventListener";
import settings from "../utils/config";
import Dungeon from "../../tska/skyblock/dungeon/Dungeon";
require("./utils/events.js");

const AbstractClientPlayer = Java.type("net.minecraft.client.entity.AbstractClientPlayer");
const BufferedImage = Java.type("java.awt.image.BufferedImage");
const Color = Java.type("java.awt.Color");

let PlayerComparator = Java.type("net.minecraft.client.gui.GuiPlayerTabOverlay").PlayerComparator;
let c = PlayerComparator.class.getDeclaredConstructor();
c.setAccessible(true);
let sorter = c.newInstance();

if (!GlStateManager) {
    var GL11 = Java.type("org.lwjgl.opengl.GL11"); //using var so it goes to global scope
    var GlStateManager = Java.type("net.minecraft.client.renderer.GlStateManager");
}

/*  ---------------- StellarNav -----------------

    Dungeon Map

    ------------------- To Do -------------------

    - Fix room name duplicates at low render distances

    --------------------------------------------- */

// Variables ///////////////////////////////////////////////////////////////////////////
//                                                                                    \\
//checkmarks                                                                          //
const checkmarkMap = new Map(); //                                                    \\
const editCheckmarkMap = new Map(); //                                                //                                                                //
//                                                                                    \\
//dungeon info                                                                        //
let players = {}; //                                                                  \\
let rooms = []; //                                                                    //
let collectedSecrets = {}; //                                                         \\
//                                                                                    //
//map image                                                                           \\
let mapBuffered = new BufferedImage(23, 23, BufferedImage.TYPE_4BYTE_ABGR); //        //
let mapImage = new Image(mapBuffered); //                                             \\
let emptyBuffered = new BufferedImage(23, 23, BufferedImage.TYPE_4BYTE_ABGR); //      //
let emptyImage = new Image(emptyBuffered); //                                         \\                                                                                   \\
//                                                                                    //
//map size                                                                            \\
const defaultMapSize = [138, 138]; //                                                 //
//                                                                                    \\
let mapScale = 1; //                                                                  //
let mapOffset = 0; //                                                                 \\
let headScale = 1; //                                                                 //
//                                                                                    \\
//map data                                                                            //
let mapIsEmpty = true; //                                                             \\
let watcherDone = false; //                                                           //
let dungeonDone = false; //                                                           \\
//                                                                                    //
let mapLine1 = "&7Secrets: &b?    &7Crypts: &c0    &7Mimic: &c✘"; //                  \\
let mapLine2 = "&7Min Secrets: &b?    &7Deaths: &a0    &7Score: &c0"; //              //
//\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

//feature
const StellaNav = FeatManager.createFeature("mapEnabled", "catacombs");

//gui
const MapGui = hud.createHud("StellaNav", 10, 10, defaultMapSize[0], defaultMapSize[1]);

//functions
let ll = 128 / 23;
const getRoomPosition = (x, y) => [ll * 1.5 + ll * 4 * x, ll * 1.5 + ll * 4 * y];

const setPixels = (x1, y1, width, height, color) => {
    if (!color) return;
    for (let x = x1; x < x1 + width; x++) for (let y = y1; y < y1 + height; y++) mapBuffered.setRGB(x, y, color.getRGB());
};

const clearMap = () => {
    setPixels(0, 0, 23, 23, new Color(0, 0, 0, 0));
};

const updatePlayer = (player) => {
    if (!Player.getPlayer()) return; //How tf is this null sometimes wtf
    let pl = Player.getPlayer()
        .field_71174_a.func_175106_d()
        .sort((a, b) => sorter.compare(a, b)); // Tab player list

    for (let p of pl) {
        if (!p.func_178854_k()) continue;
        let line = p.func_178854_k().func_150260_c();
        // https://regex101.com/r/cUzJoK/3
        line = line.replace(/§./g, ""); //support dungeons guide custom name colors
        let match = line.match(/^\[(\d+)\] (?:\[\w+\] )*(\w+) (?:.)*?\((\w+)(?: (\w+))*\)$/);
        if (!match) continue;
        let [_, sbLevel, name, clazz, level] = match;
        if (name != player) continue;
        return [p, clazz, level, sbLevel]; // [player, class, level, sbLevel]
    }
};

//edit hud
MapGui.onDraw((x, y) => {
    let [w, h] = defaultMapSize;
    let [r, g, b, a] = settings().mapBackgroundColor;
    h += settings().mapInfoUnder ? 10 : 0;

    Renderer.retainTransforms(true);
    Renderer.translate(x, y);
    Renderer.scale(MapGui.getScale());
    Renderer.drawRect(Renderer.color(r, g, b, a), 0, 0, w, h);
    Renderer.drawImage(defaultMapImage, 5, 5, 128, 128);
    Renderer.retainTransforms(false);
    Renderer.finishDraw();

    // add border
    if (settings().mapBorder) renderMapBorder();

    // Add fake checkmarks
    editCheckmarkMap.set(0, 34);
    editCheckmarkMap.set(12, 30);
    editCheckmarkMap.set(13, 18);
    editCheckmarkMap.set(16, 18);

    renderCheckmarks(editCheckmarkMap);

    // Add fake players
    let fun = AbstractClientPlayer.class.getDeclaredMethod("func_175155_b"); // getPlayerInfo
    fun.setAccessible(true);
    let info = fun.invoke(Player.getPlayer());
    if (info) renderPlayerHeads(info, 65 + x, 35 + y, 320, headScale, 3, "Mage", MapGui.getScale());

    if (settings().mapInfoUnder) renderUnderMapInfo();
});

StellaNav.register("renderOverlay", () => {
    if (hud.isOpen()) return;
    if (Dungeon.inBoss() && !settings().mapBossEnabled) return;

    renderMap();

    if (settings().mapBorder) renderMapBorder();

    if (!Dungeon.inBoss()) {
        renderCheckmarks(checkmarkMap);
        renderRoomNames();
        renderPuzzleNames();
        renderPlayers();
    } else if ((!dungeonDone && settings().mapBossEnabled && settings().mapScoreEnabled) || (dungeonDone && settings().mapBossEnabled && !settings().mapScoreEnabled)) {
        renderBoss();
    } else if (settings().mapScoreEnabled) {
        renderScore();
    }

    if (settings().mapInfoUnder) renderUnderMapInfo();
})
    //get players and map offset / scale
    .register("tick", () => {
        mapScale = oscale(Dungeon.floorNumber);
        mapOffset = Dungeon.floorNumber == 1 ? 10.6 : 0;
        headScale = settings().mapHeadScale / 5;

        let tempPlayers = DungeonScanner.players;
        if (!tempPlayers) return;

        tempPlayers.forEach((p) => {
            let player = p?.name;
            if (!player) return;

            //create a player object
            if (!Object.keys(players).includes(player)) {
                players[player] = {
                    info: [],
                    class: null,
                    uuid: null,
                    hasSpirit: false,
                    visited: [],
                    iconX: null,
                    iconY: null,
                    worldX: null,
                    worldY: null,
                    yaw: null,
                    visited: p?.visitedRooms,
                    cleared: p?.clearedRooms,
                    deaths: 0,
                    secrets: 0,
                    initSecrets: 0,
                    currSecrets: 0,
                    inRender: false,
                };
            }

            //update player info
            players[player].info = updatePlayer(player);
            players[player].class = Dungeon.players[player].className;
            players[player].visited = p?.visitedRooms;
            players[player].cleared = p?.clearedRooms;
            updatePlayerUUID(player);
        });

        //update player position from map
        if (Dungeon.mapData && Dungeon.mapCorners) {
            for (let p of Object.keys(players)) {
                let player = players[p];
                if (!players[p].inRender) {
                    let icon = Object.keys(Dungeon.icons).find((key) => Dungeon.icons[key].player == p);
                    if (!icon) continue;
                    icon = Dungeon.icons[icon];
                    player.iconX = MathLib.map(icon.x - Dungeon.mapCorners[0] * 2, 0, 256, 0, 128);
                    player.iconY = MathLib.map(icon.y - Dungeon.mapCorners[1] * 2, 0, 256, 0, 128);
                    player.yaw = icon.rotation;
                }
            }
        }

        rooms = DungeonScanner?.rooms;
    })
    //update player pos from world
    .register("tick", () => {
        for (let p of Object.keys(players)) {
            let player = World.getPlayerByName(p);
            if (!player) {
                players[p].inRender = false;
                continue;
            }
            if (player.getPing() == -1) {
                delete players[p];
                continue;
            }

            if (isBetween(player.getX(), -200, -10) || !isBetween(player.getZ(), -200, -10)) {
                players[p].iconX = MathLib.map(player.getX(), -200, -10, 0, 128);
                players[p].iconY = MathLib.map(player.getZ(), -200, -10, 0, 128);
                players[p].inRender = true;
            }

            players[p].worldX = player.getX();
            players[p].worldY = player.getZ();

            players[p].yaw = player.getYaw() + 180;
            players[p].class = Dungeon.players[p].className;
        }
    })
    //update under map info
    .register("tick", () => {
        let dSecrets = "&7Secrets: " + (!Dungeon.secretsFound ? "&b?" : `&b${Dungeon.secretsFound}&8-&e${Dungeon.scoreData.secretsRemaining}&8-&c${Dungeon.scoreData.totalSecrets}`);
        let dCrypts = "&7Crypts: " + (Dungeon.crypts >= 5 ? `&a${Dungeon.crypts}` : Dungeon.crypts > 0 ? `&e${Dungeon.crypts}` : `&c0`);
        let dMimic = [6, 7].includes(Dungeon.floorNumber) ? "&7Mimic: " + (Dungeon.mimicDead ? "&a✔" : "&c✘") : "";

        let minSecrets = "&7Min Secrets: " + (!Dungeon.secretsFound ? "&b?" : Dungeon.scoreData.minSecrets > Dungeon.secretsFound ? `&e${Dungeon.scoreData.minSecrets}` : `&a${Dungeon.scoreData.minSecrets}`);
        let dDeaths = "&7Deaths: " + (Dungeon.teamDeaths < 0 ? `&c${Dungeon.scoreData.teamDeaths}` : "&a0");
        let dScore = "&7Score: " + (Dungeon.scoreData.score >= 300 ? `&a${Dungeon.scoreData.score}` : Dungeon.scoreData.score >= 270 ? `&e${Dungeon.scoreData.score}` : `&c${Dungeon.scoreData.score}`) + (Dungeon._hasPaul ? " &b★" : "");

        mapLine1 = `${dSecrets}    ${dCrypts}    ${dMimic}`.trim();
        mapLine2 = `${minSecrets}    ${dDeaths}    ${dScore}`.trim();
    })
    //post dungeon breakdown
    .register(
        "chat",
        () => {
            dungeonDone = true;
            for (let p of Object.keys(players)) updateCurrentSecrets(p);

            Client.scheduleTask(4 * 20, () => {
                ChatLib.chat("&d[Stella]" + " &bCleared room counts:");
                for (let p of Object.keys(players)) {
                    let player = players[p];
                    let pWhiteRooms = player.cleared["WHITE"].toObject();
                    let pGreenRooms = player.cleared["GREEN"].toObject();
                    let wRoomNames = [];
                    let secrets = player.currSecrets - player.initSecrets;
                    let minRooms = 0;
                    let maxRooms = 0;
                    let final = new Message();

                    final.addTextComponent(new TextComponent("&d|" + "&b " + p + "&7 cleared "));

                    let roomLore = "";

                    for (let pRoomName of Object.keys(pGreenRooms)) {
                        if (pGreenRooms[pRoomName].solo) minRooms++;
                        maxRooms++;

                        let room = pGreenRooms[pRoomName].room;
                        wRoomNames.push(room.name);

                        let name = room.name == "Default" ? room.shape : room.name ?? room.shape;
                        let type = typeToName(room.type);
                        let color = typeToColor(room.type);
                        let time = formatTime(pGreenRooms[pRoomName].time);
                        let rplayers = room.players.toArray();
                        let stackStr = pGreenRooms[pRoomName].solo ? "" : ", Stacked with ";

                        if (!pGreenRooms[pRoomName].solo) {
                            rplayers.forEach((plr) => {
                                stackStr += plr?.name + " ";
                            });
                        }

                        roomLore += `&${color}${name} (${type}) &a✔ &${color}in ${time}${stackStr}\n`;
                    }

                    for (let pRoomName of Object.keys(pWhiteRooms)) {
                        let room = pWhiteRooms[pRoomName].room;
                        let greenCleared = false;
                        for (let roomName of wRoomNames) if (room.name == roomName) greenCleared = true;
                        if (greenCleared) continue;

                        if (pWhiteRooms[pRoomName].solo) minRooms++;
                        maxRooms++;

                        let name = room.name == "Default" ? room.shape : room.name ?? room.shape;
                        let type = typeToName(room.type);
                        let color = typeToColor(room.type);
                        let time = formatTime(pWhiteRooms[pRoomName].time);
                        let rplayers = room.players.toArray();
                        let stackStr = pWhiteRooms[pRoomName].solo ? "" : ", Stacked with ";

                        if (!pWhiteRooms[pRoomName].solo) {
                            rplayers.forEach((plr) => {
                                stackStr += plr?.name + " ";
                            });
                        }

                        roomLore += `&${color}${name} (${type}) &f✔ &${color}in ${time}${stackStr}\n`;
                    }

                    final.addTextComponent(new TextComponent("&b" + minRooms + "-" + maxRooms).setHover("show_text", roomLore.trim()));
                    final.addTextComponent(new TextComponent("&7 rooms | &b" + secrets + "&7 secrets"));
                    final.addTextComponent(new TextComponent("&7 | &b" + player.deaths + "&7 deaths"));
                    final.chat();
                }
            });
        },
        /^\s*(Master Mode)? ?(?:The)? Catacombs - (Entrance|Floor .{1,3})$/
    )
    .register(
        "chat",
        (info) => {
            let player = ChatLib.removeFormatting(info).split(" ")[0];
            if (!players) return;

            for (let p of Object.keys(players)) {
                if (p === player || (p == Player.getName() && player.toLowerCase() === "you")) {
                    players[p].deaths++;
                }
            }
        },
        "&r&c ☠ ${info} became a ghost&r&7.&r"
    )
    .register(
        "chat",
        () => {
            watcherDone = true;
        },
        /\[BOSS\] The Watcher: That will be enough for now\./
    );

//secret logging
EventListener.on("stella:secretCollect", (secret) => {
    let collected = false;
    if (collectedSecrets[secret.room]) {
        let i = 0;
        for (let srts of collectedSecrets[secret.room]) {
            if (srts.type != secret.type) continue;
            if (srts.pos[0] !== secret.pos[0] && srts.pos[1] !== secret.pos[1] && srts.pos[2] !== secret.pos[2]) continue;
            collected = true;
            i++;
        }
    } else collectedSecrets[secret.room] = [];
    if (!collected) collectedSecrets[secret.room].push(secret);

    if (settings().devMode) {
        ChatLib.chat("&d[Stella] &bSecret collected!");
        ChatLib.chat("&d| &bType: &r" + secret.type + " &bRoom: &r" + secret.room);
        ChatLib.chat("&d| &bPosition: &r" + secret.pos[0] + " " + secret.pos[1] + " " + secret.pos[2] + " &bCollected: &r" + collected.toString());
    }
});

//update from map
InternalEvents.on("mapdata", (mapData) => {
    if (Dungeon.inBoss()) return;
    const colors = mapData.field_76198_e;

    if (!colors || colors[0] == 119 || !Dungeon.mapCorners) return;

    clearMap();

    const checkmarkImages = getCheckmarks();
    const tempCheckmarkMap = new Map();

    // Find important points on the map and build a new one
    let xx = -1;
    for (let x = Dungeon.mapCorners[0] + Dungeon.mapRoomSize / 2; x < 118; x += Dungeon.mapGapSize / 2) {
        let yy = -1;
        xx++;
        for (let y = Dungeon.mapCorners[1] + Dungeon.mapRoomSize / 2 + 1; y < 118; y += Dungeon.mapGapSize / 2) {
            yy++;
            let i = x + y * 128;
            if (colors[i] == 0) continue;
            let center = colors[i - 1]; // Pixel where the checkmarks spawn
            let roomColor = colors[i + 5 + 128 * 4]; // Pixel in the borrom right-ish corner of the room which tells the room color.
            // Main room
            if (!(xx % 2) && !(yy % 2)) {
                let rmx = xx / 2;
                let rmy = yy / 2;
                let roomIndex = rmx * 6 + rmy;
                setPixels(xx * 2, yy * 2, 3, 3, mapRGBs[roomColor]);
                // Checkmarks and stuff
                if (roomColor == 18 && watcherDone && center != 30) {
                    tempCheckmarkMap.set(roomIndex, 34); // White checkmark for blood room
                }
                if (center in checkmarkImages && roomColor !== center) {
                    if (center !== 119) continue;
                    tempCheckmarkMap.set(roomIndex, center);
                }
            }
            // Center of 2x2
            if (xx % 2 && yy % 2) {
                setPixels(xx * 2 + 1, yy * 2 + 1, 1, 1, mapRGBs[center]);
                continue;
            }

            // Place where no doors or rooms can spawn
            if ((!(xx % 2) && !(yy % 2)) || (xx % 2 && yy % 2)) continue;

            let horiz = [colors[i - 128 - 4], colors[i - 128 + 4]];
            let vert = [colors[i - 128 * 5], colors[i + 128 * 3]];
            // Door
            if (horiz.every((a) => !a) || vert.every((a) => !a)) {
                if (center == 119) setPixels(xx * 2 + 1, yy * 2 + 1, 1, 1, new Color(settings().mapWitherDoorColor[0] / 255, settings().mapWitherDoorColor[1] / 255, settings().mapWitherDoorColor[2] / 255), 1);
                else if (center == 63) setPixels(xx * 2 + 1, yy * 2 + 1, 1, 1, new Color(92 / 255, 52 / 255, 14 / 255, 1));
                else setPixels(xx * 2 + 1, yy * 2 + 1, 1, 1, mapRGBs[center]);
                continue;
            }
            // Join for a larger room
            if (horiz.every((a) => !!a) && vert.every((a) => !!a)) {
                setPixels(xx * 2, yy * 2, 3, 3, mapRGBs[center]);
                continue;
            }
        }
    }
    mapImage = new Image(mapBuffered);
    mapIsEmpty = false;

    // Update the checkmark map now. Clearing it at the start of the function makes the checkmarks flicker.
    checkmarkMap.forEach((img, ind) => {
        if (!tempCheckmarkMap.has(ind)) checkmarkMap.delete(ind);
    });
    tempCheckmarkMap.forEach((img, ind) => {
        checkmarkMap.set(ind, img);
    });
});

//map border rendering
const renderMapBorder = () => {
    let [w, h] = defaultMapSize;
    let scale = settings().mapBorderWidth - 1;
    let [r, g, b, a] = settings().mapBorderColor;
    h += settings().mapInfoUnder ? 10 : 0;
    Renderer.retainTransforms(true);
    Renderer.translate(MapGui.getX(), MapGui.getY());
    Renderer.scale(MapGui.getScale());
    let color = Renderer.color(r, g, b, a);

    Renderer.drawLine(color, 0, 0 - scale, 0, h + scale, scale + 1);
    Renderer.drawLine(color, 0 - scale, 0, w + scale, 0, scale + 1);
    Renderer.drawLine(color, w, 0 - scale, w, h + scale, scale + 1);
    Renderer.drawLine(color, 0 - scale, h, w + scale, h, scale + 1);
    Renderer.retainTransforms(false);
};

//map rendering
const renderMap = () => {
    let map = mapIsEmpty ? emptyImage : mapImage;
    let [x, y] = [MapGui.getX(), MapGui.getY()];
    let [w, h] = defaultMapSize;
    let [r, g, b, a] = settings().mapBackgroundColor;
    h += settings().mapInfoUnder ? 10 : 0;

    Renderer.retainTransforms(true);
    Renderer.translate(x, y);
    Renderer.scale(MapGui.getScale());
    Renderer.drawRect(Renderer.color(r, g, b, a), 0, 0, w, h);
    if (!Dungeon.inBoss()) {
        Renderer.translate(mapOffset, 0);
        Renderer.translate(5, 5);
        Renderer.scale(mapScale);
        Renderer.drawImage(map, 0, 0, 128, 128);
    }
    Renderer.retainTransforms(false);
    Renderer.finishDraw();
};

//player heads
const renderPlayers = () => {
    if (!players) return;

    let keys = Object.keys(players);
    // Move the player to the end of the array so they get rendered above everyone else
    if (keys.includes(Player.getName())) {
        const ind = keys.indexOf(Player.getName());
        keys = keys.concat(keys.splice(ind, 1));
    }
    for (let p of keys) {
        if (players[p].class == "DEAD" && p !== Player.getName()) continue;
        let size = [7, 10];
        let head = p == Player.getName() ? GreenMarker : WhiteMarker;
        let borderWidth = 0;
        if (settings().mapHeadOutline) borderWidth = 3;

        let x = players[p].iconX || 0;
        let y = players[p].iconY || 0;
        if (!x && !y) continue;

        let yaw = players[p].yaw || 0;

        Renderer.retainTransforms(true);
        Renderer.translate(MapGui.getX(), MapGui.getY());
        Renderer.scale(MapGui.getScale());
        Renderer.translate((x + 5 + mapOffset) * mapScale, (y + 5) * mapScale);
        Renderer.scale(mapScale);

        let dontRenderOwn = !settings().mapShowOwn && p == Player.getName();

        // Render the player name
        if (settings().mapShowPlayerNames && ["Spirit Leap", "Infinileap"].includes(Player.getHeldItem()?.getName()?.removeFormatting()) && !dontRenderOwn) {
            let name = p;
            let width = Renderer.getStringWidth(name);
            let scale = headScale / 1.3;
            Renderer.translate(0, 8.5);
            Renderer.scale(scale);
            //shadow
            Renderer.drawStringWithShadow("&0" + name, -width / 2 + scale, 0);
            Renderer.drawStringWithShadow("&0" + name, -width / 2 - scale, 0);
            Renderer.drawStringWithShadow("&0" + name, -width / 2, +scale);
            Renderer.drawStringWithShadow("&0" + name, -width / 2, -scale);

            //normal
            Renderer.drawStringWithShadow(name, -width / 2, 0);
            Renderer.scale(1.3 / headScale, 1.3 / headScale);
            Renderer.translate(0, -8.5);
        }

        Renderer.rotate(yaw);
        Renderer.translate(-size[0] / 2, -size[1] / 2);
        if (!settings().mapPlayerHeads) Renderer.drawImage(head, 0, 0, size[0], size[1]);
        Renderer.retainTransforms(false);
        Renderer.finishDraw();

        let hscale = MapGui.getScale() * mapScale;

        // Render the player head
        if (!players[p] || !players[p].info) return;
        if (settings().mapPlayerHeads) renderPlayerHeads(players[p]?.info[0], (x + mapOffset) * hscale + MapGui.getX(), y * hscale + MapGui.getY(), yaw, headScale, borderWidth, players[p]?.info[1], hscale);
    }
};

//checkmarks
const renderCheckmarks = (map) => {
    //render question marks and blood room checkmarks
    const checkmarkImages = getCheckmarks();

    for (let entry of map.entries()) {
        let [roomIndex, checkmarkImage] = entry;
        let rx = Math.floor(roomIndex / 6);
        let ry = roomIndex % 6;
        let scale = 0.9;
        let [x, y] = getRoomPosition(rx, ry);
        let [w, h] = [12 * scale, 12 * scale];
        if (checkmarkImage == 119) [w, h] = [10 * scale, 12 * scale];

        Renderer.retainTransforms(true);
        Renderer.translate(MapGui.getX(), MapGui.getY());
        Renderer.scale(MapGui.getScale());
        Renderer.translate(mapOffset, 0);
        Renderer.scale(mapScale);
        Renderer.translate(x + 128 / 23 - 1, y + 128 / 23 - 1);
        Renderer.drawImage(checkmarkImages[checkmarkImage], -w / 2, -h / 2, w, h);
        Renderer.retainTransforms(false);
        Renderer.finishDraw();
    }
    //render all other checkmarks
    for (let room of rooms) {
        if (!room || !room.explored) continue;

        let check = getCheckmarks();
        let checkImg = null;
        let roomType = settings().mapRoomType;
        let mapType = settings().mapPuzzleType;

        if (!room.checkmark || !room.comps) continue;
        if (!settings().mapDontDelayRender && !room.corner) continue;

        if (roomType == 2 && room?.secrets != 0 && (room.type == 0 || room.type == 6)) continue;
        if ((roomType == 1 || roomType == 3) && (room.type == 0 || room.type == 6)) continue;
        if ((mapType > 0 && room.type == 1) || room.type == 7) continue;
        if (room.checkmark == 0) continue;
        if (room.checkmark == 1) checkImg = check[34];
        if (room.checkmark == 2) checkImg = check[30];
        if (room.checkmark == 3) checkImg = check[18];
        if (room.checkmark == 4) checkImg = check[119];

        let scale = 0.9 * (settings().mapRoomScale / 5);
        let location = room.comps[0];

        let minX = Math.min(...room.comps.map((a) => a[0]));
        let minZ = Math.min(...room.comps.map((a) => a[1]));
        let roomWidth = Math.max(...room.comps.map((a) => a[0])) - minX;
        let roomHeight = Math.max(...room.comps.map((a) => a[1])) - minZ;
        location = [minX + roomWidth / 2, minZ + roomHeight / 2];
        if (room.shape == "L") {
            if (room.comps.filter((a) => a[1] == minZ).length == 2) location[1] -= roomHeight / 2;
            else location[1] += roomHeight / 2;
        }

        let [x, y] = getRoomPosition(location[0], location[1]);

        let [w, h] = [12 * scale, 12 * scale];

        Renderer.retainTransforms(true);
        Renderer.translate(MapGui.getX(), MapGui.getY());
        Renderer.scale(MapGui.getScale());
        Renderer.translate(mapOffset, 0);
        Renderer.scale(mapScale);
        Renderer.translate(x + 128 / 23 - 1, y + 128 / 23 - 1);
        Renderer.drawImage(checkImg, -w / 2, -h / 2, w, h);
        Renderer.retainTransforms(false);
        Renderer.finishDraw();
    }
};

//room names
const renderRoomNames = () => {
    for (let room of rooms) {
        let type = settings().mapRoomType;
        if (type < 1) continue;
        if (!room || !room.explored || !room.comps || !room.name) continue;
        if (!settings().mapDontDelayRender && !room.corner) continue;
        if (room.type != 0 && room.type != 6) continue;

        let textColor = null;
        let secrets = collectedSecrets[room.name] ? collectedSecrets[room.name].length : 0;
        if (room.checkmark == 2) secrets = room.secrets;

        textColor = getTextColor(room.checkmark);
        let text = [];
        if (type == 1 || type == 3) text = room.name?.split(" ") || ["???"];

        let sectext = secrets + "/" + room?.secrets || "?";
        if (room?.secrets && room?.secrets !== 0 && (type == 2 || type == 3)) text.push(sectext);

        //let text = room.name;
        let scale = 0.75 * (settings().mapRoomScale / 5);
        let location = room.comps[0];

        let minX = Math.min(...room.comps.map((a) => a[0]));
        let minZ = Math.min(...room.comps.map((a) => a[1]));
        let roomWidth = Math.max(...room.comps.map((a) => a[0])) - minX;
        let roomHeight = Math.max(...room.comps.map((a) => a[1])) - minZ;
        location = [minX + roomWidth / 2, minZ + roomHeight / 2];

        if (room.shape == "L") {
            if (room.comps.filter((a) => a[1] == minZ).length == 2) location[1] -= roomHeight / 2;
            else location[1] += roomHeight / 2;
        }
        let [x, y] = getRoomPosition(location[0], location[1]);

        Renderer.retainTransforms(true);
        Renderer.translate(MapGui.getX(), MapGui.getY());
        Renderer.scale(MapGui.getScale());
        Renderer.translate(mapOffset, 0);
        Renderer.scale(mapScale);
        Renderer.translate(x + 128 / 23 - 1, y + 128 / 23 - 1);
        Renderer.scale(scale);

        let i = 0;
        for (let line of text) {
            let ly = 9 * i - (text.length * 9) / 2;
            let w = Renderer.getStringWidth(line);

            //shadow
            Renderer.drawStringWithShadow("&0" + line, -w / 2 + scale, ly);
            Renderer.drawStringWithShadow("&0" + line, -w / 2 - scale, ly);
            Renderer.drawStringWithShadow("&0" + line, -w / 2, ly + scale);
            Renderer.drawStringWithShadow("&0" + line, -w / 2, ly - scale);

            //normal
            Renderer.drawStringWithShadow(textColor + line, -w / 2, ly);
            i++;
        }
        Renderer.retainTransforms(false);
        Renderer.finishDraw();
    }
};

//puzzle names
const renderPuzzleNames = () => {
    for (let room of rooms) {
        let type = settings().mapPuzzleType;
        if (type < 1) continue;
        if (!room || !room.explored || !room.comps || !room.name) continue;
        if (!settings().mapDontDelayRender && !room.corner) continue;
        if (room.type != 1) continue;

        let textColor = null;
        let secrets = 0;
        if (room.checkmark == 2) secrets = room?.secrets;

        textColor = getTextColor(room.checkmark);
        let text = [];
        if (type == 1 || type == 3) text = room.name?.split(" ") || ["???"];

        let sectext = secrets + " / " + room?.secrets || "?";
        if (room?.secrets && room?.secrets !== 0 && (type == 2 || type == 3)) text.push(sectext);

        //let text = room.name;
        let scale = 0.75 * (settings().mapPuzzleScale / 5);
        let location = room.comps[0];

        let minX = Math.min(...room.comps.map((a) => a[0]));
        let minZ = Math.min(...room.comps.map((a) => a[1]));
        let roomWidth = Math.max(...room.comps.map((a) => a[0])) - minX;
        let roomHeight = Math.max(...room.comps.map((a) => a[1])) - minZ;
        location = [minX + roomWidth / 2, minZ + roomHeight / 2];
        if (room.shape == "L") {
            if (room.comps.filter((a) => a[1] == minZ).length == 2) location[1] -= roomHeight / 2;
            else location[1] += roomHeight / 2;
        }

        let [x, y] = getRoomPosition(location[0], location[1]);

        Renderer.retainTransforms(true);
        Renderer.translate(MapGui.getX(), MapGui.getY());
        Renderer.scale(MapGui.getScale());
        Renderer.translate(mapOffset, 0);
        Renderer.scale(mapScale);
        Renderer.translate(x + 128 / 23 - 1, y + 128 / 23 - 1);
        Renderer.scale(scale);

        let i = 0;
        for (let line of text) {
            let ly = 9 * i - (text.length * 9) / 2;
            let w = Renderer.getStringWidth(line);

            //shadow
            Renderer.drawStringWithShadow("&0" + line, -w / 2 + scale, ly);
            Renderer.drawStringWithShadow("&0" + line, -w / 2 - scale, ly);
            Renderer.drawStringWithShadow("&0" + line, -w / 2, ly + scale);
            Renderer.drawStringWithShadow("&0" + line, -w / 2, ly - scale);

            //normal
            Renderer.drawStringWithShadow(textColor + line, -w / 2, ly);
            i++;
        }
        Renderer.retainTransforms(false);
        Renderer.finishDraw();
    }
};

//map info
const renderUnderMapInfo = () => {
    Renderer.retainTransforms(true);
    Renderer.translate(MapGui.getX(), MapGui.getY());
    Renderer.scale(MapGui.getScale());
    Renderer.translate(138 / 2, 135);
    Renderer.scale(0.6, 0.6);
    let w1 = Renderer.getStringWidth(mapLine1);
    let w2 = Renderer.getStringWidth(mapLine2);
    Renderer.drawStringWithShadow(mapLine1, -w1 / 2, 0);
    Renderer.drawStringWithShadow(mapLine2, -w2 / 2, 10);
    Renderer.retainTransforms(false);
};

//boss map stuff
dungeonBossImages = {};
new Thread(() => {
    let imageData = JSON.parse(FileLib.read("stella", "stellanav/data/imageData.json"));
    Object.keys(imageData).forEach((v) => {
        for (let i of imageData[v]) i.image = Image.fromFile(assets + "/boss/" + i.image);
    });
    dungeonBossImages = imageData;
}).start();

const getBossMap = (floor) => {
    let tempData = dungeonBossImages[floor.toString()];
    if (!tempData) return;

    let bossMap = null;
    let playerPos = [Player.getX(), Player.getY(), Player.getZ()];
    tempData.forEach((data) => {
        // Creates an array of player coords, corner1, corner2 and transposes it to make it easier to use the inBetween function.
        let c = [playerPos, data.bounds[0], data.bounds[1]];
        let coords = [0, 1, 2].map((v) => c.map((b) => b[v])); // Transpose the matrix
        if (!coords.every((v) => isBetween(...v))) return;
        bossMap = data;
    });
    return bossMap;
};

const renderBoss = () => {
    let bossMap = getBossMap(Dungeon.floorNumber);
    if (!bossMap) return;

    let [x, y, scale] = [MapGui.getX(), MapGui.getY(), MapGui.getScale()];

    let size = 128;
    let topLeftHudLocX = 0;
    let topLeftHudLocZ = 0;
    let sizeInWorld = 0;
    let sizeInPixels = 0;
    let textureScale = 0;

    //icons
    let headScale = 1;
    let borderWidth = 0;

    sizeInWorld = Math.min(bossMap.widthInWorld, bossMap.heightInWorld, bossMap.renderSize || Infinity);
    let pixelWidth = (bossMap.image.getTextureWidth() / bossMap.widthInWorld) * (bossMap.renderSize || bossMap.widthInWorld);
    let pixelHeight = (bossMap.image.getTextureHeight() / bossMap.heightInWorld) * (bossMap.renderSize || bossMap.heightInWorld);
    sizeInPixels = Math.min(pixelWidth, pixelHeight);

    textureScale = size / sizeInPixels;

    topLeftHudLocX = ((Player.getX() - bossMap.topLeftLocation[0]) / sizeInWorld) * size - size / 2;
    topLeftHudLocZ = ((Player.getZ() - bossMap.topLeftLocation[1]) / sizeInWorld) * size - size / 2;

    topLeftHudLocX = MathLib.clampFloat(topLeftHudLocX, 0, Math.max(0, bossMap.image.getTextureWidth() * textureScale - size));
    topLeftHudLocZ = MathLib.clampFloat(topLeftHudLocZ, 0, Math.max(0, bossMap.image.getTextureHeight() * textureScale - size));

    let image = bossMap.image;

    let guiScale = Renderer.screen.getScale();
    let screenHeight = Renderer.screen.getHeight();
    let sx = (x + 5) * guiScale;
    let sy = screenHeight * guiScale - (y + 5) * guiScale - size * scale * guiScale;
    let ssize = size * scale * guiScale;

    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor(sx, sy, ssize, ssize);

    Renderer.retainTransforms(true);
    Renderer.translate(x + 5, y + 5);
    Renderer.scale(scale);
    image.draw(-topLeftHudLocX, 0 - topLeftHudLocZ, image.getTextureWidth() * textureScale, image.getTextureHeight() * textureScale);
    Renderer.retainTransforms(false);
    Renderer.finishDraw();

    //players
    for (let p of Object.keys(players)) {
        if (players[p].class == "DEAD" && p !== Player.getName()) continue;

        let player = players[p];

        let hsize = [7, 10];
        let head = p == Player.getName() ? GreenMarker : WhiteMarker;
        if (settings().mapHeadOutline) borderWidth = 3;

        let renderX = null;
        let renderY = null;

        renderX = ((player.worldX - bossMap.topLeftLocation[0]) / sizeInWorld) * size - topLeftHudLocX;
        renderY = ((player.worldY - bossMap.topLeftLocation[1]) / sizeInWorld) * size - topLeftHudLocZ;

        Renderer.retainTransforms(true);
        Renderer.translate(x + renderX + 5, y + renderY + 5);
        Renderer.scale(scale);

        let dontRenderOwn = !settings().mapShowOwn && p == Player.getName();

        // Render the player name
        if (settings().mapShowPlayerNames && ["Spirit Leap", "Infinileap"].includes(Player.getHeldItem()?.getName()?.removeFormatting()) && !dontRenderOwn) {
            let name = p;
            let width = Renderer.getStringWidth(name);
            let tscale = headScale / 1.3;
            Renderer.translate(0, 8.5);
            Renderer.scale(tscale);
            Renderer.drawStringWithShadow(name, -width / 2, 0);
            Renderer.scale(1.3 / headScale, 1.3 / headScale);
            Renderer.translate(0, -8.5);
        }

        Renderer.rotate(player.yaw ? player.yaw : 0);
        Renderer.translate(-hsize[0] / 2, -hsize[1] / 2);
        if (!settings().mapPlayerHeads) Renderer.drawImage(head, 0, 0, hsize[0], hsize[1]);
        Renderer.retainTransforms(false);
        Renderer.finishDraw();

        if (settings().mapPlayerHeads) renderPlayerHeads(player?.info[0], renderX + x + 5, renderY + y + 5, player.yaw ? player.yaw : 0, headScale, borderWidth, players[p]?.info[1], scale);
    }

    GL11.glScissor(0, 0, 0, 0);
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
};

const renderScore = () => {
    let mapData; // Get map data from hotbar
    try {
        let item = Player.getInventory().getStackInSlot(8);
        mapData = item.getItem().func_77873_a(item.getItemStack(), World.getWorld()); // ItemStack.getItem().getMapData()
    } catch (error) {}

    if (!mapData) return;

    // Render map directly from hotbar
    let [x, y, scale] = [MapGui.getX(), MapGui.getY(), MapGui.getScale()];
    let size = 128;

    GlStateManager.func_179094_E(); // GlStateManager.push()
    Renderer.translate(x + 5, y + 5, 1);
    GlStateManager.func_179152_a((size / 128) * scale, (size / 128) * scale, 1); // GlStateManager.scale()
    GlStateManager.func_179131_c(1.0, 1.0, 1.0, 1.0); // GlStateManager.color()
    Client.getMinecraft().field_71460_t.func_147701_i().func_148250_a(mapData, true);
    GlStateManager.func_179121_F(); // GlStateManager.pop()
};

//api stuff
let secretsData = new Map();

register("step", () => {
    // Check if peoples data needs to be cleared from the map
    secretsData.forEach(([timestamp], uuid) => {
        if (Date.now() - timestamp > 5 * 60 * 1000) secretsData.delete(uuid);
    });
}).setDelay(10);

function getPlayerSecrets(uuid, cacheMs, callback) {
    if (secretsData.get(uuid)?.[0]?.timestamp > Date.now() - cacheMs) {
        callback(secretsData.get(uuid)[1]);
        return;
    }
    fetch(`https://api.tenios.dev/secrets/${uuid}`, {
        headers: { "User-Agent": "Stella" },
        json: true,
    }).then((secretsNum) => {
        let secrets = parseInt(secretsNum);
        secretsData.set(uuid, [Date.now(), secrets]);

        callback(secretsData.get(uuid)[1]);
    });
}

function updatePlayerUUID(p) {
    if (players[p].uuid) return;
    // Check players in world to update uuid field
    let player = World.getPlayerByName(p);
    if (!player) return;
    players[p].uuid = player.getUUID().toString();
    getPlayerSecrets(players[p].uuid, 120000, (secrets) => {
        players[p].initSecrets = secrets;
        players[p].currSecrets = secrets;
    });
}

function updateCurrentSecrets(p) {
    if (!players[p].uuid) return;
    getPlayerSecrets(players[p].uuid, 0, (secrets) => {
        players[p].currSecrets = secrets;
    });
}

//Reset on world unload
register("worldUnload", () => {
    clearMap();
    checkmarkMap.clear();
    collectedSecrets = {};
    players = {};
    mapIsEmpty = true;
    rooms = [];
    watcherDone = false;
    dungeonDone = false;
});
