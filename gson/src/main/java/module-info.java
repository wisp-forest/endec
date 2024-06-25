module gson_endec {
    requires transitive endec;
    requires com.google.gson;
    requires static org.jetbrains.annotations;
    exports io.wispforest.endec.format.json;
    // other declarations
}