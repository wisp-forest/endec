module endec {
    requires static org.jetbrains.annotations;
    requires com.google.common;
    requires it.unimi.dsi.fastutil;
    exports io.wispforest.endec;
    exports io.wispforest.endec.impl;
    exports io.wispforest.endec.util;
    exports io.wispforest.endec.annotations;
    exports io.wispforest.endec.format.data;
    exports io.wispforest.endec.format.edm;
    exports io.wispforest.endec.format.forwarding;
}