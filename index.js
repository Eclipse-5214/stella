import { prefix } from "./utils/utils";
import { fetch } from "../tska/polyfill/Fetch";
import { data } from "./utils/helpers";
import { hud } from "./utils/hud";
import settings from "./utils/config";

import "./features/firstInstall";
import "./features/blockOverlay";
import "./features/terms";
import "./features/dungeon";
import "./features/slotBinding";
import "./stellanav/map";

/*  ------------------- Index -------------------

    Core File    
    I tried to doccument how it works as best as I could

    ------------------- To Do -------------------

    - finish routes stuff
    - add profit calculator

    --------------------------------------------- */

//commands

register("command", (...args) => {
    if (args[0] === "help") {
        ChatLib.chat("&8&m-------------------------------------------------");
        ChatLib.chat("&6/stella &7main command! Aliases: &6/sa /sta");
        ChatLib.chat("&6/sa help &7Opens the Stella help menu!");
        ChatLib.chat("&6/sa update &7Checks for updates!");
        ChatLib.chat("&6/sa hud &7Opens the HUD editor!");
        ChatLib.chat("&6/stellaroutes &routes config! (if installed) Aliases: &6/sr /str");
        ChatLib.chat("&6/srdb &7 debug options for routes try &6/srdb help &7for more info!");
        ChatLib.chat("&6/route &7 route recording try &6/route help &7for more info!");
        ChatLib.chat("&8&m-------------------------------------------------");
    } else if (args[0] === "update") {
        checkUpdate();
        updateMessage = `&9&m${ChatLib.getChatBreak("-")}\n`;
    } else if (args[0] === "hud") {
        hud.open();
    } else if (!args || !args.length || !args[0]) {
        return settings().getConfig().openGui();
    } else {
        ChatLib.chat("&cUnknown command. &7Try &6/sa help &7for a list of commands");
    }
})
    .setName("stella")
    .setAliases("sa", "sta");

// Update check
const LOCAL_VERSION = JSON.parse(FileLib.read("stella", "metadata.json")).version.replace(/^v/, "");
const API_URL = "https://api.github.com/repos/Eclipse-5214/stella/releases";
let updateMessage = `&9&m${ChatLib.getChatBreak("-")}\n`;

function compareVersions(v1, v2) {
    const a = v1.split(".").map(Number),
        b = v2.split(".").map(Number);
    for (let i = 0, l = Math.max(a.length, b.length); i < l; i++) if ((a[i] || 0) !== (b[i] || 0)) return (a[i] || 0) > (b[i] || 0) ? 1 : -1;
    return 0;
}

function buildUpdateMessage(releases) {
    let message = `&9&m${ChatLib.getChatBreak("-")}\n&d&lStella Changelog: \n&fChanges since &bv${data.version}&f:\n`;
    releases
        .filter((release) => compareVersions(release.tag_name.replace(/^v/, ""), data.version) > 0)
        .forEach((r) => r.body.split("\n").forEach((l) => l.trim() !== "" && !l.trim().includes("**Full Changelog**") && (message += `&b${l.trim()}\n`)));
    return message + `&9&m${ChatLib.getChatBreak("-")}`;
}

function checkUpdate(silent = false) {
    fetch(API_URL, {
        headers: { "User-Agent": "Stella" },
        json: true,
    })
        .then((releases) => {
            if (!releases.length && !silent) return ChatLib.chat(prefix + " &fNo releases found!");
            updateMessage = buildUpdateMessage(releases);
            if (silent) return;
            compareVersions(LOCAL_VERSION, releases[0].tag_name) > 0
                ? ChatLib.chat(prefix + " &fYou're on a development build.")
                : compareVersions(LOCAL_VERSION, releases[0].tag_name) < 0 &&
                  (ChatLib.chat(prefix + ` &fUpdate available: &bv${releases[0].tag_name}&f! Current: &bv${LOCAL_VERSION}`),
                  ChatLib.chat(new TextComponent(prefix + ` &fClick here to go to the release page!`).setClick("open_url", `https://github.com/Eclipse-5214/stella/releases/latest`)),
                  ChatLib.chat(new TextComponent(prefix + ` &fHover over this message to view changelogs!`).setHoverValue(updateMessage)));
        })
        .catch((error) => ChatLib.chat(prefix + ` &fUpdate check failed: &c${error}`));
}

let updateChecked = false;

const Changelog = register("worldLoad", () => {
    checkUpdate(true);
    Changelog.unregister();
    Client.scheduleTask(40, () => (ChatLib.chat(updateMessage + "\n&bWe now have a discord server\n&bJoin it at https://discord.gg/EzEfQyGdAg"), (data.version = LOCAL_VERSION)));
}).unregister();

const UpdateCH = register("worldLoad", () => {
    updateChecked = true;
    UpdateCH.unregister();
    Client.scheduleTask(1000, () => (checkUpdate(), (updateMessage = `&9&m${ChatLib.getChatBreak("-")}\n`)));
}).unregister();

register("gameLoad", () => (data.version < LOCAL_VERSION && Changelog.register(), !updateChecked && UpdateCH.register()));
