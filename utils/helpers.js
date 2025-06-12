import { FeatureManager } from "../../tska/event/FeatureManager";
//import { Event } from "../../tska/event/Event";
import settings from "./config";

/*  ------------- Helper Utilities --------------

    Various helper functions for the mod

    ------------------- To Do -------------------

    - Nothing :D

    --------------------------------------------- */

export const FeatManager = new FeatureManager(settings().getConfig());

//data
export const data = new LocalStore(
    "stella",
    {
        version: "0.0.0",
        firstInstall: false,
    },
    "./data/stella.json"
);
