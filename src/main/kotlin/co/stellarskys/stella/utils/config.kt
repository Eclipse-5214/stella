package co.stellarskys.stella.utils

import co.stellarskys.stella.features.msc.buttonUtils.ButtonLayoutEditor
import co.stellarskys.stella.features.secrets.utils.RouteRecorder
import co.stellarskys.stella.features.secrets.utils.RouteRegistry
import co.stellarskys.stella.hud.HUDEditor
import co.stellarskys.stella.utils.config.core.Config
import dev.deftu.omnicore.api.client.client
import kotlinx.coroutines.supervisorScope
import net.minecraft.Util
import java.net.URI

val config = Config("Stella", "Stella") {
    category("General"){
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

    category( "Dungeons") {
        subcategory( "Room Name") {
            toggle {
                configName = "showRoomName"
                name = "Show Room Name"
                description = "Shows the current map rooms name in a hud"
            }

            toggle {
                configName = "roomNameChroma"
                name = "Chroma Room Name"
                description = "Makes the room name chroma (Requires SBA or Skyhanni)"
            }
        }

        subcategory("Terminals") {
            toggle {
                configName = "termNumbers"
                name = "Terminal Numbers"
                description = "Number the terminals in dungeons (for calling terms)"
                default = false
            }

            dropdown {
                configName = "termNumber"
                name = "Number"
                description = "What terminal number you want to call"
                options = listOf("1", "2", "3", "4", "All")
                default = 4
                shouldShow { settings -> settings["termNumbers"] as Boolean }
            }

            toggle {
                configName = "highlightTerms"
                name = "Highlight Terms"
                description = "Highlights the terminals"
                default = false
                shouldShow { settings -> settings["termNumbers"] as Boolean }
            }

            colorpicker {
                configName = "termColor"
                name = "Highlight Color"
                description = "The color to highlight the terminals"
                default = rgba(0, 255, 255, 255)
                shouldShow { settings ->
                    (settings["termNumbers"] as Boolean) &&
                            (settings["highlightTerms"] as Boolean)
                }
            }

            toggle {
                configName = "showTermClass"
                name = "Show Class"
                description = "Displays related class"
                default = false
                shouldShow { settings -> settings["termNumbers"] as Boolean }
            }

            toggle {
                configName = "classColor"
                name = "Highlight Class Color"
                description = "Highlights the terminals the color of the class"
                default = false
                shouldShow { settings ->
                    (settings["termNumbers"] as Boolean) &&
                            (settings["highlightTerms"] as Boolean) &&
                            (settings["showTermClass"] as Boolean)
                }
            }

            toggle {
                configName = "hideNumber"
                name = "Hide Number"
                description = "Hides the terminal number"
                default = false
                shouldShow { settings ->
                    (settings["termNumbers"] as Boolean) &&
                            (settings["showTermClass"] as Boolean)
                }
            }

            toggle {
                configName = "m7Roles"
                name = "M7 Roles"
                description = "Displays M7 roles instead"
                default = false
                shouldShow { settings ->
                    (settings["termNumbers"] as Boolean) &&
                            (settings["showTermClass"] as Boolean)
                }
            }

            dropdown {
                configName = "termClass"
                name = "M7 Class"
                description = "What class you are playing"
                options = listOf("Tank", "Mage", "Bers", "Arch", "All")
                default = 4
                shouldShow { settings ->
                    (settings["termNumbers"] as Boolean) &&
                            (settings["showTermClass"] as Boolean) &&
                            (settings["m7Roles"] as Boolean)
                }
            }

            toggle {
                configName = "termTracker"
                name = "Terminal Tracker"
                description = "Tracks terminals, devices, and levers"
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
                default = rgba(255, 0, 255, 255)
            }

            colorpicker {
                configName = "dungDispenserColour"
                name = "Dispenser Color"
                description = "Color used for dispensers"
                default = rgba(255, 255, 0, 255)
            }

            colorpicker {
                configName = "dungLeverColour"
                name = "Lever Color"
                description = "Color used for levers"
                default = rgba(0, 255, 0, 255)
            }

            colorpicker {
                configName = "dungTripWireColour"
                name = "Tripwire Color"
                description = "Color used for tripwires"
                default = rgba(0, 255, 255, 255)
            }

            colorpicker {
                configName = "dungBatColour"
                name = "Bat Color"
                description = "Color used for map bats"
                default = rgba(255, 100, 255, 255)
            }

            colorpicker {
                configName = "dungChestColour"
                name = "Chest Color"
                description = "Color used for normal map chests"
                default = rgba(255, 150, 0, 255)
            }

            colorpicker {
                configName = "dungTrappedChestColour"
                name = "Trapped Chest Color"
                description = "Color used for trapped map chests"
                default = rgba(255, 0, 0, 255)
            }
        }
         */

        subcategory("Class Colors") {
            colorpicker {
                configName = "healerColor"
                name = "Healer Color"
                description = "Color used for Healer class"
                default = rgba(240, 70, 240, 255)
            }

            colorpicker {
                configName = "mageColor"
                name = "Mage Color"
                description = "Color used for Mage class"
                default = rgba(70, 210, 210, 255)
            }

            colorpicker {
                configName = "berzColor"
                name = "Berserker Color"
                description = "Color used for Berserker class"
                default = rgba(255, 0, 0, 255)
            }

            colorpicker {
                configName = "archerColor"
                name = "Archer Color"
                description = "Color used for Archer class"
                default = rgba(254, 223, 0, 255)
            }

            colorpicker {
                configName = "tankColor"
                name = "Tank Color"
                description = "Color used for Tank class"
                default = rgba(30, 170, 50, 255)
            }
        }

        subcategory("Score Alerts") {
            toggle {
                configName = "scoreAlerts"
                name = "Enable Score Alerts"
                description = "Enables alerts for dungeon score milestones"
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
                shouldShow { settings ->  (settings["scoreAlerts"] as Boolean) && (settings["scoreAlerts.alert270"] as Boolean) }
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
                shouldShow { settings -> (settings["scoreAlerts"] as Boolean) && (settings["scoreAlerts.alert300"] as Boolean) }
            }

            toggle {
                configName = "scoreAlerts.alert5Crypts"
                name = "5 Crypts Alert"
                description = "Alerts you when your team reaches 5 crypts"
                default = true
                shouldShow { settings -> settings["scoreAlerts"] as Boolean }
            }

            textinput {
                configName = "scoreAlerts.message5Crypts"
                name = "5 Crypts Message"
                description = "Message to display when reaching 5 crypts"
                placeholder = "&d5 crypts!"
                shouldShow { settings -> (settings["scoreAlerts"] as Boolean) && (settings["scoreAlerts.alert5Crypts"] as Boolean) }
            }
        }
    }

    category("StellaNav") {
        subcategory("General") {
            toggle {
                configName = "mapEnabled"
                name = "Enable map"
                description = "Enables the dungeon map"
                default = false
            }

            toggle {
                configName = "bossMapEnabled"
                name = "Enable Boss map"
                description = "Enables the map boss map"
                default = false
                shouldShow { settings -> settings["mapEnabled"] as Boolean }
            }

            toggle {
                configName = "scoreMapEnabled"
                name = "Enable Score map"
                description = "Enables the map score map"
                default = false
                shouldShow { settings -> settings["mapEnabled"] as Boolean }
            }

            toggle {
                configName = "mapInfoUnder"
                name = "map Info Under map"
                description = "Renders map info below the map"
                default = true
                shouldShow { settings -> settings["mapEnabled"] as Boolean }
            }
        }

        subcategory("Display") {
            colorpicker {
                configName = "mapBgColor"
                name = "map Background Color"
                description = "Background color of the map"
                default = rgba(0, 0, 0, 100)
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
                default = rgba(0, 0, 0, 255)
                shouldShow { settings -> settings["mapBorder"] as Boolean }
            }

            slider {
                configName = "mapBdWidth"
                name = "Border Width"
                description = "The width of the map border"
                min = 1f
                max = 5f
                default = 2f
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
                default = rgba(0,0,0,255)
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
                default = rgba(107, 58, 17, 255)
            }
            colorpicker {
                configName = "puzzleRoomColor"
                name = "Puzzle"
                default = rgba(117, 0, 133, 255)
            }
            colorpicker {
                configName = "trapRoomColor"
                name = "Trap"
                default = rgba(216, 127, 51, 255)
            }
            colorpicker {
                configName = "minibossRoomColor"
                name = "Miniboss"
                default = rgba(254, 223, 0, 255)
            }
            colorpicker {
                configName = "bloodRoomColor"
                name = "Blood"
                default = rgba(255, 0, 0, 255)
            }
            colorpicker {
                configName = "fairyRoomColor"
                name = "Fairy"
                default = rgba(224, 0, 255, 255)
            }
            colorpicker {
                configName = "entranceRoomColor"
                name = "Entrance"
                default = rgba(20, 133, 0, 255)
            }
        }

        subcategory("Door Colors") {
            colorpicker {
                configName = "normalDoorColor"
                name = "Normal Door"
                default = rgba(80, 40, 10, 255)
            }
            colorpicker {
                configName = "witherDoorColor"
                name = "Wither Door"
                default = rgba(0, 0, 0, 255)
            }
            colorpicker {
                configName = "bloodDoorColor"
                name = "Blood Door"
                default = rgba(255, 0, 0, 255)
            }
            colorpicker {
                configName = "entranceDoorColor"
                name = "Entrance Door"
                default = rgba(0, 204, 0, 255)
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
                default = rgba(0, 255, 0, 255)
                shouldShow { settings -> settings["boxWitherDoors"] as Boolean }
            }

            colorpicker {
                configName = "noKeyColor"
                name = "No Key Color"
                description = "Color for doors without keys"
                default = rgba(255, 0, 0, 255)
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
        subcategory("Waypoints") {
            toggle {
                configName = "secretWaypoints"
                name = "Show Waypoints"
                description = "Renders Secret Waypoints"
                default = false
            }

            colorpicker {
                configName = "secretWaypointColor.redstoneKey"
                name = "Redstone Key Color"
                description = "Highlight color for Redstone Key waypoints"
                default = rgba(255, 0, 0, 255) // red
                shouldShow { settings -> settings["secretWaypoints"] as Boolean }
            }

            colorpicker {
                configName = "secretWaypointColor.wither"
                name = "Wither Color"
                description = "Highlight color for Wither waypoints"
                default = rgba(0, 0, 255, 255) // blue
                shouldShow { settings -> settings["secretWaypoints"] as Boolean }
            }

            colorpicker {
                configName = "secretWaypointColor.bat"
                name = "Bat Color"
                description = "Highlight color for Bat waypoints"
                default = rgba(128, 128, 128, 255) // gray
                shouldShow { settings -> settings["secretWaypoints"] as Boolean }
            }

            colorpicker {
                configName = "secretWaypointColor.item"
                name = "Item Color"
                description = "Highlight color for Item waypoints"
                default = rgba(0, 255, 0, 255) // green
                shouldShow { settings -> settings["secretWaypoints"] as Boolean }
            }

            colorpicker {
                configName = "secretWaypointColor.chest"
                name = "Chest Color"
                description = "Highlight color for Chest waypoints"
                default = rgba(255, 255, 0, 255) // yellow
                shouldShow { settings -> settings["secretWaypoints"] as Boolean }
            }
        }

        subcategory("Routes") {
            toggle {
                configName = "secretRoutes"
                name = "Show Routes"
                description = "Enable rendering of route waypoints."
                default = false
            }

            toggle {
                configName = "secretRoutes.onlyRenderAfterClear"
                name = "Only Render After Clear"
                description = "Only show route waypoints after the room has been cleared."
                default = false
                shouldShow { settings -> settings["secretRoutes"] as Boolean }
            }

            toggle {
                configName = "secretRoutes.stopRenderAfterGreen"
                name = "Stop Render After Green"
                description = "Stop rendering route waypoints once the room is marked green."
                default = false
                shouldShow { settings -> settings["secretRoutes"] as Boolean }
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
            colorpicker {
                configName = "secretRoutes.startColor"
                name = "Start Color"
                description = "Color for the starting point of a route."
                default = rgba(0, 255, 0, 255) // green
            }

            colorpicker {
                configName = "secretRoutes.mineColor"
                name = "Mine Color"
                description = "Color for mining-related route waypoints."
                default = rgba(255, 165, 0, 255) // orange
            }

            colorpicker {
                configName = "secretRoutes.superboomColor"
                name = "Superboom Color"
                description = "Color for Superboom TNT route waypoints."
                default = rgba(255, 0, 0, 255) // red
            }

            colorpicker {
                configName = "secretRoutes.etherwarpColor"
                name = "Etherwarp Color"
                description = "Color for Etherwarp route waypoints."
                default = rgba(0, 0, 255, 255) // blue
            }

            colorpicker {
                configName = "secretRoutes.chestColor"
                name = "Chest Color"
                description = "Color for Chest waypoints."
                default = rgba(255, 255, 0, 255) // yellow
            }

            colorpicker {
                configName = "secretRoutes.itemColor"
                name = "Item Color"
                description = "Color for Item waypoints."
                default = rgba(255, 255, 0, 255) // yellow
            }

            colorpicker {
                configName = "secretRoutes.essenceColor"
                name = "Essence Color"
                description = "Color for Essence waypoints."
                default = rgba(255, 255, 0, 255) // yellow
            }

            colorpicker {
                configName = "secretRoutes.batColor"
                name = "Bat Color"
                description = "Color for bat route waypoints."
                default = rgba(128, 128, 128, 255) // gray
            }

            colorpicker {
                configName = "secretRoutes.leverColor"
                name = "Lever Color"
                description = "Color for lever route waypoints."
                default = rgba(0, 255, 255, 255) // cyan
                shouldShow { settings -> settings["secretRoutes"] as Boolean }
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
                description = "Saves the recording of the route (/sa route save) (To change route file version do it in the file)"

                onclick {
                    RouteRecorder.saveRoute()
                }
            }
        }
    }

    category( "Msc."){
        subcategory("Block Overlay") {
            toggle {
                configName = "overlayEnabled"
                name = "Render Block Overlay"
                description = "Highlights the block you are looking at"
            }

            colorpicker {
                configName = "blockHighlightColor"
                name = "Block Highlight Color"
                description = "The color to highlight blocks"
                default = rgba(0, 255, 255, 255)
                shouldShow { settings -> settings["overlayEnabled"] as Boolean }
            }

            toggle {
                configName = "fillBlockOverlay"
                name = "Fill blocks"
                description = "Fills the blocks with the color"
                shouldShow { settings -> settings["overlayEnabled"] as Boolean }
            }

            colorpicker {
                configName = "blockFillColor"
                name = "Block Fill Color"
                description = "The color to fill blocks"
                default = rgba(0, 255, 255, 30)
                shouldShow { settings -> settings["overlayEnabled"] as Boolean && settings["fillBlockOverlay"] as Boolean }
            }

            stepslider {
                configName = "overlayLineWidth"
                name = "Line width"
                description = "Line width for the outline"
                min = 1
                max = 5
                step = 1
                default = 3
                shouldShow { settings -> settings["overlayEnabled"] as Boolean }
            }
        }

        subcategory("Inventory Buttons"){
            toggle {
                configName = "buttonsEnabled"
                name = "Enabled"
                description = "Enables the inventory buttons"
            }

            button {
                configName = "buttonEdit"
                name = "Inventory Button Editor"
                description = "Opens the inventory button editor"
                placeholder = "Open"

                onclick {
                    client.setScreen(ButtonLayoutEditor())
                }
            }
        }

        subcategory("Pet Display") {
            toggle {
                configName = "petDisplay"
                name = "Enabled"
                description = "Enables the pet display"
            }
        }
    }
}