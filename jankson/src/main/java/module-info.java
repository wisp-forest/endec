module jankson_endec {
    requires transitive endec;
    requires static org.jetbrains.annotations;
    requires jankson;
    exports io.wispforest.endec.format.json;
}