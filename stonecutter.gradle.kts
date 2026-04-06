plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.11-cheat"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod_version")}\""
    swaps["minecraft"] = "\"${node.metadata.version.toString().substringBefore("-")}\""
}
