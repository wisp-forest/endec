package io.wispforest.endec.util;

import it.unimi.dsi.fastutil.Pair;

import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

public class BlockWriter {
    private final StringWriter result = new StringWriter();
    private final Deque<Pair<Boolean, String>> blocks = new ArrayDeque<>();

    private int indentLevel = 0;

    public BlockWriter() {}

    public BlockWriter write(String value) {
        this.result.write(value);

        return this;
    }

    public BlockWriter writeln() {
        this.writeln("");

        return this;
    }

    public BlockWriter writeln(String value) {
        this.result.write(value + "\n" + ("  ".repeat(Math.max(0, this.indentLevel))));

        return this;
    }

    public BlockWriter startBlock(String startDelimiter, String endDelimiter) {
        return startBlock(startDelimiter, endDelimiter, true);
    }

    public BlockWriter startBlock(String startDelimiter, String endDelimiter, boolean incrementIndentation) {
        if (incrementIndentation) {
            this.indentLevel++;
            this.writeln(startDelimiter);
        } else {
            this.write(startDelimiter);
        }

        this.blocks.addLast(Pair.of(incrementIndentation, endDelimiter));

        return this;
    }

    public BlockWriter writeBlock(String startDelimiter, String endDelimiter, Consumer<BlockWriter> consumer) {
        return writeBlock(startDelimiter, endDelimiter, true, consumer);
    }

    public BlockWriter writeBlock(String startDelimiter, String endDelimiter, boolean incrementIndentation, Consumer<BlockWriter> consumer) {
        this.startBlock(startDelimiter, endDelimiter, incrementIndentation);

        consumer.accept(this);

        this.endBlock();

        return this;
    }

    public BlockWriter endBlock() {
        var endBlockData = this.blocks.removeLast();

        if (endBlockData.first()) {
            this.indentLevel--;
            writeln();
        }

        write(endBlockData.right());

        return this;
    }

    public String buildResult() {
        return this.result.toString();
    }
}
