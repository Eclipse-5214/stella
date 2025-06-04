import Settings from "../../Amaterasu/core/Settings";
import DefaultConfig from "../../Amaterasu/core/DefaultConfig";
import { hud } from "./hud";

/*  ------------------- Config ------------------

    Core Config

    ------------------- To Do -------------------

    - Make theming work

    --------------------------------------------- */

//setup

//markdown stuff
const version = JSON.parse(FileLib.read("stella", "metadata.json")).version;

const CREDITS = FileLib.read("stella", "assets/credits.md");
const CHANGELOG = `# §dStella v${version}\n ${FileLib.read("stella", "assets/changelog.md")}`;

const schemes = ["data/ColorScheme.json", "data/scheme-vigil.json", "data/scheme-nwjn.json"];

//guis
export const roomName = new Gui();

//config
const defaultConf = new DefaultConfig("stella", "data/settings.json")

    //general
    .addTextParagraph({
        category: "General",
        configName: "Info",
        title: `&6&l&dStella`,
        description: "&bMade by NEXD_",
        centered: true,
        subcategory: "",
    })

    .addButton({
        category: "General",
        subcategory: "",
        configName: "MyDiscord",
        title: "Discord Server",
        description: "Join if you want to report a bug or want to make a suggestion",
        tags: ["discord"],
        onClick(setting) {
            ChatLib.command("ct copy coming soon", true);
            ChatLib.chat("&6Copied Discord Link!");
        },
    })

    .addButton({
        category: "General",
        subcategory: "",
        configName: "MyGithub",
        title: "Github",
        description: "The source code for all this :D",
        tags: ["github"],
        onClick(setting) {
            ChatLib.command("ct copy https://github.com/Eclipse-5214/stella", true);
            ChatLib.chat("&6Copied Discord Link!");
        },
    })

    .addButton({
        category: "General",
        subcategory: "",
        configName: "OpenSR",
        title: "Routes",
        description: "Open Stella Routes config (if installed)",
        //tags: ["discord"],
        onClick(setting) {
            ChatLib.command("stellaroutes", true);
        },
    })

    .addButton({
        category: "General",
        subcategory: "",
        configName: "InstallsrSR",
        title: "Install Stella Routes",
        description: "Installs the Stella Routes addon (if not installed)",
        //tags: ["discord"],
        onClick(setting) {
            ChatLib.command("ct import stellaRoutes", true);
        },
    })

    //dungoens

    //dugeon trash
    .addSwitch({
        category: "Dungeons",
        configName: "highlightTrash",
        title: "Highlight Dungeon trash",
        description: "Highlights dungeon trash in your inventory",
        subcategory: "General",
    })

    .addColorPicker({
        configName: "trashColor",
        title: "Highlight Color",
        description: "The color to highlight trash",
        category: "Dungeons",
        subcategory: "General",
        value: [0, 255, 255, 255],

        shouldShow(data) {
            return data.highlightTrash;
        },
    })

    //room name
    .addSwitch({
        category: "Dungeons",
        configName: "showRoomName",
        title: "Show Room Name",
        description: "Shows the current rooms name",
        subcategory: "Room Name",
    })

    .addSwitch({
        category: "Dungeons",
        configName: "chromaRoomName",
        title: "Chroma room name",
        description: "Makes the name chroma",
        subcategory: "Room Name",

        shouldShow(data) {
            return data.showRoomName;
        },
    })

    .addButton({
        category: "Dungeons",
        configName: "editRoomName",
        title: "Room Name Location",
        description: "Move the room name, change the scale, etc.",
        subcategory: "Room Name",
        placeHolder: "Edit",

        onClick() {
            hud.open();
        },

        shouldShow(data) {
            return data.showRoomName;
        },
    })

    .addColorPicker({
        configName: "roomNameColor",
        title: "Background Color",
        description: "The Background Color of the room name",
        category: "Dungeons",
        subcategory: "Room Name",
        value: [0, 0, 0, 0],

        shouldShow(data) {
            return data.showRoomName;
        },
    })

    //terminal numbers
    .addSwitch({
        category: "Dungeons",
        configName: "termNumbers",
        title: "Terminal Numbers",
        description: "Number the terminals in dungeons (for calling terms)",
        subcategory: "Terminals",
    })

    .addDropDown({
        configName: "termNumber",
        title: "Number",
        description: "What terminal number you want to call",
        category: "Dungeons",
        subcategory: "Terminals",
        options: ["1", "2", "3", "4", "All"],
        value: 4,

        shouldShow(data) {
            return data.termNumbers;
        },
    })

    .addSwitch({
        category: "Dungeons",
        configName: "highlightTerms",
        title: "Highlight Terms",
        description: "Highlihgts the terminals",
        subcategory: "Terminals",

        shouldShow(data) {
            return data.termNumbers;
        },
    })

    .addColorPicker({
        configName: "termColor",
        title: "Highlight Color",
        description: "The color to highlight the terminals",
        category: "Dungeons",
        subcategory: "Terminals",
        value: [0, 255, 255, 255],

        shouldShow(data) {
            return data.termNumbers && data.highlightTerms;
        },
    })

    .addSwitch({
        category: "Dungeons",
        configName: "showTermClass",
        title: "Show Class",
        description: "Displays related class",
        subcategory: "Terminals",

        shouldShow(data) {
            return data.termNumbers;
        },
    })

    .addSwitch({
        category: "Dungeons",
        configName: "classColor",
        title: "Highlight class Color",
        description: "Highlights the terminals the color of the class",
        subcategory: "Terminals",

        shouldShow(data) {
            return data.termNumbers && data.highlightTerms && data.showTermClass;
        },
    })

    .addSwitch({
        category: "Dungeons",
        configName: "hideNumber",
        title: "Hide number",
        description: "Hides the terminal number",
        subcategory: "Terminals",

        shouldShow(data) {
            return data.termNumbers && data.showTermClass;
        },
    })

    .addSwitch({
        category: "Dungeons",
        configName: "m7Roles",
        title: "M7 roles",
        description: "Displays m7 roles instead",
        subcategory: "Terminals",

        shouldShow(data) {
            return data.termNumbers && data.showTermClass;
        },
    })

    .addDropDown({
        configName: "termClass",
        title: "M7 Class",
        description: "What class you are playing",
        category: "Dungeons",
        subcategory: "Terminals",
        options: ["Tank", "Mage", "Bers", "Arch", "All"],
        value: 4,

        shouldShow(data) {
            return data.termNumbers && data.showTermClass && data.m7Roles;
        },
    })

    //terminal tracker
    .addSwitch({
        category: "Dungeons",
        configName: "termTracker",
        title: "Terminal Tracker",
        description: "Tracks terminals, devices, and levers",
        subcategory: "Terminals",
    })

    //block overlay
    .addSwitch({
        category: "Msc.",
        configName: "overlayEnabled",
        title: "Render Block Overlay",
        description: "Highlights the block you are looking at",
        subcategory: "Block Overlay",
    })

    .addColorPicker({
        configName: "blockHighlightColor",
        title: "Block Highlight Color",
        description: "The color to highlight blocks",
        category: "Msc.",
        subcategory: "Block Overlay",
        value: [0, 255, 255, 255],

        shouldShow(data) {
            return data.overlayEnabled;
        },
    })

    .addSwitch({
        configName: "fillBlockOverlay",
        title: "Fill blocks",
        description: "Fills the blocks with the color",
        category: "Msc.",
        subcatagory: "Block Overlay",

        shouldShow(data) {
            return data.overlayEnabled;
        },
    })

    .addColorPicker({
        configName: "blockFillColor",
        title: "Block Fill Color",
        description: "The color to fill blocks",
        category: "Msc.",
        subcategory: "Block Overlay",
        value: [0, 255, 255, 30],

        shouldShow(data) {
            return data.overlayEnabled && data.fillBlockOverlay;
        },
    })

    .addSwitch({
        configName: "chromaHighlight",
        title: "Chroma overlay",
        description: "Makes the outline chroma",
        category: "Msc.",
        subcatagory: "Block Overlay",

        shouldShow(data) {
            return data.overlayEnabled;
        },
    })

    .addSlider({
        configName: "chromaOverlaySpeed",
        title: "Chroma Speed",
        description: "The speed of the chroma effect",
        category: "Msc.",
        subcategory: "Block Overlay",
        options: [1, 10],
        value: 1,

        shouldShow(data) {
            return data.overlayEnabled && data.chromaHighlight;
        },
    })

    .addSlider({
        configName: "overlayLineWidth",
        title: "line width",
        description: "Line width for the outline",
        category: "Msc.",
        subcategory: "Block Overlay",
        options: [1, 5],
        value: 3,

        shouldShow(data) {
            return data.overlayEnabled;
        },
    })

    .addSwitch({
        configName: "devMode",
        title: "Debug Info",
        description: "Debug info for developers",
        category: "Msc.",
        subcatagory: "Dev",
    })

    //slot binding
    .addSwitch({
        category: "Msc.",
        configName: "slotBinding",
        title: "Slot Binding",
        description: "Bind slots to hotbar slots",
        subcategory: "Slot Binding",
    })

    //map
    .addSwitch({
        category: "StellaNav",
        configName: "mapEnabled",
        title: "Dungeon Map",
        description: "Renders stella's dungeon map",
        subcategory: "Main",
    })

    .addButton({
        category: "StellaNav",
        configName: "editMap",
        title: "Map Location",
        description: "Move StellaNav, change the scale, etc.",
        subcategory: "Main",
        placeHolder: "Edit",

        onClick() {
            hud.open();
        },

        shouldShow(data) {
            return data.showRoomName;
        },
    })

    .addSwitch({
        category: "StellaNav",
        configName: "mapBossEnabled",
        title: "Dungeon Boss Map",
        description: "Renders stella's boss map",
        subcategory: "Main",
    })

    .addSwitch({
        category: "StellaNav",
        configName: "mapScoreEnabled",
        title: "Dungeon Score Map",
        description: "Renders stella's score map",
        subcategory: "Main",
    })

    .addSlider({
        configName: "mapHeadScale",
        title: "Marker Size",
        description: "Size of the player marker / heads",
        category: "StellaNav",
        subcategory: "Markers",
        options: [1, 10],
        value: 5,
    })

    .addSwitch({
        category: "StellaNav",
        configName: "mapPlayerHeads",
        title: "Show Player Heads",
        description: "Shows player heads on the map",
        subcategory: "Markers",
    })

    .addSwitch({
        category: "StellaNav",
        configName: "mapHeadOutline",
        title: "Outline player heads",
        description: "Outlines the head",
        subcategory: "Markers",

        shouldShow(data) {
            return data.mapPlayerHeads;
        },
    })

    .addColorPicker({
        configName: "mapHeadColor",
        title: "Player Head Outline Color",
        description: "The color to make the player head outline",
        category: "StellaNav",
        subcategory: "Markers",
        value: [0, 0, 0, 255],

        shouldShow(data) {
            return data.mapHeadOutline && data.mapPlayerHeads;
        },
    })

    .addSwitch({
        category: "StellaNav",
        configName: "mapClassColors",
        title: "Class Colors",
        description: "Makes the outline the color of the class",
        subcategory: "Markers",

        shouldShow(data) {
            return data.mapHeadOutline && data.mapPlayerHeads;
        },
    })

    .addSwitch({
        category: "StellaNav",
        configName: "mapShowPlayerNames",
        title: "Show Names",
        description: "Shows the player names on the map when holding leaps",
        subcategory: "Markers",
    })

    .addSwitch({
        category: "StellaNav",
        configName: "mapShowOwn",
        title: "Show Own Name",
        description: "Showes your own name",
        subcategory: "Markers",

        shouldShow(data) {
            return data.mapShowPlayerNames;
        },
    })

    .addDropDown({
        configName: "mapRoomType",
        title: "Room Check Type",
        description: "What type of room checkmark you want",
        category: "StellaNav",
        subcategory: "Map",
        options: ["Checkmark", "Name", "Secrets", "Both"],
        value: 0,
    })

    .addSlider({
        configName: "mapRoomScale",
        title: "Room Name / Checkmark Size",
        description: "Size of the room name / checkmark",
        category: "StellaNav",
        subcategory: "Map",
        options: [1, 10],
        value: 5,
    })

    .addDropDown({
        configName: "mapPuzzleType",
        title: "Puzzle Check Type",
        description: "What type of puzzle checkmark you want",
        category: "StellaNav",
        subcategory: "Map",
        options: ["Checkmark", "Name", "Secrets", "Both"],
        value: 1,
    })

    .addSlider({
        configName: "mapPuzzleScale",
        title: "Puzzle Name / Checkmark Size",
        description: "Size of the puzzle name / checkmark",
        category: "StellaNav",
        subcategory: "Map",
        options: [1, 10],
        value: 5,
    })

    .addSwitch({
        configName: "mapInfoUnder",
        title: "Dungeon Info",
        description: "Info under the map",
        category: "StellaNav",
        subcategory: "Map",
    })

    .addColorPicker({
        configName: "mapWitherDoorColor",
        title: "Wither Door Color",
        description: "The color to make wither doors on the map",
        category: "StellaNav",
        subcategory: "Map",
        value: [0, 0, 0, 255],
    })

    .addColorPicker({
        configName: "mapBackgroundColor",
        title: "Map Background Color",
        description: "The color to make the map background",
        category: "StellaNav",
        subcategory: "Map",
        value: [0, 0, 0, 100],
    })

    .addSwitch({
        configName: "mapBorder",
        title: "Map Border",
        description: "Shows a border around the map",
        category: "StellaNav",
        subcategory: "Map",
    })

    .addSlider({
        configName: "mapBorderWidth",
        title: "Map Border Width",
        description: "Width of the map border",
        category: "StellaNav",
        subcategory: "Map",
        options: [1, 5],
        value: 1,

        shouldShow(data) {
            return data.mapBorder;
        },
    })

    .addColorPicker({
        configName: "mapBorderColor",
        title: "Map Border Color",
        description: "The color to make the map border",
        category: "StellaNav",
        subcategory: "Map",
        value: [0, 0, 0, 255],

        shouldShow(data) {
            return data.mapBorder;
        },
    })

    .addSwitch({
        configName: "mapDontDelayRender",
        title: "Don't Delay Render",
        description: "Renders the map names / secrets without being in the render distance of the room, can cause graphical issues",
        category: "StellaNav",
        subcategory: "Advanced",
    })

    //themeing
    .addSelection({
        category: "Theme",
        configName: "scheme",
        title: "Themes",
        description: "Select which theme you want from these presets (needs apply after)",
        options: ["Default", "Vigil", "nwjn"],
    })

    .addButton({
        category: "Theme",
        configName: "apply",
        title: "Apply Changes",
        description: "Need to click this for window to reload with selected changes",
        placeHolder: "Apply",
        onClick(config) {
            const currentScheme = schemes[config.settings.scheme];
            config.setPos(config.settings.x, config.settings.y).setSize(config.settings.width, config.settings.height).setScheme(currentScheme).apply();
        },
    })

    .addSlider({
        category: "Theme",
        configName: "x",
        title: "Change X",
        description: "Changes the starting X coordinate of the Config (in percent)",
        options: [0, 75],
        value: 20,
    })

    .addSlider({
        category: "Theme",
        configName: "y",
        title: "Change Y",
        description: "Changes the starting Y coordinate of the Config (in percent)",
        options: [0, 75],
        value: 20,
    })

    .addSlider({
        category: "Theme",
        configName: "width",
        title: "Change Width",
        description: "Changes the width of the Config (in percent)",
        options: [25, 100],
        value: 60,
    })

    .addSlider({
        category: "Theme",
        configName: "height",
        title: "Change Height",
        description: "Changes the height of the Config (in percent)",
        options: [25, 100],
        value: 60,
    });

const config = new Settings("stella", defaultConf, "data/ColorScheme.json").addMarkdown("Changelog", CHANGELOG).addMarkdown("Credits", CREDITS);

const currentScheme = schemes[config.settings.scheme];
config.setPos(config.settings.x, config.settings.y).setSize(config.settings.width, config.settings.height).setScheme(currentScheme).apply();

export default () => config.settings;
