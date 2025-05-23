import { LocalStore } from "../../tska/storage/LocalStore";
import { FeatManager } from "../utils/helpers";
import { prefix, outlineSlot, highlightSlot, drawLineBetweenSlots } from "../utils/utils";

const data = new LocalStore(
    "stella",
    {
        binds: {},
    },
    "./data/stella-binds.json"
);

const slotBinnding = FeatManager.createFeature("slotBinding");
const bindingKeybind = new KeyBind("Bind Slots", Keyboard.KEY_NONE, "Stella");
const getPlayerController = () => Client.getMinecraft().field_71442_b;
let previousSlot = null;

const handleShiftClick = (slotClicked) => {
    let slot = slotClicked;
    const container = Player.getContainer();
    if (slot > 35) {
        slot = Object.keys(data.binds).find((key) => data.binds[key] === slotClicked);
    }
    const hotbarSlot = data.binds[slot] % 36;
    if (hotbarSlot == null || hotbarSlot >= 9) return;
    getPlayerController().func_78753_a(container.getWindowId(), slot, hotbarSlot, 2, Player.getPlayer());
};

slotBinnding.register("guiMouseClick", (_, __, mbtn, gui, event) => {
    if (mbtn !== 0 || !(gui instanceof net.minecraft.client.gui.inventory.GuiInventory)) return;
    const slot = gui.getSlotUnderMouse()?.field_75222_d;
    // 0 - 4 crafting slots
    if (!slot || slot < 5) return;
    if (previousSlot && (slot < 36 || slot > 44)) {
        ChatLib.chat(`${prefix} &cPlease click a valid hotbar slot!`);
        previousSlot = null;

        return;
    }

    if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
        if (slot in data.binds || Object.values(data.binds).includes(slot)) {
            cancel(event);
            handleShiftClick(slot);
            return;
        }
    }

    if (!Keyboard.isKeyDown(bindingKeybind.getKeyCode())) return;
    if (slot in data.binds || Object.values(data.binds).includes(slot)) {
        cancel(event);
        let tempslot = slot;
        if (Object.values(data.binds).includes(slot)) tempslot = Object.keys(data.binds).find((key) => data.binds[key] === slot);
        delete data.binds[tempslot];
        data.save();
        ChatLib.chat(`${prefix} &bBinding deleted`);
        return;
    }

    if (!previousSlot) previousSlot = slot;

    if (!(slot in data.binds) && !previousSlot) {
        data.binds[slot] = null;
        data.save();
    }

    cancel(event);
    if (slot === previousSlot) return;

    data.binds[previousSlot] = slot;
    data.save();

    ChatLib.chat(`${prefix} &bSaved binding&r: &6${previousSlot} &5-> &6${slot}`);
    previousSlot = null;
});

slotBinnding.register("guiRender", (mx, mt, gui) => {
    if (!(gui instanceof net.minecraft.client.gui.inventory.GuiInventory)) return;
    let curslot = gui?.getSlotUnderMouse()?.field_75222_d;
    let [r, g, b, a] = [0, 0, 255, 255];
    for (let slot of Object.keys(data.binds)) {
        outlineSlot(gui, slot, r, g, b, a, 1, false);
        outlineSlot(gui, data.binds[slot], r, g, b, a, 1, false);
        if ((slot == curslot || data.binds[slot] == curslot) && curslot) {
            drawLineBetweenSlots(gui, slot, data.binds[slot], r, g, b, a, 1, true);
        }
    }

    if (previousSlot) {
        let inv = Player.getContainer();
        for (let i = 0; i < inv.getSize(); i++) {
            if (i > 35 || i == previousSlot || i < 5) continue;
            highlightSlot(gui, i, 1, 0, 0, 1, false);
        }
    }
});
