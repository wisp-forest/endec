module netty_endec {
    requires transitive endec;
    requires io.netty.buffer;
    requires static org.jetbrains.annotations;
    exports io.wispforest.endec.format.bytebuf;
    // other declarations
}