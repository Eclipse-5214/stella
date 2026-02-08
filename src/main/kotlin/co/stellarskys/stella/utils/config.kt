package co.stellarskys.stella.utils

import co.stellarskys.stella.features.msc.buttonUtils.ButtonLayoutEditor
import co.stellarskys.stella.features.secrets.utils.RouteRecorder
import co.stellarskys.stella.features.secrets.utils.RouteRegistry
import co.stellarskys.stella.hud.HUDEditor
import co.stellarskys.stella.utils.config.core.Config
import dev.deftu.omnicore.api.client.client
import kotlinx.coroutines.supervisorScope
import net.minecraft.Util
import java.awt.Color
import java.net.URI

val config = Config("Stella", "Stella") {
    category("General") {
        subcategory("Info") {
            textparagraph {
                configName = "info"
                name = "Stella"
                description = "§bDungeon & QOL Mod" +
                        "\n§bMade by §dNEXD_" +
                        "\n§bCommands: §6/stella §f, §6/sa §f, §6/sta"
            }

        }

        subcategory("Socials") {
            button {
                configName = "website"
                name = "Website"
                description = "A link to stella's website"

                onclick {
                    val uri = URI("https://stellarskys.co")
                    Util.getPlatform().openUri(uri)
                }
            }

            button {
                configName = "discord"
                name = "Discord"
                description = "A link to stella's discord"

                onclick {
                    val uri = URI("https://discord.gg/EzEfQyGdAg")
                    Util.getPlatform().openUri(uri)
                }
            }

            button {
                configName = "source"
                name = "Source"
                description = "A link to stella's source"

                onclick {
                    val uri = URI("https://github.com/Eclipse-5214/stella")
                    Util.getPlatform().openUri(uri)
                }
            }
        }

        subcategory("Shortcuts") {
            toggle {
                configName = "loadMessage"
                name = "Show Load Message"
                description = "Showes Stella's loading message"
                default = true
            }

            button {
                configName = "hudEditor"
                name = "Hud Editor"
                description = "Opens Stella's Hud Editor (/sa hud)"

                onclick {
                    client.setScreen(HUDEditor())
                }
            }
        }
    }

    category("Dungeons") {
        subcategory("Room Name", "showRoomName", "Shows the current map rooms name in a hud") {
            toggle {
                configName = "roomNameChroma"
                name = "Chroma Room Name"
                description = "Makes the room name chroma (Requires SBA or Skyhanni)"
            }
        }

        subcategory("Terminals") {
            toggle {
                configName = "termNumbers"
                name = "Enabled"
                description = "Shows terminal numbers and class labels in F7/M7 boss."
                default = false
            }

            dropdown {
                configName = "selectedRole"
                name = "Your Role"
                description = "Which class you are playing for terminal assignments."
                options = listOf("Tank", "Mage", "Bers", "Arch", "Heal", "All")
                default = 4 // All
                shouldShow { it["termNumbers"] as Boolean }
            }

            dropdown {
                configName = "preset"
                name = "Role Presets"
                description = "Which roll presets you want to use (from M7 Guides)"
                options = listOf("F7", "SL M7", "Low M7", "Mid M7", "High M7")
                default = 1 // All
                shouldShow { it["termNumbers"] as Boolean }
            }

            toggle {
                configName = "showTermClass"
                name = "Show Class Label"
                description = "Displays the class label next to each terminal."
                default = false
                shouldShow { it["termNumbers"] as Boolean }
            }

            toggle {
                configName = "hideNumber"
                name = "Hide Terminal Number"
                description = "Hides the terminal number and only shows the class label."
                default = false
                shouldShow { settings ->
                    (settings["termNumbers"] as Boolean) &&
                            (settings["showTermClass"] as Boolean)
                }
            }

            toggle {
                configName = "highlightTerms"
                name = "Highlight Term Blocks"
                description = "Outlines terminals in the world."
                default = false
                shouldShow { it["termNumbers"] as Boolean }
            }

            colorpicker {
                configName = "termColor"
                name = "Highlight Color"
                description = "Color used when not using class colors."
                default = Color(0, 255, 255, 255)
                shouldShow { settings ->
                    (settings["termNumbers"] as Boolean) &&
                            (settings["highlightTerms"] as Boolean) &&
                            !(settings["classColor"] as Boolean)
                }
            }

            toggle {
                configName = "classColor"
                name = "Use Class Color"
                description = "Colors terminals based on the assigned class."
                default = false
                shouldShow { it["termNumbers"] as Boolean && it["showTermClass"] as Boolean && it["highlightTerms"] as Boolean }
            }

            toggle {
                configName = "termTracker"
                name = "Terminal Tracker"
                description = "Tracks terminals, devices, and levers during boss."
                default = false
            }
        }

        /*
        subcategory("Block Overlay") {
            toggle {
                configName = "enableDungBlockOverlay"
                name = "Enable Block Overlay"
                description = "Replaces map block textures with colored overlays"
                default = true
            }

            toggle {
                configName = "dungeonBlocksEverywhere"
                name = "Render Outside Dungeons"
                description = "Shows block overlays even outside of dungeons"
                default = false
                shouldShow { settings -> settings["enableDungBlockOverlay"] as Boolean }
            }

            colorpicker {
                configName = "dungCrackedColour"
                name = "Cracked Brick Color"
                description = "Color used for cracked stone bricks"
                default = Color(255, 0, 255, 255)
            }

            colorpicker {
                configName = "dungDispenserColour"
                name = "Dispenser Color"
                description = "Color used for dispensers"
                default = Color(255, 255, 0, 255)
            }

            colorpicker {
                configName = "dungLeverColour"
                name = "Lever Color"
                description = "Color used for levers"
                default = Color(0, 255, 0, 255)
            }

            colorpicker {
                configName = "dungTripWireColour"
                name = "Tripwire Color"
                description = "Color used for tripwires"
                default = Color(0, 255, 255, 255)
            }

            colorpicker {
                configName = "dungBatColour"
                name = "Bat Color"
                description = "Color used for map bats"
                default = Color(255, 100, 255, 255)
            }

            colorpicker {
                configName = "dungChestColour"
                name = "Chest Color"
                description = "Color used for normal map chests"
                default = Color(255, 150, 0, 255)
            }

            colorpicker {
                configName = "dungTrappedChestColour"
                name = "Trapped Chest Color"
                description = "Color used for trapped map chests"
                default = Color(255, 0, 0, 255)
            }
        }
         */

        subcategory("Class Colors") {
            colorpicker {
                configName = "healerColor"
                name = "Healer Color"
                description = "Color used for Healer class"
                default = Color(240, 70, 240, 255)
            }

            colorpicker {
                configName = "mageColor"
                name = "Mage Color"
                description = "Color used for Mage class"
                default = Color(70, 210, 210, 255)
            }

            colorpicker {
                configName = "berzColor"
                name = "Berserker Color"
                description = "Color used for Berserker class"
                default = Color(255, 0, 0, 255)
            }

            colorpicker {
                configName = "archerColor"
                name = "Archer Color"
                description = "Color used for Archer class"
                default = Color(254, 223, 0, 255)
            }

            colorpicker {
                configName = "tankColor"
                name = "Tank Color"
                description = "Color used for Tank class"
                default = Color(30, 170, 50, 255)
            }
        }

        subcategory("Score Alerts", "scoreAlerts", "Enables alerts for dungeon score milestones") {
            toggle {
                configName = "forcePaul"
                name = "Force Paul"
                description = "Forces Paul's EZPZ +10 score"
                default = false
            }

            toggle {
                configName = "scoreAlerts.alert270"
                name = "270 Score Alert"
                description = "Alerts you when your score reaches 270"
                default = true
                shouldShow { settings -> settings["scoreAlerts"] as Boolean }
            }

            textinput {
                configName = "scoreAlerts.message270"
                name = "270 Score Message"
                description = "Message to display when reaching 270 score"
                placeholder = "&d270 score!"
                shouldShow { settings -> settings["scoreAlerts.alert270"] as Boolean }
            }

            toggle {
                configName = "scoreAlerts.chat270"
                name = "270 Score Chat Alert"
                description = "Sends a chat message when reaching 270 score"
                default = true
            }

            textinput {
                configName = "scoreAlerts.chatMessage270"
                name = "270 Score Chat Message"
                description = "Chat message to send when reaching 270 score"
                placeholder = "270 score!"
                shouldShow { settings -> settings["scoreAlerts.chat270"] as Boolean }
            }

            toggle {
                configName = "scoreAlerts.alert300"
                name = "300 Score Alert"
                description = "Alerts you when your score reaches 300"
                default = true
                shouldShow { settings -> settings["scoreAlerts"] as Boolean }
            }

            textinput {
                configName = "scoreAlerts.message300"
                name = "300 Score Message"
                description = "Message to display when reaching 300 score"
                placeholder = "&d300 score!"
                shouldShow { settings -> settings["scoreAlerts.alert300"] as Boolean }
            }

            toggle {
                configName = "scoreAlerts.chat300"
                name = "300 Score Chat Alert"
                description = "Sends a chat message when reaching 300 score"
                default = true
            }

            textinput {
                configName = "scoreAlerts.chatMessage300"
                name = "300 Score Chat Message"
                description = "Chat message to send when reaching 300 score"
                placeholder = "300 score!"
                shouldShow { settings -> settings["scoreAlerts.chat300"] as Boolean }
            }

            toggle {
                configName = "scoreAlerts.alert5Crypts"
                name = "5 Crypts Alert"
                description = "Alerts you when your team reaches 5 crypts"
                default = true
            }

            textinput {
                configName = "scoreAlerts.message5Crypts"
                name = "5 Crypts Message"
                description = "Message to display when reaching 5 crypts"
                placeholder = "&d5 crypts!"
                shouldShow { settings -> settings["scoreAlerts.alert5Crypts"] as Boolean }
            }

            toggle {
                configName = "scoreAlerts.chat5Crypts"
                name = "5 Crypts Chat Alert"
                description = "Sends a chat message when reaching 5 crypts"
                default = true
            }

            textinput {
                configName = "scoreAlerts.chatMessage5Crypts"
                name = "5 Crypts Chat Message"
                description = "Chat message to send when reaching 5 crypts"
                placeholder = "5 crypts!"
                shouldShow { settings -> settings["scoreAlerts.chat5Crypts"] as Boolean }
            }
        }
    }

    category("StellaNav") {
        subcategory("Map", "mapEnabled", "Enables the dungeon map") {
            toggle {
                configName = "bossMapEnabled"
                name = "Enable Boss map"
                description = "Enables the map boss map"
                default = false
            }

            toggle {
                configName = "scoreMapEnabled"
                name = "Enable Score map"
                description = "Enables the map score map"
                default = false
            }

            toggle {
                configName = "mapInfoUnder"
                name = "map Info Under map"
                description = "Renders map info below the map"
                default = true
            }
        }

        subcategory("Display") {
            colorpicker {
                configName = "mapBgColor"
                name = "map Background Color"
                description = "Background color of the map"
                default = Color(0, 0, 0, 100)
            }

            toggle {
                configName = "mapBorder"
                name = "map Border"
                description = "Renders a border around the map"
                default = true
            }

            colorpicker {
                configName = "mapBdColor"
                name = "map Border Color"
                description = "Color of the map border"
                default = Color(0, 0, 0, 255)
                shouldShow { settings -> settings["mapBorder"] as Boolean }
            }

            stepslider {
                configName = "mapBdWidth"
                name = "Border Width"
                description = "The width of the map border"
                min = 1
                max = 5
                step = 1
                default = 2
                shouldShow { settings -> settings["mapBorder"] as Boolean }
            }

            dropdown {
                configName = "roomCheckmarks"
                name = "Room Checkmarks"
                description = "Style of room checkmarks"
                options = listOf("Checkmark", "Name", "Secrets", "Both")
                default = 0
            }

            dropdown {
                configName = "puzzleCheckmarks"
                name = "Puzzle Checkmarks"
                description = "Style of puzzle checkmarks"
                options = listOf("Checkmark", "Name", "Secrets", "Both")
                default = 0
            }

            slider {
                configName = "checkmarkScale"
                name = "Checkmark Size"
                description = "Size of the checkmarks"
                min = 0.1f
                max = 2f
                default = 1f
            }

            slider {
                configName = "rcsize"
                name = "Room Text"
                description = "Size of room text"
                min = 0.1f
                max = 2f
                default = 1f
            }

            slider {
                configName = "pcsize"
                name = "Puzzle Text"
                description = "Size of puzzle text"
                min = 0.1f
                max = 2f
                default = 1f
            }

            toggle {
                configName = "mtextshadow"
                name = "Text Shadow"
                description = "Gives the text a cool shadow"
                default = true
            }
        }

        subcategory("Player Icons") {
            slider {
                configName = "iconScale"
                name = "Icon Scale"
                description = "Scale of the player icons"
                min = 0.1f
                max = 2f
                default = 1f
            }

            toggle {
                configName = "smoothMovement"
                name = "Smooth Movement"
                description = "Smooths marker movement"
                default = true
            }

            toggle {
                configName = "showPlayerHeads"
                name = "Player Heads"
                description = "Use player heads instead of map markers"
                default = false
            }

            slider {
                configName = "iconBorderWidth"
                name = "Border Width"
                description = "The width of the icon border"
                min = 0f
                max = 1f
                default = 0.2f
            }

            colorpicker {
                configName = "iconBorderColor"
                name = "Border Color"
                description = "The color for the icon border"
                default = Color(0, 0, 0, 255)
            }

            toggle {
                configName = "iconClassColors"
                name = "Class Colors"
                description = "Use the color for the players class for the icon border"
                default = false
            }

            toggle {
                configName = "showNames"
                name = "Show Player Names"
                description = "Render player names under map icons"
            }

            toggle {
                configName = "dontShowOwn"
                name = "Hide Own Name"
                description = "Hides your name on the map"
                shouldShow { settings -> settings["showNames"] as Boolean }
            }

        }

        subcategory("Room Colors") {
            colorpicker {
                configName = "normalRoomColor"
                name = "Normal"
                default = Color(107, 58, 17, 255)
            }
            colorpicker {
                configName = "puzzleRoomColor"
                name = "Puzzle"
                default = Color(117, 0, 133, 255)
            }
            colorpicker {
                configName = "trapRoomColor"
                name = "Trap"
                default = Color(216, 127, 51, 255)
            }
            colorpicker {
                configName = "minibossRoomColor"
                name = "Miniboss"
                default = Color(254, 223, 0, 255)
            }
            colorpicker {
                configName = "bloodRoomColor"
                name = "Blood"
                default = Color(255, 0, 0, 255)
            }
            colorpicker {
                configName = "fairyRoomColor"
                name = "Fairy"
                default = Color(224, 0, 255, 255)
            }
            colorpicker {
                configName = "entranceRoomColor"
                name = "Entrance"
                default = Color(20, 133, 0, 255)
            }
        }

        subcategory("Door Colors") {
            colorpicker {
                configName = "normalDoorColor"
                name = "Normal Door"
                default = Color(80, 40, 10, 255)
            }
            colorpicker {
                configName = "witherDoorColor"
                name = "Wither Door"
                default = Color(0, 0, 0, 255)
            }
            colorpicker {
                configName = "bloodDoorColor"
                name = "Blood Door"
                default = Color(255, 0, 0, 255)
            }
            colorpicker {
                configName = "entranceDoorColor"
                name = "Entrance Door"
                default = Color(0, 204, 0, 255)
            }
        }

        subcategory("Extra") {
            toggle {
                configName = "boxWitherDoors"
                name = "Box Wither Doors"
                description = "Renders a box around wither doors"
                default = false
            }

            colorpicker {
                configName = "keyColor"
                name = "Key Color"
                description = "Color for doors with keys"
                default = Color(0, 255, 0, 255)
                shouldShow { settings -> settings["boxWitherDoors"] as Boolean }
            }

            colorpicker {
                configName = "noKeyColor"
                name = "No Key Color"
                description = "Color for doors without keys"
                default = Color(255, 0, 0, 255)
                shouldShow { settings -> settings["boxWitherDoors"] as Boolean }
            }

            stepslider {
                configName = "doorLineWidth"
                name = "Door Line Width"
                description = "Line width for doors"
                min = 1
                max = 5
                step = 1
                default = 3
                shouldShow { settings -> settings["boxWitherDoors"] as Boolean }
            }

            toggle {
                configName = "separateMapInfo"
                name = "Separate map Info"
                description = "Renders the map info separate from the dungeon map"
                default = false
            }

            toggle {
                configName = "dungeonBreakdown"
                name = "Dungeon Breakdown"
                description = "Sends map info after run"
                default = false
            }
        }
    }

    category("Secrets") {
        subcategory("Waypoints", "secretWaypoints", "Renders Secret Waypoints") {
            toggle {
                configName = "secretWaypoints.text"
                name = "Show Waypoint Text"
                description = "Renders Secret Waypoints text"
                default = true
            }

            slider {
                configName = "secretWaypoints.textScale"
                name = "Text Scale"
                description = "Scale of the waypoint text"
                min = 0.1f
                max = 2f
                default = 1f
            }

            colorpicker {
                configName = "secretWaypointColor.redstonekey"
                name = "Redstone Key Color"
                description = "Highlight color for Redstone Key waypoints"
                default = Color(255, 0, 0, 255) // red
            }

            colorpicker {
                configName = "secretWaypointColor.wither"
                name = "Wither Color"
                description = "Highlight color for Wither waypoints"
                default = Color(0, 0, 255, 255) // blue
            }

            colorpicker {
                configName = "secretWaypointColor.bat"
                name = "Bat Color"
                description = "Highlight color for Bat waypoints"
                default = Color(128, 128, 128, 255) // gray
            }

            colorpicker {
                configName = "secretWaypointColor.item"
                name = "Item Color"
                description = "Highlight color for Item waypoints"
                default = Color(0, 255, 0, 255) // green
            }

            colorpicker {
                configName = "secretWaypointColor.chest"
                name = "Chest Color"
                description = "Highlight color for Chest waypoints"
                default = Color(255, 255, 0, 255) // yellow
            }
        }

        subcategory("Routes","secretRoutes", "Enable rendering of route waypoints.") {
            toggle {
                configName = "secretRoutes.onlyRenderAfterClear"
                name = "Only After Clear"
                description = "Only show route waypoints after the room has been cleared."
                default = false
            }

            toggle {
                configName = "secretRoutes.stopRenderAfterGreen"
                name = "Stop After Green"
                description = "Stop rendering route waypoints once the room is marked green."
                default = false
            }

            textinput {
                configName = "secretRoutes.fileName"
                name = "File Name"
                description = "The name of the file to load the routes from (press reload after changing)"
                placeholder = "default.json"
            }

            button {
                configName = "secretRoutes.reload"
                name = "Reload Routes"
                description = "reloads the secret routes from the config file"

                onclick {
                    RouteRegistry.reload()
                }
            }

            keybind {
                configName = "secretRoutes.nextStep"
                name = "Next Step Bind"
                description = "Goes to the next step of a route"
            }

            keybind {
                configName = "secretRoutes.lastStep"
                name = "Last Step Bind"
                description = "Goes to the last step of a route"
            }
        }

        subcategory("Route Rendering") {
            toggle {
                configName = "secretRoutes.text"
                name = "Show Waypoint Text"
                description = "Renders Secret Routes Waypoints text"
                default = true
            }

            slider {
                configName = "secretRoutes.textScale"
                name = "Text Scale"
                description = "Scale of the waypoint text"
                min = 0.1f
                max = 2f
                default = 1f
            }

            colorpicker {
                configName = "secretRoutes.startColor"
                name = "Start Color"
                description = "Color for the starting point of a route."
                default = Color(0, 255, 0, 255) // green
            }

            colorpicker {
                configName = "secretRoutes.mineColor"
                name = "Mine Color"
                description = "Color for mining-related route waypoints."
                default = Color(255, 165, 0, 255) // orange
            }

            colorpicker {
                configName = "secretRoutes.superboomColor"
                name = "Superboom Color"
                description = "Color for Superboom TNT route waypoints."
                default = Color(255, 0, 0, 255) // red
            }

            colorpicker {
                configName = "secretRoutes.etherwarpColor"
                name = "Etherwarp Color"
                description = "Color for Etherwarp route waypoints."
                default = Color(0, 0, 255, 255) // blue
            }

            colorpicker {
                configName = "secretRoutes.pearlColor"
                name = "Pearl Color"
                description = "Color for Pearl waypoints."
                default = Color(0, 255, 255, 255) // blue
            }

            colorpicker {
                configName = "secretRoutes.chestColor"
                name = "Chest Color"
                description = "Color for Chest waypoints."
                default = Color(255, 255, 0, 255) // yellow
            }

            colorpicker {
                configName = "secretRoutes.itemColor"
                name = "Item Color"
                description = "Color for Item waypoints."
                default = Color(255, 255, 0, 255) // yellow
            }

            colorpicker {
                configName = "secretRoutes.essenceColor"
                name = "Essence Color"
                description = "Color for Essence waypoints."
                default = Color(255, 255, 0, 255) // yellow
            }

            colorpicker {
                configName = "secretRoutes.batColor"
                name = "Bat Color"
                description = "Color for bat route waypoints."
                default = Color(128, 128, 128, 255) // gray
            }

            colorpicker {
                configName = "secretRoutes.leverColor"
                name = "Lever Color"
                description = "Color for lever route waypoints."
                default = Color(0, 255, 255, 255) // cyan
            }
        }

        subcategory("Route Recording") {
            toggle {
                configName = "secretRoutes.recordingHud"
                name = "Recording Hud"
                description = "A helpful hud for recording secret routes"
            }

            toggle {
                configName = "secretRoutes.recordingHud.minimized"
                name = "Minimize Recording Hud"
                description = "Makes the hud A lot smaller"
            }

            button {
                configName = "secretRoutes.startRecording"
                name = "Start Recording"
                description = "Starts recording a route (/sa route start)"

                onclick {
                    RouteRecorder.startRecording()
                }
            }

            button {
                configName = "secretRoutes.stopRecording"
                name = "Stop Recording"
                description = "Stops recording a route (/sa route stop)"

                onclick {
                    RouteRecorder.stopRecording()
                }
            }

            button {
                configName = "secretRoutes.saveRecording"
                name = "Save Recording"
                description =
                    "Saves the recording of the route (/sa route save) (To change route file version do it in the file)"

                onclick {
                    RouteRecorder.saveRoute()
                }
            }
        }
    }

    category("Msc.") {
        subcategory("Block Overlay", "overlayEnabled", "Highlights the block you are looking at" ) {
            colorpicker {
                configName = "blockHighlightColor"
                name = "Block Highlight Color"
                description = "The color to highlight blocks"
                default = Color(0, 255, 255, 255)
            }

            toggle {
                configName = "fillBlockOverlay"
                name = "Fill blocks"
                description = "Fills the blocks with the color"
            }

            colorpicker {
                configName = "blockFillColor"
                name = "Block Fill Color"
                description = "The color to fill blocks"
                default = Color(0, 255, 255, 30)
                shouldShow { settings -> settings["fillBlockOverlay"] as Boolean }
            }

            stepslider {
                configName = "overlayLineWidth"
                name = "Line width"
                description = "Line width for the outline"
                min = 1
                max = 5
                step = 1
                default = 3
            }
        }

        subcategory("Inventory Buttons", "buttonsEnabled", "Enables the inventory buttons") {
            button {
                configName = "buttonEdit"
                name = "Button Editor"
                description = "Opens the inventory button editor"
                placeholder = "Open"

                onclick {
                    client.setScreen(ButtonLayoutEditor())
                }
            }
        }

        subcategory("Pet Display", "petDisplay", "Enables the pet display")

        subcategory("Health & Mana",  "bars", "Enables the health & mana bars") {
            toggle {
                configName = "bars.hideVanillaHealth"
                name = "Hide Vanilla Health"
                description = "Hides the vanilla Minecraft health bar"
                default = false
            }

            toggle {
                configName = "bars.hideVanillaHunger"
                name = "Hide Vanilla Hunger"
                description = "Hides the vanilla hunger display"
                default = false
            }

            toggle {
                configName = "bars.hideVanillaArmor"
                name = "Hide Vanilla Mana"
                description = "Hides the vanilla armor display"
                default = false
            }

            toggle {
                configName = "bars.healthBar"
                name = "Health Bar"
                description = "Shows a custom health bar"
                default = false
            }

            toggle {
                configName = "bars.absorptionBar"
                name = "Absorption Bar"
                description = "Shows a custom absorption bar"
                default = false
                shouldShow { it["bars.healthBar"] as Boolean }
            }

            toggle {
                configName = "bars.hpChange"
                name = "Health Change HUD"
                description = "Shows the health delta (damage/healing numbers)"
                default = false
                shouldShow { it["bars.healthBar"] as Boolean }
            }

            toggle {
                configName = "bars.hpNum"
                name = "Health Number HUD"
                description = "Shows the numeric health value"
                default = false
                shouldShow { it["bars.healthBar"] as Boolean }
            }

            colorpicker {
                configName = "bars.healthColor"
                name = "Health Bar Color"
                description = "Color of the custom health bar"
                default = Color(255, 0, 0, 255)
                shouldShow { it["bars.healthBar"] as Boolean }
            }

            colorpicker {
                configName = "bars.absorptionColor"
                name = "Absorption Bar Color"
                description = "Color of the custom absorption bar"
                default = Color(255, 200, 0, 255)
                shouldShow { it["bars.absorptionBar"] as Boolean && it["bars.healthBar"] as Boolean }
            }

            toggle {
                configName = "bars.manaBar"
                name = "Mana Bar"
                description = "Shows a custom mana bar"
                default = false
            }

            toggle {
                configName = "bars.overflowManaBar"
                name = "Overflow Mana Bar"
                description = "Shows a custom overflow mana bar"
                default = false
                shouldShow { it["bars.manaBar"] as Boolean }
            }

            toggle {
                configName = "bars.ofMana"
                name = "Overflow Mana HUD"
                description = "Shows your overflow mana value"
                default = false
                shouldShow { it["bars.manaBar"] as Boolean }
            }

            toggle {
                configName = "bars.mpNum"
                name = "Mana Number HUD"
                description = "Shows the numeric mana value"
                default = false
                shouldShow { it["bars.manaBar"] as Boolean }
            }

            colorpicker {
                configName = "bars.manaColor"
                name = "Mana Bar Color"
                description = "Color of the custom mana bar"
                default = Color(0, 128, 255, 255)
                shouldShow { it["bars.manaBar"] as Boolean }
            }

            colorpicker {
                configName = "bars.ofmColor"
                name = "Overflow Mana Color"
                description = "Color of the overflow mana bar"
                default = Color(128, 0, 255, 255)
                shouldShow { it["bars.overflowManaBar"] as Boolean && it["bars.manaBar"] as Boolean }
            }
        }

        subcategory("Soulflow Display", "soulflowDisplay", "Enables the soulflow display")

        /*
        subcategory("Custom Nametags") {
            toggle {
                configName = "customNametags"
                name = "Enabled"
                description = "Enables the soulflow display"
            }
        }
         */
    }
}